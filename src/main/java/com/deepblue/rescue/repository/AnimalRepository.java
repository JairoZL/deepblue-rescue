package com.deepblue.rescue.repository;

import com.deepblue.rescue.domain.Animal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AnimalRepository extends JpaRepository<Animal, Long> {

    Optional<Animal> findByAnimalCode(String animalCode);
}