package fisi.edu.pe.SPA.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import fisi.edu.pe.SPA.domain.entity.Convalidacion;
import fisi.edu.pe.SPA.domain.entity.Curso;
import fisi.edu.pe.SPA.domain.entity.Escuela;
import fisi.edu.pe.SPA.domain.entity.OfertaCurso;
import fisi.edu.pe.SPA.domain.entity.PlanCurricular;
import fisi.edu.pe.SPA.domain.entity.Planificacion;
import fisi.edu.pe.SPA.domain.entity.Seccion;
import fisi.edu.pe.SPA.repository.SeccionHorarioRepository;
import fisi.edu.pe.SPA.domain.enums.EstadoSeccion;
import fisi.edu.pe.SPA.repository.ConvalidacionRespository;
import fisi.edu.pe.SPA.repository.OfertaCursoRepository;
import fisi.edu.pe.SPA.repository.SeccionRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class ValidarOfertaAcademicaServiceTests {
    @Test
    void includesSharedSectionAndPlanQuotasForBothConvalidatedOffers() {
        OfertaCursoRepository ofertaCursoRepository = org.mockito.Mockito.mock(OfertaCursoRepository.class);
        ConvalidacionRespository convalidacionRepository = org.mockito.Mockito.mock(ConvalidacionRespository.class);
        SeccionRepository seccionRepository = org.mockito.Mockito.mock(SeccionRepository.class);
        SeccionHorarioRepository horarioRepository = org.mockito.Mockito.mock(SeccionHorarioRepository.class);
        ValidarOfertaAcademicaService service = new ValidarOfertaAcademicaService(
                ofertaCursoRepository, convalidacionRepository, seccionRepository, horarioRepository);

        UUID planificacionId = UUID.randomUUID();
        Escuela escuela = new Escuela();
        Curso curso2018 = curso(escuela, (short) 2018, "MAT101", "Álgebra");
        Curso curso2023 = curso(escuela, (short) 2023, "MAT100", "Matemáticas");
        Planificacion planificacion = new Planificacion();
        asignarId(planificacion);
        OfertaCurso oferta2018 = oferta(planificacion, curso2018);
        OfertaCurso oferta2023 = oferta(planificacion, curso2023);

        Convalidacion convalidacion = new Convalidacion();
        convalidacion.setCursoConvalidado2018(curso2018);
        convalidacion.setCurso2023(curso2023);

        Seccion seccionCompartida = new Seccion();
        asignarId(seccionCompartida);
        seccionCompartida.setOfertaCurso(oferta2023);
        seccionCompartida.setCodigo("S01");
        seccionCompartida.setCapacidad(35);
        seccionCompartida.setCuposPlan2018(15);
        seccionCompartida.setCuposPlan2023(20);
        seccionCompartida.setEstado(EstadoSeccion.ACTIVA);

        when(ofertaCursoRepository.findByPlanificacion_Id(planificacionId))
                .thenReturn(List.of(oferta2018, oferta2023));
        when(convalidacionRepository.findAll()).thenReturn(List.of(convalidacion));
        when(seccionRepository.findByOfertaCurso_IdIn(
                List.of(oferta2018.getId(), oferta2023.getId())))
                .thenReturn(List.of(seccionCompartida));

        var resultado = service.getOfertaAcademicaByPlanificacionId(planificacionId);

        assertEquals(2, resultado.size());
        assertEquals(List.of("S01"), resultado.get(0).secciones().stream()
                .map(seccion -> seccion.codigo()).toList());
        assertEquals(List.of("S01"), resultado.get(1).secciones().stream()
                .map(seccion -> seccion.codigo()).toList());
        assertEquals(15, resultado.get(0).secciones().get(0).cuposPlan2018());
        assertEquals(20, resultado.get(1).secciones().get(0).cuposPlan2023());
        assertEquals(1, resultado.get(0).seccionesCalculadas());
        assertEquals(1, resultado.get(1).seccionesCalculadas());
    }

    private OfertaCurso oferta(Planificacion planificacion, Curso curso) {
        OfertaCurso oferta = new OfertaCurso();
        asignarId(oferta);
        oferta.setPlanificacion(planificacion);
        oferta.setCurso(curso);
        oferta.setEstudiantesEstimados(15);
        oferta.setCapacidadAplicada(35);
        oferta.setSeccionesPropuestas(1);
        return oferta;
    }

    private Curso curso(Escuela escuela, Short anioPlan, String codigo, String nombre) {
        PlanCurricular plan = new PlanCurricular();
        plan.setAreaEscuela(escuela);
        plan.setAnio(anioPlan);
        plan.setActivo(true);
        Curso curso = new Curso();
        asignarId(curso);
        curso.setPlan(plan);
        curso.setCodigo(codigo);
        curso.setNombre(nombre);
        curso.setCiclo((short) 1);
        return curso;
    }

    private void asignarId(Object entity) {
        ReflectionTestUtils.setField(entity, "id", UUID.randomUUID());
    }
}
