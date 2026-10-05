package com.deepblue.rescue.service.impl;

import com.deepblue.rescue.domain.Animal;
import com.deepblue.rescue.domain.RescueCase;
import com.deepblue.rescue.domain.Specialist;
import com.deepblue.rescue.domain.Treatment;
import com.deepblue.rescue.dto.request.CreateTreatmentRequest;
import com.deepblue.rescue.dto.response.TreatmentResponse;
import com.deepblue.rescue.exception.BusinessRuleException;
import com.deepblue.rescue.exception.ResourceNotFoundException;
import com.deepblue.rescue.mapper.TreatmentMapper;
import com.deepblue.rescue.repository.AnimalRepository;
import com.deepblue.rescue.repository.SpecialistRepository;
import com.deepblue.rescue.repository.TreatmentRepository;
import com.deepblue.rescue.service.TreatmentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Service
@Validated
@Transactional(readOnly = true)
public class TreatmentServiceImpl implements TreatmentService {

    private final AnimalRepository animalRepository;
    private final SpecialistRepository specialistRepository;
    private final TreatmentRepository treatmentRepository;
    private final TreatmentMapper mapper;

    public TreatmentServiceImpl(AnimalRepository animalRepository,
                                SpecialistRepository specialistRepository,
                                TreatmentRepository treatmentRepository,
                                TreatmentMapper mapper) {
        this.animalRepository = animalRepository;
        this.specialistRepository = specialistRepository;
        this.treatmentRepository = treatmentRepository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public TreatmentResponse register(CreateTreatmentRequest request) {

        // Regla 1: el animal debe existir
        Animal animal = animalRepository.findByAnimalCode(request.animalCode())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Animal %s does not exist.".formatted(request.animalCode())));

        // Regla 2: el especialista debe existir
        Specialist specialist = specialistRepository.findByProfessionalCode(request.specialistCode())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Specialist %s does not exist.".formatted(request.specialistCode())));

        // Regla 3: el especialista debe estar activo (null se trata como inactivo)
        if (!Boolean.TRUE.equals(specialist.getActive())) {
            throw new BusinessRuleException(
                    "Specialist %s is not active.".formatted(specialist.getProfessionalCode()));
        }

        RescueCase rescueCase = animal.getRescueCase();
        if (rescueCase == null) {
            throw new BusinessRuleException(
                    "Animal %s has no rescue case.".formatted(animal.getAnimalCode()));
        }

        // Regla 4: solo en UNDER_EVALUATION o IN_REHABILITATION
        if (!rescueCase.getStatus().allowsTreatments()) {
            throw new BusinessRuleException(
                    "Cannot register treatment because rescue case %s is %s."
                            .formatted(rescueCase.getCaseCode(), rescueCase.getStatus()));
        }

        // Regla 5: la fecha del tratamiento no puede ser anterior al rescate
        if (request.performedAt().toLocalDate().isBefore(rescueCase.getRescueDate())) {
            throw new BusinessRuleException(
                    "Treatment date %s is before rescue date %s."
                            .formatted(request.performedAt(), rescueCase.getRescueDate()));
        }

        Treatment treatment = new Treatment(
                animal,
                specialist,
                request.performedAt(),
                request.type(),
                request.description()
        );

        Treatment saved = treatmentRepository.save(treatment);

        return mapper.toResponse(saved);
    }

    @Override
    public List<TreatmentResponse> findByAnimalCode(String animalCode) {
        return treatmentRepository.findByAnimalAnimalCodeOrderByPerformedAtAsc(animalCode)
                .stream()
                .map(mapper::toResponse)
                .toList();
    }
}
