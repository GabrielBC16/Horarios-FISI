package fisi.edu.pe.SPA.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fisi.edu.pe.SPA.domain.entity.Convalidacion;
import fisi.edu.pe.SPA.domain.entity.Curso;
import fisi.edu.pe.SPA.domain.entity.Escuela;
import fisi.edu.pe.SPA.domain.entity.PlanCurricular;
import fisi.edu.pe.SPA.dto.convalidacion.ConvalidacionRequestDto;
import fisi.edu.pe.SPA.dto.convalidacion.CursoReferenciaDto;
import fisi.edu.pe.SPA.repository.ConvalidacionRespository;
import fisi.edu.pe.SPA.repository.CursoRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

class ConvalidacionServiceTests {
    @Test
    void acceptsTwoPlan2018CoursesMappedToTheSamePlan2023Course() {
        ConvalidacionRespository convalidacionRepository = org.mockito.Mockito.mock(ConvalidacionRespository.class);
        CursoRepository cursoRepository = org.mockito.Mockito.mock(CursoRepository.class);
        ConvalidacionService service = new ConvalidacionService(convalidacionRepository, cursoRepository);

        Escuela escuela = new Escuela();
        asignarId(escuela);
        Curso algebra2018 = curso(escuela, (short) 2018, "MAT101", "Álgebra");
        Curso calculo2018 = curso(escuela, (short) 2018, "MAT102", "Cálculo");
        Curso matematicas2023 = curso(escuela, (short) 2023, "MAT100", "Matemáticas");

        when(cursoRepository.buscarActivosPorAnioYCodigo((short) 2018, "MAT101", null, null))
                .thenReturn(List.of(algebra2018));
        when(cursoRepository.buscarActivosPorAnioYCodigo((short) 2018, "MAT102", null, null))
                .thenReturn(List.of(calculo2018));
        when(cursoRepository.buscarActivosPorAnioYCodigo((short) 2023, "MAT100", null, null))
                .thenReturn(List.of(matematicas2023));
        doAnswer(invocation -> invocation.getArgument(0))
                .when(convalidacionRepository).save(any(Convalidacion.class));

        service.crear(request("MAT101", "MAT100"));
        service.crear(request("MAT102", "MAT100"));

        verify(convalidacionRepository, times(2)).save(any(Convalidacion.class));
        verify(convalidacionRepository).existsByCursoConvalidado2018_Id(algebra2018.getId());
        verify(convalidacionRepository).existsByCursoConvalidado2018_Id(calculo2018.getId());
    }

    @Test
    void resolvesByNameAndRejectsAmbiguousNames() {
        ConvalidacionRespository convalidacionRepository = org.mockito.Mockito.mock(ConvalidacionRespository.class);
        CursoRepository cursoRepository = org.mockito.Mockito.mock(CursoRepository.class);
        ConvalidacionService service = new ConvalidacionService(convalidacionRepository, cursoRepository);
        Escuela escuela = new Escuela();
        asignarId(escuela);
        Curso cursoA = curso(escuela, (short) 2018, "CUR001", "Curso común");
        Curso cursoB = curso(escuela, (short) 2018, "CUR002", "Curso común");

        when(cursoRepository.buscarActivosPorAnioYNombre((short) 2018, "Curso común", null, null))
                .thenReturn(List.of(cursoA, cursoB));

        ResponseStatusException exception = org.junit.jupiter.api.Assertions.assertThrows(
                ResponseStatusException.class,
                () -> service.crear(new ConvalidacionRequestDto(
                        null,
                        new CursoReferenciaDto(null, null, "Curso común"),
                        new CursoReferenciaDto(null, "MAT100", null))));

        assertEquals(409, exception.getStatusCode().value());
    }

    @Test
    void updatesAnExistingConvalidation() {
        ConvalidacionRespository convalidacionRepository = org.mockito.Mockito.mock(ConvalidacionRespository.class);
        CursoRepository cursoRepository = org.mockito.Mockito.mock(CursoRepository.class);
        ConvalidacionService service = new ConvalidacionService(convalidacionRepository, cursoRepository);
        Escuela escuela = new Escuela();
        asignarId(escuela);
        Curso curso2018 = curso(escuela, (short) 2018, "MAT104", "Geometría");
        Curso curso2023 = curso(escuela, (short) 2023, "MAT200", "Geometría y Álgebra");
        Convalidacion existente = new Convalidacion();
        UUID convalidacionId = UUID.randomUUID();
        ReflectionTestUtils.setField(existente, "id", convalidacionId);

        when(convalidacionRepository.findById(convalidacionId)).thenReturn(Optional.of(existente));
        when(cursoRepository.buscarActivosPorAnioYCodigo((short) 2018, "MAT104", null, null))
                .thenReturn(List.of(curso2018));
        when(cursoRepository.buscarActivosPorAnioYCodigo((short) 2023, "MAT200", null, null))
                .thenReturn(List.of(curso2023));
        doAnswer(invocation -> invocation.getArgument(0))
                .when(convalidacionRepository).save(any(Convalidacion.class));

        var actualizado = service.actualizar(convalidacionId, request("MAT104", "MAT200"));

        assertEquals("MAT104", actualizado.curso2018().codigo());
        assertEquals("MAT200", actualizado.curso2023().codigo());
        verify(convalidacionRepository).existsByCursoConvalidado2018_IdAndIdNot(
                curso2018.getId(), convalidacionId);
        verify(convalidacionRepository).save(existente);
    }

    @Test
    void listsConvalidationsWithCourseDetailsAndFiltersByPlan2018Cycle() {
        ConvalidacionRespository convalidacionRepository = org.mockito.Mockito.mock(ConvalidacionRespository.class);
        CursoRepository cursoRepository = org.mockito.Mockito.mock(CursoRepository.class);
        ConvalidacionService service = new ConvalidacionService(convalidacionRepository, cursoRepository);
        Escuela escuela = new Escuela();
        asignarId(escuela);
        Curso curso2018 = curso(escuela, (short) 2018, "MAT101", "Álgebra");
        curso2018.setCiclo((short) 2);
        Curso curso2023 = curso(escuela, (short) 2023, "MAT100", "Matemáticas");
        curso2023.setCiclo((short) 1);
        Convalidacion convalidacion = new Convalidacion();
        convalidacion.setCursoConvalidado2018(curso2018);
        convalidacion.setCurso2023(curso2023);
        when(convalidacionRepository.listarTodas()).thenReturn(List.of(convalidacion));
        when(convalidacionRepository.listarPorCiclo2018((short) 2)).thenReturn(List.of(convalidacion));

        var todas = service.listarTodas();
        var porCiclo = service.listarPorCiclo2018((short) 2);

        assertEquals(1, todas.size());
        assertEquals("MAT101", todas.get(0).curso2018().codigo());
        assertEquals("Álgebra", todas.get(0).curso2018().nombre());
        assertEquals((short) 2, todas.get(0).curso2018().ciclo());
        assertEquals((short) 1, todas.get(0).curso2023().ciclo());
        assertEquals(1, porCiclo.size());
        assertEquals((short) 2, porCiclo.get(0).curso2018().ciclo());
        verify(convalidacionRepository).listarPorCiclo2018((short) 2);
    }

    private ConvalidacionRequestDto request(String codigo2018, String codigo2023) {
        return new ConvalidacionRequestDto(
                null,
                new CursoReferenciaDto(null, codigo2018, null),
                new CursoReferenciaDto(null, codigo2023, null));
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
        curso.setActivo(true);
        return curso;
    }

    private void asignarId(Object entity) {
        ReflectionTestUtils.setField(entity, "id", UUID.randomUUID());
    }
}
