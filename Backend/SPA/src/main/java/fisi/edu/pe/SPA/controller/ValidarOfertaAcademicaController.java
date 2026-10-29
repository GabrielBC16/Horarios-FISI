package fisi.edu.pe.SPA.controller;

import java.util.List;
import java.util.UUID;
import fisi.edu.pe.SPA.dto.validacion.OfertaCursoConSeccionesDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import fisi.edu.pe.SPA.services.ValidarOfertaAcademicaService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/validar")
public class ValidarOfertaAcademicaController {

    private static final Logger log = LoggerFactory.getLogger(ValidarOfertaAcademicaController.class);
    private final ValidarOfertaAcademicaService validarOfertaAcademicaService;

    public ValidarOfertaAcademicaController(ValidarOfertaAcademicaService validarOfertaAcademicaService) {
        this.validarOfertaAcademicaService = validarOfertaAcademicaService;
    }

    @GetMapping("/verSeccionesCalculadas/{planificacionId}")
    public List<OfertaCursoConSeccionesDto> getVerSeccionesCalculadas(
            @PathVariable("planificacionId") UUID planificacionId) {
        log.info("Solicitud recibida para obtener cursos y secciones; planificacionId={}", planificacionId);
        List<OfertaCursoConSeccionesDto> resultado = validarOfertaAcademicaService
                .getOfertaAcademicaByPlanificacionId(planificacionId);
        log.info("Solicitud completada para planificacionId={}; ofertas={}", planificacionId, resultado.size());
        return resultado;
    }

}
