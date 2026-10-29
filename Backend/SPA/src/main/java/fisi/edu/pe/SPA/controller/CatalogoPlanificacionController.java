package fisi.edu.pe.SPA.controller;

import fisi.edu.pe.SPA.dto.planificacion.EscuelaDto;
import fisi.edu.pe.SPA.dto.planificacion.PeriodoAcademicoDto;
import fisi.edu.pe.SPA.repository.EscuelaRepository;
import fisi.edu.pe.SPA.repository.PeriodoAcademicoRepository;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class CatalogoPlanificacionController {
    private final EscuelaRepository escuelaRepository;
    private final PeriodoAcademicoRepository periodoRepository;

    public CatalogoPlanificacionController(
            EscuelaRepository escuelaRepository,
            PeriodoAcademicoRepository periodoRepository) {
        this.escuelaRepository = escuelaRepository;
        this.periodoRepository = periodoRepository;
    }

    @GetMapping("/escuelas")
    public List<EscuelaDto> listarEscuelas() {
        return escuelaRepository.findByActivoTrueOrderByNombreAsc().stream()
                .map(escuela -> new EscuelaDto(
                        escuela.getId(), escuela.getCodigo(), escuela.getNombre()))
                .toList();
    }

    @GetMapping("/periodos-academicos")
    public List<PeriodoAcademicoDto> listarPeriodos() {
        return periodoRepository.findAllByOrderByCodigoDesc().stream()
                .map(periodo -> new PeriodoAcademicoDto(
                        periodo.getId(),
                        periodo.getCodigo(),
                        periodo.getFechaInicio(),
                        periodo.getFechaFin()))
                .toList();
    }
}
