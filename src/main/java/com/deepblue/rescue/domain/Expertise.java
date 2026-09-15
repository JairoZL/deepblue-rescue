    package com.deepblue.rescue.domain;

    import jakarta.persistence.*;

    import java.util.HashSet;
    import java.util.Set;

    @Entity
    @Table(name = "expertise")
    public class Expertise {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        @Column(name = "id")
        private Long id;

        @Column(name = "name",unique = true,nullable = false,length = 150)
        private String name;

        @ManyToMany(mappedBy = "expertiseAreas")
        private Set<Specialist> specialists = new HashSet<>();

        public Expertise() {
        }

        public Expertise(String name) {
            this.name = name;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Set<Specialist> getSpecialists() {
            return specialists;
        }

    }


