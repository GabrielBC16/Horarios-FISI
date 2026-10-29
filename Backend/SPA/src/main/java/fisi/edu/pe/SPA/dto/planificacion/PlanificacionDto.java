package fisi.edu.pe.SPA.dto.planificacion;

import fisi.edu.pe.SPA.domain.enums.EstadoPlanificacion;
import java.util.UUID;

public record PlanificacionDto(
        UUID id,
        UUID escuelaId,
        String escuela,
        UUID periodoAcademicoId,
        String periodoAcademico,
        UUID creadoPor,
        EstadoPlanificacion estado,
        Long version) {
}
