package fisi.edu.pe.SPA.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fisi.edu.pe.SPA.domain.entity.Escuela;
import fisi.edu.pe.SPA.domain.entity.PeriodoAcademico;
import fisi.edu.pe.SPA.domain.entity.Planificacion;
import fisi.edu.pe.SPA.domain.entity.Usuario;
import fisi.edu.pe.SPA.dto.planificacion.CrearPlanificacionRequest;
import fisi.edu.pe.SPA.domain.enums.EstadoPlanificacion;
import fisi.edu.pe.SPA.repository.CambioPlanificacionRepository;
import fisi.edu.pe.SPA.repository.EscuelaRepository;
import fisi.edu.pe.SPA.repository.ImportacionRepository;
import fisi.edu.pe.SPA.repository.IntercambioExcelRepository;
import fisi.edu.pe.SPA.repository.OfertaCursoRepository;
import fisi.edu.pe.SPA.repository.PeriodoAcademicoRepository;
import fisi.edu.pe.SPA.repository.PlanificacionRepository;
import fisi.edu.pe.SPA.repository.SeccionHorarioRepository;
import fisi.edu.pe.SPA.repository.SeccionRepository;
import fisi.edu.pe.SPA.repository.UsuarioRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

class PlanificacionServiceTests {
    @Test
    void createsDraftPlanningForSchoolAndPeriod() {
        PlanificacionRepository planificacionRepository = org.mockito.Mockito.mock(PlanificacionRepository.class);
        EscuelaRepository escuelaRepository = org.mockito.Mockito.mock(EscuelaRepository.class);
        PeriodoAcademicoRepository periodoRepository = org.mockito.Mockito.mock(PeriodoAcademicoRepository.class);
        UsuarioRepository usuarioRepository = org.mockito.Mockito.mock(UsuarioRepository.class);
        AuditoriaPlanificacionService auditoriaService =
                org.mockito.Mockito.mock(AuditoriaPlanificacionService.class);
        PlanificacionService service = new PlanificacionService(
                planificacionRepository,
                escuelaRepository,
                periodoRepository,
                usuarioRepository,
                auditoriaService,
                org.mockito.Mockito.mock(SeccionHorarioRepository.class),
                org.mockito.Mockito.mock(SeccionRepository.class),
                org.mockito.Mockito.mock(OfertaCursoRepository.class),
                org.mockito.Mockito.mock(CambioPlanificacionRepository.class),
                org.mockito.Mockito.mock(IntercambioExcelRepository.class),
                org.mockito.Mockito.mock(ImportacionRepository.class));

        UUID escuelaId = UUID.randomUUID();
        UUID periodoId = UUID.randomUUID();
        UUID usuarioId = UUID.randomUUID();
        Escuela escuela = new Escuela();
        ReflectionTestUtils.setField(escuela, "id", escuelaId);
        escuela.setCodigo("SW");
        escuela.setNombre("Ingeniería de Software");
        Usuario usuario = new Usuario();
        ReflectionTestUtils.setField(usuario, "id", usuarioId);
        usuario.setActivo(true);
        PeriodoAcademico periodo = new PeriodoAcademico();
        ReflectionTestUtils.setField(periodo, "id", periodoId);
        periodo.setCodigo("2026-2");

        when(escuelaRepository.findById(escuelaId)).thenReturn(Optional.of(escuela));
        when(periodoRepository.findById(periodoId)).thenReturn(Optional.of(periodo));
        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));
        when(planificacionRepository.findByAreaEscuela_IdAndPeriodoAcademico_Id(escuelaId, periodoId))
                .thenReturn(Optional.empty());
        when(planificacionRepository.saveAndFlush(org.mockito.ArgumentMatchers.any(Planificacion.class)))
                .thenAnswer(invocation -> {
                    Planificacion planificacion = invocation.getArgument(0);
                    ReflectionTestUtils.setField(planificacion, "id", UUID.randomUUID());
                    return planificacion;
                });

        var result = service.crear(new CrearPlanificacionRequest(escuelaId, periodoId, usuarioId));

        assertEquals(escuelaId, result.escuelaId());
        assertEquals(periodoId, result.periodoAcademicoId());
        assertEquals(EstadoPlanificacion.BORRADOR, result.estado());
        verify(auditoriaService).establecerContexto(usuario, "Creación de planificación académica");
    }

    @Test
    void deletesPlanningAndDependentsInForeignKeyOrder() {
        PlanificacionRepository planificacionRepository = org.mockito.Mockito.mock(PlanificacionRepository.class);
        EscuelaRepository escuelaRepository = org.mockito.Mockito.mock(EscuelaRepository.class);
        PeriodoAcademicoRepository periodoRepository = org.mockito.Mockito.mock(PeriodoAcademicoRepository.class);
        UsuarioRepository usuarioRepository = org.mockito.Mockito.mock(UsuarioRepository.class);
        AuditoriaPlanificacionService auditoriaService =
                org.mockito.Mockito.mock(AuditoriaPlanificacionService.class);
        SeccionHorarioRepository seccionHorarioRepository =
                org.mockito.Mockito.mock(SeccionHorarioRepository.class);
        SeccionRepository seccionRepository = org.mockito.Mockito.mock(SeccionRepository.class);
        OfertaCursoRepository ofertaCursoRepository = org.mockito.Mockito.mock(OfertaCursoRepository.class);
        CambioPlanificacionRepository cambioPlanificacionRepository =
                org.mockito.Mockito.mock(CambioPlanificacionRepository.class);
        IntercambioExcelRepository intercambioExcelRepository =
                org.mockito.Mockito.mock(IntercambioExcelRepository.class);
        ImportacionRepository importacionRepository = org.mockito.Mockito.mock(ImportacionRepository.class);
        PlanificacionService service = new PlanificacionService(
                planificacionRepository,
                escuelaRepository,
                periodoRepository,
                usuarioRepository,
                auditoriaService,
                seccionHorarioRepository,
                seccionRepository,
                ofertaCursoRepository,
                cambioPlanificacionRepository,
                intercambioExcelRepository,
                importacionRepository);

        UUID planificacionId = UUID.randomUUID();
        UUID usuarioId = UUID.randomUUID();
        Planificacion planificacion = new Planificacion();
        Usuario usuario = new Usuario();
        usuario.setActivo(true);
        when(planificacionRepository.findByIdForUpdate(planificacionId))
                .thenReturn(Optional.of(planificacion));
        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));

        service.eliminar(planificacionId, usuarioId);

        InOrder orden = inOrder(
                auditoriaService,
                seccionHorarioRepository,
                seccionRepository,
                ofertaCursoRepository,
                cambioPlanificacionRepository,
                intercambioExcelRepository,
                importacionRepository,
                planificacionRepository);
        orden.verify(auditoriaService)
                .establecerContexto(usuario, "Eliminación de planificación académica");
        orden.verify(seccionHorarioRepository).deleteAllByPlanificacionId(planificacionId);
        orden.verify(seccionRepository).deleteAllByPlanificacionId(planificacionId);
        orden.verify(ofertaCursoRepository).deleteAllByPlanificacionId(planificacionId);
        orden.verify(cambioPlanificacionRepository).deleteAllByPlanificacionId(planificacionId);
        orden.verify(intercambioExcelRepository).deleteAllByPlanificacionId(planificacionId);
        orden.verify(importacionRepository).deleteAllByPlanificacionId(planificacionId);
        orden.verify(planificacionRepository).delete(planificacion);
    }

    @Test
    void doesNotDeleteDependentsWhenPlanningDoesNotExist() {
        PlanificacionRepository planificacionRepository = org.mockito.Mockito.mock(PlanificacionRepository.class);
        PlanificacionService service = new PlanificacionService(
                planificacionRepository,
                org.mockito.Mockito.mock(EscuelaRepository.class),
                org.mockito.Mockito.mock(PeriodoAcademicoRepository.class),
                org.mockito.Mockito.mock(UsuarioRepository.class),
                org.mockito.Mockito.mock(AuditoriaPlanificacionService.class),
                org.mockito.Mockito.mock(SeccionHorarioRepository.class),
                org.mockito.Mockito.mock(SeccionRepository.class),
                org.mockito.Mockito.mock(OfertaCursoRepository.class),
                org.mockito.Mockito.mock(CambioPlanificacionRepository.class),
                org.mockito.Mockito.mock(IntercambioExcelRepository.class),
                org.mockito.Mockito.mock(ImportacionRepository.class));
        UUID planificacionId = UUID.randomUUID();
        UUID usuarioId = UUID.randomUUID();
        when(planificacionRepository.findByIdForUpdate(planificacionId)).thenReturn(Optional.empty());

        ResponseStatusException exception =
                assertThrows(ResponseStatusException.class, () -> service.eliminar(planificacionId, usuarioId));
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(planificacionRepository).findByIdForUpdate(planificacionId);
    }
}
