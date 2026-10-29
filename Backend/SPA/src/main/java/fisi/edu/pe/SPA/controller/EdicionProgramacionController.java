package fisi.edu.pe.SPA.controller;

import fisi.edu.pe.SPA.dto.programacion.ActualizarHorarioRequest;
import fisi.edu.pe.SPA.dto.programacion.ActualizarOfertaRequest;
import fisi.edu.pe.SPA.dto.programacion.ActualizarSeccionRequest;
import fisi.edu.pe.SPA.dto.programacion.HorarioDto;
import fisi.edu.pe.SPA.dto.programacion.HorarioRequest;
import fisi.edu.pe.SPA.dto.programacion.OfertaCursoDto;
import fisi.edu.pe.SPA.dto.programacion.SeccionDto;
import fisi.edu.pe.SPA.dto.programacion.SeccionRequest;
import fisi.edu.pe.SPA.services.EdicionProgramacionService;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/planificaciones/{planificacionId}")
public class EdicionProgramacionController {
    private final EdicionProgramacionService edicionService;

    public EdicionProgramacionController(EdicionProgramacionService edicionService) {
        this.edicionService = edicionService;
    }

    @PutMapping("/ofertas/{ofertaId}")
    public OfertaCursoDto actualizarOferta(
            @PathVariable("planificacionId") UUID planificacionId,
            @PathVariable("ofertaId") UUID ofertaId,
            @RequestBody ActualizarOfertaRequest request) {
        return edicionService.actualizarOferta(planificacionId, ofertaId, request);
    }

    @PostMapping("/ofertas/{ofertaId}/secciones")
    @ResponseStatus(HttpStatus.CREATED)
    public SeccionDto crearSeccion(
            @PathVariable("planificacionId") UUID planificacionId,
            @PathVariable("ofertaId") UUID ofertaId,
            @RequestBody SeccionRequest request) {
        return edicionService.crearSeccion(planificacionId, ofertaId, request);
    }

    @PutMapping("/secciones/{seccionId}")
    public SeccionDto actualizarSeccion(
            @PathVariable("planificacionId") UUID planificacionId,
            @PathVariable("seccionId") UUID seccionId,
            @RequestBody ActualizarSeccionRequest request) {
        return edicionService.actualizarSeccion(planificacionId, seccionId, request);
    }

    @DeleteMapping("/secciones/{seccionId}")
    public SeccionDto retirarSeccion(
            @PathVariable("planificacionId") UUID planificacionId,
            @PathVariable("seccionId") UUID seccionId,
            @RequestParam UUID usuarioId,
            @RequestParam Long version) {
        return edicionService.retirarSeccion(planificacionId, seccionId, usuarioId, version);
    }

    @PostMapping("/secciones/{seccionId}/horarios")
    @ResponseStatus(HttpStatus.CREATED)
    public HorarioDto crearHorario(
            @PathVariable("planificacionId") UUID planificacionId,
            @PathVariable("seccionId") UUID seccionId,
            @RequestBody HorarioRequest request) {
        return edicionService.crearHorario(planificacionId, seccionId, request);
    }

    @PutMapping("/secciones/{seccionId}/horarios/{horarioId}")
    public HorarioDto actualizarHorario(
            @PathVariable("planificacionId") UUID planificacionId,
            @PathVariable("seccionId") UUID seccionId,
            @PathVariable("horarioId") UUID horarioId,
            @RequestBody ActualizarHorarioRequest request) {
        return edicionService.actualizarHorario(planificacionId, seccionId, horarioId, request);
    }

    @DeleteMapping("/secciones/{seccionId}/horarios/{horarioId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminarHorario(
            @PathVariable("planificacionId") UUID planificacionId,
            @PathVariable("seccionId") UUID seccionId,
            @PathVariable("horarioId") UUID horarioId,
            @RequestParam UUID usuarioId) {
        edicionService.eliminarHorario(planificacionId, seccionId, horarioId, usuarioId);
    }
}

