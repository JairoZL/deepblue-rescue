package com.deepblue.rescue.domain;

public enum RescueStatus {
    ADMITTED,
    UNDER_EVALUATION,
    IN_REHABILITATION,
    READY_FOR_RELEASE,
    RELEASED,
    CLOSED;

    /**
     * Un animal solo puede recibir tratamientos mientras su caso está
     * en evaluación o en rehabilitación.
     */
    public boolean allowsTreatments() {
        return this == UNDER_EVALUATION || this == IN_REHABILITATION;
    }
}
