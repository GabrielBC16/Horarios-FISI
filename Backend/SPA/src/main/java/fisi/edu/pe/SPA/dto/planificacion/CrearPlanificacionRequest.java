package fisi.edu.pe.SPA.dto.planificacion;

import java.util.UUID;

public record CrearPlanificacionRequest(
        UUID escuelaId,
        UUID periodoAcademicoId,
        UUID usuarioId) {
}
