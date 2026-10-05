package com.deepblue.rescue.repository;

import com.deepblue.rescue.domain.Animal;
import com.deepblue.rescue.domain.RescueStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AnimalRepository extends JpaRepository<Animal, Long> {

    Optional<Animal> findByAnimalCode(String animalCode);
    List<Animal> findByCommonNameContainingIgnoreCase(String commonName);
    List<Animal> findByRescueCaseStatus(RescueStatus rescueCaseStatus);
    List<Animal> findByRescueCaseRescueCenterCode(String rescueCenterCode);

    // Parte XIII - Reto sin guia:
    // animales cuyo caso esta en determinado status Y que recibieron al menos
    // un tratamiento de un especialista con cierta expertise (ignorando mayusculas).
    // Requiere @Query porque "al menos un tratamiento de un especialista con tal
    // expertise" exige navegar animal -> treatments -> specialist -> expertiseAreas
    // y DISTINCT para no duplicar el animal si tuvo varios tratamientos que matchean;
    // DISTINCT no tiene equivalente como Query Method.
    @Query("""
        select distinct a
        from Animal a
        join a.rescueCase rc
        join a.treatments t
        join t.specialist s
        join s.expertiseAreas e
        where rc.status = :status
          and LOWER(e.name) = LOWER(:expertiseName)
        """)
    List<Animal> findByStatusAndTreatedBySpecialistWithExpertise(@Param("status") RescueStatus status,
                                                                  @Param("expertiseName") String expertiseName);
}
