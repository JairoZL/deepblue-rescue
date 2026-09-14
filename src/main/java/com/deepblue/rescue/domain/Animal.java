package com.deepblue.rescue.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "animals")
public class Animal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "animal_code",nullable = false,unique = true,length = 20)
    private String animalCode;

    @Column(name = "common_name",nullable = false,length = 150)
    private String commonName;

    @Column(name = "scientific_name",nullable = false,length = 150)
    private String scientificName;

    @Enumerated(EnumType.STRING)
    @Column(name = "sex",nullable = false)
    private AnimalSex sex;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rescue_case_id",nullable = false,unique = true)
    private RescueCase rescueCase;

    @OneToOne(mappedBy = "animal",fetch = FetchType.LAZY,cascade = CascadeType.ALL,orphanRemoval = true)
    private MedicalRecord medicalRecord;

    public void assignMedicalRecord(MedicalRecord medicalRecord) {
        this.medicalRecord = medicalRecord;
        medicalRecord.setAnimal(this);
    };

    public Animal() {
    }

    public Animal(String animalCode, String commonName, String scientificName, AnimalSex sex) {
        this.animalCode = animalCode;
        this.commonName = commonName;
        this.scientificName = scientificName;
        this.sex = sex;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAnimalCode() {
        return animalCode;
    }

    public void setAnimalCode(String animalCode) {
        this.animalCode = animalCode;
    }

    public String getCommonName() {
        return commonName;
    }

    public void setCommonName(String commonName) {
        this.commonName = commonName;
    }

    public String getScientificName() {
        return scientificName;
    }

    public void setScientificName(String scientificName) {
        this.scientificName = scientificName;
    }

    public AnimalSex getSex() {
        return sex;
    }

    public void setSex(AnimalSex sex) {
        this.sex = sex;
    }

    public RescueCase getRescueCase() {
        return rescueCase;
    }

    public void setRescueCase(RescueCase rescueCase) {
        this.rescueCase = rescueCase;
    }

    public MedicalRecord getMedicalRecord() {
        return medicalRecord;
    }
}
