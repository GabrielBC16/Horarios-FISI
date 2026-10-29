package fisi.edu.pe.SPA.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import fisi.edu.pe.SPA.domain.entity.Convalidacion;
import fisi.edu.pe.SPA.domain.entity.Curso;
import fisi.edu.pe.SPA.domain.entity.Escuela;
import fisi.edu.pe.SPA.domain.entity.Importacion;
import fisi.edu.pe.SPA.domain.entity.OfertaCurso;
import fisi.edu.pe.SPA.domain.entity.PlanCurricular;
import fisi.edu.pe.SPA.domain.entity.Planificacion;
import fisi.edu.pe.SPA.domain.entity.Seccion;
import fisi.edu.pe.SPA.domain.enums.EstadoImportacion;
import fisi.edu.pe.SPA.dto.importacion.FilaImportacionDto;
import fisi.edu.pe.SPA.repository.ConvalidacionRespository;
import fisi.edu.pe.SPA.repository.CursoRepository;
import fisi.edu.pe.SPA.repository.ImportacionRepository;
import fisi.edu.pe.SPA.repository.OfertaCursoRepository;
import fisi.edu.pe.SPA.repository.SeccionRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class CalcularOfertaAcademicaServiceTests {
    @Test
    void combinesConvalidatedDemandAndAllocatesProportionalSectionQuotas() {
        ImportacionRepository importacionRepository = org.mockito.Mockito.mock(ImportacionRepository.class);
        CursoRepository cursoRepository = org.mockito.Mockito.mock(CursoRepository.class);
        ConvalidacionRespository convalidacionRepository = org.mockito.Mockito.mock(ConvalidacionRespository.class);
        OfertaCursoRepository ofertaCursoRepository = org.mockito.Mockito.mock(OfertaCursoRepository.class);
        SeccionRepository seccionRepository = org.mockito.Mockito.mock(SeccionRepository.class);
        AuditoriaPlanificacionService auditoriaService =
                org.mockito.Mockito.mock(AuditoriaPlanificacionService.class);

        CalcularOfertaAcademicaService service = new CalcularOfertaAcademicaService(
                new ObjectMapper(),
                importacionRepository,
                cursoRepository,
                convalidacionRepository,
                ofertaCursoRepository,
                seccionRepository,
                auditoriaService);

        UUID planificacionId = UUID.randomUUID();
        UUID importacionId = UUID.randomUUID();
        Planificacion planificacion = new Planificacion();
        asignarId(planificacion, planificacionId);

        Importacion importacion = new Importacion();
        asignarId(importacion, importacionId);
        importacion.setPlanificacion(planificacion);
        importacion.setEstado(EstadoImportacion.VALIDADA);
        importacion.setDetalleValidacion(Map.of("filas", List.of(
                new FilaImportacionDto(1, "MAT101", "Álgebra", "2018", 20,
                        UUID.fromString("00000000-0000-0000-0000-000000000018"), true, null),
                new FilaImportacionDto(2, "MAT102", "Cálculo", "2018", 10,
                        UUID.fromString("00000000-0000-0000-0000-000000000019"), true, null),
                new FilaImportacionDto(3, "MAT100", "Matemáticas", "2023", 30,
                        UUID.fromString("00000000-0000-0000-0000-000000000023"), true, null))));

        Escuela escuela = new Escuela();
        asignarId(escuela, UUID.randomUUID());
        Curso curso2018 = curso(escuela, (short) 2018, "MAT101", "Álgebra",
                UUID.fromString("00000000-0000-0000-0000-000000000018"));
        Curso otroCurso2018 = curso(escuela, (short) 2018, "MAT102", "Cálculo",
                UUID.fromString("00000000-0000-0000-0000-000000000019"));
        Curso curso2023 = curso(escuela, (short) 2023, "MAT100", "Matemáticas",
                UUID.fromString("00000000-0000-0000-0000-000000000023"));

        Convalidacion convalidacion = new Convalidacion();
        convalidacion.setCursoConvalidado2018(curso2018);
        convalidacion.setCurso2023(curso2023);
        Convalidacion otraConvalidacion = new Convalidacion();
        otraConvalidacion.setCursoConvalidado2018(otroCurso2018);
        otraConvalidacion.setCurso2023(curso2023);

        when(importacionRepository.findById(importacionId)).thenReturn(Optional.of(importacion));
        when(cursoRepository.findById(curso2018.getId())).thenReturn(Optional.of(curso2018));
        when(cursoRepository.findById(otroCurso2018.getId())).thenReturn(Optional.of(otroCurso2018));
        when(cursoRepository.findById(curso2023.getId())).thenReturn(Optional.of(curso2023));
        when(convalidacionRepository.findAll()).thenReturn(List.of(convalidacion, otraConvalidacion));
        when(ofertaCursoRepository.findByPlanificacion_IdAndCurso_Id(planificacionId, curso2018.getId()))
                .thenReturn(Optional.empty());
        when(ofertaCursoRepository.findByPlanificacion_IdAndCurso_Id(planificacionId, otroCurso2018.getId()))
                .thenReturn(Optional.empty());
        when(ofertaCursoRepository.findByPlanificacion_IdAndCurso_Id(planificacionId, curso2023.getId()))
                .thenReturn(Optional.empty());
        when(seccionRepository.findByOfertaCurso_IdIn(any())).thenReturn(List.of());

        List<OfertaCurso> ofertasGuardadas = new ArrayList<>();
        doAnswer(invocation -> {
            OfertaCurso oferta = invocation.getArgument(0);
            if (oferta.getId() == null) {
                asignarId(oferta, UUID.randomUUID());
            }
            if (!ofertasGuardadas.contains(oferta)) {
                ofertasGuardadas.add(oferta);
            }
            return oferta;
        }).when(ofertaCursoRepository).save(any(OfertaCurso.class));
        doAnswer(invocation -> invocation.getArgument(0))
                .when(seccionRepository).save(any(Seccion.class));
        when(ofertaCursoRepository.findByPlanificacion_Id(planificacionId)).thenAnswer(
                invocation -> List.copyOf(ofertasGuardadas));

        var resultado = service.procesarImportacion(importacionId);
        List<Seccion> secciones = capturadas(seccionRepository);

        assertEquals(3, resultado.ofertasActualizadas());
        assertEquals(2, resultado.seccionesPropuestas());
        assertEquals(2, secciones.size());
        assertEquals(30, secciones.stream().mapToInt(Seccion::getCuposPlan2018).sum());
        assertEquals(30, secciones.stream().mapToInt(Seccion::getCuposPlan2023).sum());
        assertTrue(secciones.stream().allMatch(seccion ->
                seccion.getCuposPlan2018() + seccion.getCuposPlan2023() <= seccion.getCapacidad()));
        assertTrue(secciones.stream().allMatch(seccion -> seccion.getCapacidad() == 35));
        assertEquals(EstadoImportacion.APLICADA, importacion.getEstado());
    }

    private List<Seccion> capturadas(SeccionRepository repository) {
        var captor = org.mockito.ArgumentCaptor.forClass(Seccion.class);
        org.mockito.Mockito.verify(repository, org.mockito.Mockito.times(2)).save(captor.capture());
        return captor.getAllValues();
    }

    private Curso curso(Escuela escuela, Short anioPlan, String codigo, String nombre, UUID id) {
        PlanCurricular plan = new PlanCurricular();
        plan.setAreaEscuela(escuela);
        plan.setAnio(anioPlan);
        plan.setActivo(true);

        Curso curso = new Curso();
        asignarId(curso, id);
        curso.setPlan(plan);
        curso.setCodigo(codigo);
        curso.setNombre(nombre);
        curso.setActivo(true);
        curso.setCapacidadSeccionDefault(35);
        return curso;
    }

    private void asignarId(Object entity, UUID id) {
        ReflectionTestUtils.setField(entity, "id", id);
    }
}
