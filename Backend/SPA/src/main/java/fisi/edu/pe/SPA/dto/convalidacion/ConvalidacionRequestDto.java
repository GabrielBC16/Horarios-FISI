package fisi.edu.pe.SPA.dto.convalidacion;

import java.util.UUID;

public record ConvalidacionRequestDto(
        UUID escuelaId,
        CursoReferenciaDto curso2018,
        CursoReferenciaDto curso2023) {
}
