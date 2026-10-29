package fisi.edu.pe.SPA.dto.programacion;

import java.time.LocalTime;
import java.util.UUID;

public record HorarioDto(
        UUID id,
        Short dia,
        LocalTime horaInicio,
        LocalTime horaFin,
        Long version) {
}
