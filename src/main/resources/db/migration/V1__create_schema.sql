CREATE TABLE rescue_centers(
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(20) NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL,
    city VARCHAR(100) NOT NULL
);

CREATE TABLE rescue_cases(
    id BIGSERIAL PRIMARY KEY,
    case_code VARCHAR(20) NOT NULL UNIQUE,
    rescue_date DATE NOT NULL,
    rescue_location VARCHAR(150) NOT NULL ,
    status VARCHAR(150) NOT NULL ,
    rescue_center_id BIGINT NOT NULL,

    CONSTRAINT fk_rescue_cases_rescue_center
        FOREIGN KEY (rescue_center_id) REFERENCES rescue_centers (id),

    CONSTRAINT ck_status CHECK ( status IN ('ADMITTED','UNDER_EVALUATION','IN_REHABILITATION',
                                            'READY_FOR_RELEASE','RELEASED','CLOSED'))
);

CREATE TABLE animals(
    id BIGSERIAL PRIMARY KEY,
    animal_code VARCHAR(20) UNIQUE NOT NULL,
    common_name VARCHAR(150) NOT NULL,
    scientific_name VARCHAR(150) NOT NULL,
    sex VARCHAR(150) NOT NULL,
    rescue_case_id BIGINT UNIQUE NOT NULL,

    CONSTRAINT fk_animals_rescue_case FOREIGN KEY (rescue_case_id) REFERENCES rescue_cases(id),
    CONSTRAINT ck_sex CHECK (  sex IN('MALE','FEMALE','UNKNOWN'))
);

CREATE TABLE medical_records(
    id BIGSERIAL PRIMARY KEY,
    animal_id BIGINT UNIQUE NOT NULL,
    initial_weight NUMERIC(6,2) NOT NULL,
    initial_condition VARCHAR(150) NOT NULL,
    injuries VARCHAR(150) NOT NULL,
    observations VARCHAR(150),

    CONSTRAINT fk_medical_records_animals FOREIGN KEY (animal_id) REFERENCES animals(id)
);

CREATE TABLE specialists(
    id BIGSERIAL PRIMARY KEY,
    professional_code VARCHAR(20) UNIQUE NOT NULL,
    first_name VARCHAR(150) NOT NULL,
    last_name VARCHAR(150) NOT NULL,
    email VARCHAR(150) UNIQUE NOT NULL,
    active BOOLEAN NOT NULL
);

CREATE TABLE expertise(
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(150) UNIQUE NOT NULL
);

CREATE TABLE specialist_expertise(
    specialist_id BIGINT NOT NULL,
    expertise_id BIGINT NOT NULL,

    CONSTRAINT fk_specialist_expertise_expertise FOREIGN KEY (expertise_id) REFERENCES expertise(id),
    CONSTRAINT fk_specialist_expertise_specialists FOREIGN KEY (specialist_id) REFERENCES specialists(id),

    CONSTRAINT pk_specialists_expertise PRIMARY KEY (specialist_id,expertise_id)
);

CREATE TABLE treatments(
    id BIGSERIAL PRIMARY KEY,
    animal_id BIGINT NOT NULL,
    specialist_id BIGINT NOT NULL,
    performed_at  TIMESTAMP NOT NULL,
    type VARCHAR(150) NOT NULL,
    description VARCHAR NOT NULL,

    CONSTRAINT fk_treatments_animals FOREIGN KEY (animal_id) REFERENCES animals(id),
    CONSTRAINT fk_treatments_specialists FOREIGN KEY (specialist_id) REFERENCES specialists(id)
);

CREATE INDEX idx_rescue_cases_rescue_center_id ON rescue_cases (rescue_center_id);
CREATE INDEX idx_rescue_cases_status ON rescue_cases (status);
CREATE INDEX idx_rescue_cases_rescue_date ON rescue_cases (rescue_date);
CREATE INDEX idx_treatments_animal_id ON treatments (animal_id);
CREATE INDEX idx_treatments_specialist_id ON treatments (specialist_id);
CREATE INDEX idx_treatments_performed_at ON treatments (performed_at);