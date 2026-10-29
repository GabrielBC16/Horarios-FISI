package fisi.edu.pe.SPA.dto.planificacion;

import java.time.LocalDate;
import java.util.UUID;

public record PeriodoAcademicoDto(
        UUID id,
        String codigo,
        LocalDate fechaInicio,
        LocalDate fechaFin) {
}
