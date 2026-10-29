package fisi.edu.pe.SPA.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fisi.edu.pe.SPA.domain.entity.Curso;
import fisi.edu.pe.SPA.domain.entity.Escuela;
import fisi.edu.pe.SPA.domain.entity.OfertaCurso;
import fisi.edu.pe.SPA.domain.entity.PlanCurricular;
import fisi.edu.pe.SPA.domain.entity.Planificacion;
import fisi.edu.pe.SPA.domain.entity.Seccion;
import fisi.edu.pe.SPA.domain.entity.SeccionHorario;
import fisi.edu.pe.SPA.domain.entity.Usuario;
import fisi.edu.pe.SPA.dto.programacion.SeccionRequest;
import fisi.edu.pe.SPA.repository.ConvalidacionRespository;
import fisi.edu.pe.SPA.repository.OfertaCursoRepository;
import fisi.edu.pe.SPA.repository.PlanificacionRepository;
import fisi.edu.pe.SPA.repository.SeccionHorarioRepository;
import fisi.edu.pe.SPA.repository.SeccionRepository;
import fisi.edu.pe.SPA.repository.UsuarioRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

class EdicionProgramacionServiceTests {
    @Test
    void createsAnEditableSectionWithPlanCapacityAndAuditUser() {
        PlanificacionRepository planificacionRepository = org.mockito.Mockito.mock(PlanificacionRepository.class);
        OfertaCursoRepository ofertaCursoRepository = org.mockito.Mockito.mock(OfertaCursoRepository.class);
        SeccionRepository seccionRepository = org.mockito.Mockito.mock(SeccionRepository.class);
        SeccionHorarioRepository horarioRepository = org.mockito.Mockito.mock(SeccionHorarioRepository.class);
        ConvalidacionRespository convalidacionRepository = org.mockito.Mockito.mock(ConvalidacionRespository.class);
        UsuarioRepository usuarioRepository = org.mockito.Mockito.mock(UsuarioRepository.class);
        AuditoriaPlanificacionService auditoriaService =
                org.mockito.Mockito.mock(AuditoriaPlanificacionService.class);
        EdicionProgramacionService service = new EdicionProgramacionService(
                planificacionRepository,
                ofertaCursoRepository,
                seccionRepository,
                horarioRepository,
                convalidacionRepository,
                usuarioRepository,
                auditoriaService);

        UUID planificacionId = UUID.randomUUID();
        UUID ofertaId = UUID.randomUUID();
        UUID usuarioId = UUID.randomUUID();
        Planificacion planificacion = new Planificacion();
        ReflectionTestUtils.setField(planificacion, "id", planificacionId);
        Escuela escuela = new Escuela();
        PlanCurricular planCurricular = new PlanCurricular();
        planCurricular.setAreaEscuela(escuela);
        planCurricular.setAnio((short) 2023);
        Curso curso = new Curso();
        ReflectionTestUtils.setField(curso, "id", UUID.randomUUID());
        curso.setPlan(planCurricular);
        curso.setCodigo("MAT100");
        curso.setNombre("Matemáticas");
        OfertaCurso oferta = new OfertaCurso();
        ReflectionTestUtils.setField(oferta, "id", ofertaId);
        oferta.setPlanificacion(planificacion);
        oferta.setCurso(curso);

        Usuario usuario = new Usuario();
        ReflectionTestUtils.setField(usuario, "id", usuarioId);
        usuario.setActivo(true);
        usuario.setArea(escuela);
        when(planificacionRepository.findByIdForUpdate(planificacionId)).thenReturn(Optional.of(planificacion));
        when(ofertaCursoRepository.findById(ofertaId)).thenReturn(Optional.of(oferta));
        when(seccionRepository.existsByOfertaCurso_IdAndCodigoIgnoreCase(ofertaId, "S99"))
                .thenReturn(false);
        when(convalidacionRepository.findByCurso2023_Id(curso.getId())).thenReturn(List.of());
        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));
        doAnswer(invocation -> {
            Seccion seccion = invocation.getArgument(0);
            ReflectionTestUtils.setField(seccion, "id", UUID.randomUUID());
            return seccion;
        }).when(seccionRepository).saveAndFlush(any(Seccion.class));
        when(horarioRepository.findBySeccion_IdOrderByDiaAscHoraInicioAsc(any())).thenReturn(List.of());

        var section = service.crearSeccion(planificacionId, ofertaId,
                new SeccionRequest(usuarioId, " S99 ", "Docente", 35, 0, 30));

        assertEquals("S99", section.codigo());
        assertEquals("Docente", section.profesor());
        assertEquals(30, section.cuposPlan2023());
        verify(auditoriaService).establecerContexto(usuario, "Creación manual de sección");
    }

    @Test
    void createsScheduleAndRejectsOverlappingBlocks() {
        PlanificacionRepository planificacionRepository = org.mockito.Mockito.mock(PlanificacionRepository.class);
        OfertaCursoRepository ofertaCursoRepository = org.mockito.Mockito.mock(OfertaCursoRepository.class);
        SeccionRepository seccionRepository = org.mockito.Mockito.mock(SeccionRepository.class);
        SeccionHorarioRepository horarioRepository = org.mockito.Mockito.mock(SeccionHorarioRepository.class);
        ConvalidacionRespository convalidacionRepository = org.mockito.Mockito.mock(ConvalidacionRespository.class);
        UsuarioRepository usuarioRepository = org.mockito.Mockito.mock(UsuarioRepository.class);
        AuditoriaPlanificacionService auditoriaService =
                org.mockito.Mockito.mock(AuditoriaPlanificacionService.class);
        EdicionProgramacionService service = new EdicionProgramacionService(
                planificacionRepository, ofertaCursoRepository, seccionRepository,
                horarioRepository, convalidacionRepository, usuarioRepository, auditoriaService);

        UUID planificacionId = UUID.randomUUID();
        UUID seccionId = UUID.randomUUID();
        UUID usuarioId = UUID.randomUUID();
        Planificacion planificacion = new Planificacion();
        ReflectionTestUtils.setField(planificacion, "id", planificacionId);
        OfertaCurso oferta = new OfertaCurso();
        oferta.setPlanificacion(planificacion);
        Seccion seccion = new Seccion();
        ReflectionTestUtils.setField(seccion, "id", seccionId);
        seccion.setOfertaCurso(oferta);
        Usuario usuario = new Usuario();
        ReflectionTestUtils.setField(usuario, "id", usuarioId);
        usuario.setActivo(true);
        usuario.setArea(new Escuela());

        when(planificacionRepository.findByIdForUpdate(planificacionId)).thenReturn(Optional.of(planificacion));
        when(seccionRepository.findById(seccionId)).thenReturn(Optional.of(seccion));
        when(horarioRepository.findBySeccion_IdOrderByDiaAscHoraInicioAsc(seccionId)).thenReturn(List.of());
        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));
        doAnswer(invocation -> {
            SeccionHorario horario = invocation.getArgument(0);
            ReflectionTestUtils.setField(horario, "id", UUID.randomUUID());
            return horario;
        }).when(horarioRepository).saveAndFlush(any(SeccionHorario.class));

        var horario = service.crearHorario(planificacionId, seccionId,
                new fisi.edu.pe.SPA.dto.programacion.HorarioRequest(
                        usuarioId, (short) 1,
                        java.time.LocalTime.of(8, 0), java.time.LocalTime.of(10, 0)));

        assertEquals((short) 1, horario.dia());
        assertEquals(java.time.LocalTime.of(8, 0), horario.horaInicio());
        verify(auditoriaService).establecerContexto(usuario, "Creación de horario de sección");

        SeccionHorario existente = new SeccionHorario();
        ReflectionTestUtils.setField(existente, "id", UUID.randomUUID());
        existente.setDia((short) 1);
        existente.setHoraInicio(java.time.LocalTime.of(9, 0));
        existente.setHoraFin(java.time.LocalTime.of(10, 0));
        when(horarioRepository.findBySeccion_IdOrderByDiaAscHoraInicioAsc(seccionId))
                .thenReturn(List.of(existente));

        assertThrows(ResponseStatusException.class, () -> service.crearHorario(planificacionId, seccionId,
                new fisi.edu.pe.SPA.dto.programacion.HorarioRequest(
                        usuarioId, (short) 1,
                        java.time.LocalTime.of(9, 30), java.time.LocalTime.of(11, 0))));
        verify(horarioRepository, times(1)).saveAndFlush(any(SeccionHorario.class));
    }
}
