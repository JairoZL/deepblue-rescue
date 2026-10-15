package com.deepblue.rescue.service.impl;

import com.deepblue.rescue.dto.response.SpecialistResponse;
import com.deepblue.rescue.exception.ResourceNotFoundException;
import com.deepblue.rescue.mapper.SpecialistMapper;
import com.deepblue.rescue.repository.SpecialistRepository;
import com.deepblue.rescue.service.SpecialistService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * Consultas de solo lectura sobre especialistas.
 * <p>
 * Limitación conocida (N+1): {@code Specialist.expertiseAreas} es una colección perezosa.
 * Al mapear cada especialista, {@link SpecialistMapper} la recorre y Hibernate lanza una
 * consulta adicional por especialista. Se acepta porque el volumen es pequeño; la
 * transacción de solo lectura mantiene la sesión abierta para que la carga perezosa funcione.
 */
@Service
@Validated
@Transactional(readOnly = true)
public class SpecialistServiceImpl implements SpecialistService {

    private final SpecialistRepository repository;
    private final SpecialistMapper mapper;

    public SpecialistServiceImpl(SpecialistRepository repository,
                                 SpecialistMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public SpecialistResponse findByCode(String professionalCode) {
        return repository.findByProfessionalCode(professionalCode)
                .map(mapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Specialist %s does not exist.".formatted(professionalCode)));
    }

    @Override
    public List<SpecialistResponse> findAllSpecialists() {
        return repository.findAll()
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Override
    public List<SpecialistResponse> findActiveByExpertise(String expertiseName) {
        return repository.findActiveByExpertise(expertiseName)
                .stream()
                .map(mapper::toResponse)
                .toList();
    }
}
