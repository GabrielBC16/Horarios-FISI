package fisi.edu.pe.SPA.dto.programacion;

import java.time.LocalTime;
import java.util.UUID;

public record HorarioRequest(
        UUID usuarioId,
        Short dia,
        LocalTime horaInicio,
        LocalTime horaFin) {
}
