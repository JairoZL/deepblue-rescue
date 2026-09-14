package com.deepblue.rescue.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "medical_records")
public class MedicalRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "initial_weight", nullable = false)
    private BigDecimal initialWeight;

    @Column(name = "initial_condition", nullable = false, length = 150)
    private String initialCondition;

    @Column(name = "injuries", nullable = false, length = 150)
    private String injuries;

    @Column(name = "observations", length = 150)
    private String observations;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "animal_id",unique = true,nullable = false)
    private Animal animal;

    public MedicalRecord() {
    }

    public MedicalRecord(BigDecimal initialWeight, String initialCondition, String injuries, String observations) {
        this.initialWeight = initialWeight;
        this.initialCondition = initialCondition;
        this.injuries = injuries;
        this.observations = observations;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public BigDecimal getInitialWeight() {
        return initialWeight;
    }

    public void setInitialWeight(BigDecimal initialWeight) {
        this.initialWeight = initialWeight;
    }

    public String getInitialCondition() {
        return initialCondition;
    }

    public void setInitialCondition(String initialCondition) {
        this.initialCondition = initialCondition;
    }

    public String getInjuries() {
        return injuries;
    }

    public void setInjuries(String injuries) {
        this.injuries = injuries;
    }

    public String getObservations() {
        return observations;
    }

    public void setObservations(String observations) {
        this.observations = observations;
    }

    public Animal getAnimal() {
        return animal;
    }

    public void setAnimal(Animal animal) {
        this.animal = animal;
    }
}