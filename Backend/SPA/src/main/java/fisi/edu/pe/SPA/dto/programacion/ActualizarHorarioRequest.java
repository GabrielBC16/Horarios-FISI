package fisi.edu.pe.SPA.dto.programacion;

import java.time.LocalTime;
import java.util.UUID;

public record ActualizarHorarioRequest(
        UUID usuarioId,
        Long version,
        Short dia,
        LocalTime horaInicio,
        LocalTime horaFin) {
}
