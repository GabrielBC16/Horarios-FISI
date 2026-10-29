package fisi.edu.pe.SPA.dto.validacion;

import fisi.edu.pe.SPA.domain.enums.SituacionOferta;
import fisi.edu.pe.SPA.domain.enums.EstadoSeccion;
import fisi.edu.pe.SPA.dto.programacion.HorarioDto;
import java.util.List;
import java.util.UUID;

public record OfertaCursoConSeccionesDto(
        UUID id,
        CursoDto curso,
        Integer estudiantesEstimados,
        Integer capacidadAplicada,
        Integer seccionesCalculadas,
        Integer seccionesPropuestas,
        SituacionOferta situacion,
        String motivoDecision,
        Long version,
        List<SeccionDto> secciones) {

    public record CursoDto(UUID id, String codigo, String nombre, Short ciclo) {}

    public record SeccionDto(
            UUID id,
            String codigo,
            String profesor,
            Integer capacidad,
            Integer cuposPlan2018,
            Integer cuposPlan2023,
            EstadoSeccion estado,
            Long version,
            List<HorarioDto> horarios) {}
}
