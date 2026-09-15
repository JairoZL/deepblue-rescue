package com.deepblue.rescue.repository;

import com.deepblue.rescue.domain.Treatment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TreatmentRepository extends JpaRepository<Treatment, Long> {

    List<Treatment> findByAnimalAnimalCodeOrderByPerformedAtAsc(String animalCode);
}