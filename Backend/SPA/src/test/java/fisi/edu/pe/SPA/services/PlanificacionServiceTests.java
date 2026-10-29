package fisi.edu.pe.SPA.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fisi.edu.pe.SPA.domain.entity.Escuela;
import fisi.edu.pe.SPA.domain.entity.PeriodoAcademico;
import fisi.edu.pe.SPA.domain.entity.Planificacion;
import fisi.edu.pe.SPA.domain.entity.Usuario;
import fisi.edu.pe.SPA.dto.planificacion.CrearPlanificacionRequest;
import fisi.edu.pe.SPA.domain.enums.EstadoPlanificacion;
import fisi.edu.pe.SPA.repository.EscuelaRepository;
import fisi.edu.pe.SPA.repository.PeriodoAcademicoRepository;
import fisi.edu.pe.SPA.repository.PlanificacionRepository;
import fisi.edu.pe.SPA.repository.UsuarioRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

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
                auditoriaService);

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
}
