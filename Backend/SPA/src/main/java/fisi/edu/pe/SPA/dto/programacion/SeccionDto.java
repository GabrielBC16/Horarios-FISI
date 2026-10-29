package fisi.edu.pe.SPA.dto.programacion;

import fisi.edu.pe.SPA.domain.enums.EstadoSeccion;
import java.util.List;
import java.util.UUID;

public record SeccionDto(
        UUID id,
        String codigo,
        String profesor,
        Integer capacidad,
        Integer cuposPlan2018,
        Integer cuposPlan2023,
        EstadoSeccion estado,
        Long version,
        List<HorarioDto> horarios) {
}
