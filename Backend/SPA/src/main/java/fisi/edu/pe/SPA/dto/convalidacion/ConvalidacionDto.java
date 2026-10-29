package fisi.edu.pe.SPA.dto.convalidacion;

import java.util.UUID;

public record ConvalidacionDto(
        UUID id,
        CursoDto curso2018,
        CursoDto curso2023) {

    public record CursoDto(
            UUID id,
            String codigo,
            String nombre,
            Short ciclo,
            Short anioPlan,
            UUID escuelaId) {
    }
}
