package fisi.edu.pe.SPA.services;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.alibaba.excel.EasyExcel;
import fisi.edu.pe.SPA.domain.entity.Convalidacion;
import fisi.edu.pe.SPA.domain.entity.Curso;
import fisi.edu.pe.SPA.domain.entity.Escuela;
import fisi.edu.pe.SPA.domain.entity.OfertaCurso;
import fisi.edu.pe.SPA.domain.entity.PlanCurricular;
import fisi.edu.pe.SPA.domain.entity.Planificacion;
import fisi.edu.pe.SPA.domain.entity.Seccion;
import fisi.edu.pe.SPA.domain.entity.SeccionHorario;
import fisi.edu.pe.SPA.domain.enums.EstadoSeccion;
import fisi.edu.pe.SPA.repository.ConvalidacionRespository;
import fisi.edu.pe.SPA.repository.OfertaCursoRepository;
import fisi.edu.pe.SPA.repository.PlanificacionRepository;
import fisi.edu.pe.SPA.repository.SeccionHorarioRepository;
import fisi.edu.pe.SPA.repository.SeccionRepository;
import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.test.util.ReflectionTestUtils;

class ExportarOfertaAcademicaServiceTests {
    @Test
    void exportsConvalidatedCoursesWithTheirSectionsAndSchedules() throws Exception {
        PlanificacionRepository planificacionRepository = org.mockito.Mockito.mock(PlanificacionRepository.class);
        OfertaCursoRepository ofertaCursoRepository = org.mockito.Mockito.mock(OfertaCursoRepository.class);
        ConvalidacionRespository convalidacionRepository = org.mockito.Mockito.mock(ConvalidacionRespository.class);
        SeccionRepository seccionRepository = org.mockito.Mockito.mock(SeccionRepository.class);
        SeccionHorarioRepository horarioRepository = org.mockito.Mockito.mock(SeccionHorarioRepository.class);
        ExportarOfertaAcademicaService service = new ExportarOfertaAcademicaService(
                planificacionRepository,
                ofertaCursoRepository,
                convalidacionRepository,
                seccionRepository,
                horarioRepository);

        UUID planificacionId = UUID.randomUUID();
        Planificacion planificacion = new Planificacion();
        ReflectionTestUtils.setField(planificacion, "id", planificacionId);
        when(planificacionRepository.findById(planificacionId)).thenReturn(java.util.Optional.of(planificacion));

        Escuela escuela = new Escuela();
        Curso curso2018 = curso(escuela, (short) 2018, "MAT101", "Álgebra");
        Curso curso2023 = curso(escuela, (short) 2023, "MAT100", "Matemáticas");
        Curso cursoCiclo2 = curso(escuela, (short) 2023, "FIS200", "Física II");
        cursoCiclo2.setCiclo((short) 2);
        OfertaCurso oferta2018 = oferta(planificacion, curso2018);
        OfertaCurso oferta2023 = oferta(planificacion, curso2023);
        OfertaCurso ofertaCiclo2 = oferta(planificacion, cursoCiclo2);
        when(ofertaCursoRepository.findByPlanificacion_Id(planificacionId))
                .thenReturn(List.of(oferta2018, oferta2023, ofertaCiclo2));

        Convalidacion convalidacion = new Convalidacion();
        convalidacion.setCursoConvalidado2018(curso2018);
        convalidacion.setCurso2023(curso2023);
        when(convalidacionRepository.listarTodas()).thenReturn(List.of(convalidacion));

        Seccion seccion = new Seccion();
        asignarId(seccion);
        seccion.setOfertaCurso(oferta2023);
        seccion.setCodigo("S01");
        seccion.setCapacidad(35);
        seccion.setCuposPlan2018(15);
        seccion.setCuposPlan2023(20);
        seccion.setEstado(EstadoSeccion.ACTIVA);
        Seccion seccionCiclo2 = new Seccion();
        asignarId(seccionCiclo2);
        seccionCiclo2.setOfertaCurso(ofertaCiclo2);
        seccionCiclo2.setCodigo("S02");
        seccionCiclo2.setCapacidad(35);
        seccionCiclo2.setCuposPlan2018(0);
        seccionCiclo2.setCuposPlan2023(30);
        seccionCiclo2.setEstado(EstadoSeccion.ACTIVA);
        when(seccionRepository.findByOfertaCurso_IdIn(
                List.of(oferta2018.getId(), oferta2023.getId(), ofertaCiclo2.getId())))
                .thenReturn(List.of(seccion, seccionCiclo2));
        SeccionHorario horario = new SeccionHorario();
        asignarId(horario);
        horario.setSeccion(seccion);
        horario.setDia((short) 2);
        horario.setHoraInicio(java.time.LocalTime.of(9, 0));
        horario.setHoraFin(java.time.LocalTime.of(11, 0));
        when(horarioRepository.findBySeccion_IdInOrderByDiaAscHoraInicioAsc(
                org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of(horario));

        byte[] archivo = service.exportar(planificacionId);
        List<Map<Integer, String>> filas = EasyExcel.read(new ByteArrayInputStream(archivo))
                .headRowNumber(0)
                .sheet("Profesores")
                .doReadSync();
        String contenido = filas.toString();
        Map<Integer, String> filaSeccion = filas.stream()
                .filter(fila -> "S01".equals(fila.get(0)))
                .findFirst()
                .orElseThrow(() -> new AssertionError(filas.toString()));
        int filaCiclo = filas.stream()
                .filter(fila -> "CICLO 1".equals(fila.get(0)))
                .mapToInt(filas::indexOf)
                .findFirst()
                .orElseThrow();
        int filaCurso = filas.stream()
                .filter(fila -> "PLAN: 2018".equals(fila.get(0)))
                .mapToInt(filas::indexOf)
                .findFirst()
                .orElseThrow();
        int filaCiclo2 = filas.stream()
                .filter(fila -> "CICLO 2".equals(fila.get(0)))
                .mapToInt(filas::indexOf)
                .findFirst()
                .orElseThrow();

        assertTrue(archivo.length > 4);
        assertTrue(archivo[0] == 'P' && archivo[1] == 'K');
        assertTrue(filaCiclo < filaCurso, filas.toString());
        assertTrue(filaCiclo2 > filaCiclo, filas.toString());
        assertTrue(contenido.contains("MAT101"), contenido);
        assertTrue(contenido.contains("Álgebra"));
        assertTrue(contenido.contains("MAT100"));
        assertTrue(contenido.contains("Matemáticas"));
        assertTrue(filas.stream().anyMatch(fila -> "PLAN 2018".equals(fila.get(6))));
        assertTrue(contenido.contains("S01"));
        assertTrue(contenido.contains("15"));
        assertTrue(contenido.contains("20"));
        assertTrue(contenido.contains("DOCENTE"));
        assertTrue(filaSeccion.get(5) == null || filaSeccion.get(5).isBlank());
        assertTrue("MARTES".equals(filaSeccion.get(2)), filaSeccion.toString());
        assertTrue("09:00".equals(filaSeccion.get(3)), filaSeccion.toString());
        assertTrue("11:00".equals(filaSeccion.get(4)), filaSeccion.toString());
        assertTrue("15".equals(filaSeccion.get(6)));
        assertTrue("20".equals(filaSeccion.get(7)));

        List<Map<Integer, String>> filasSemanales = EasyExcel.read(new ByteArrayInputStream(archivo))
                .headRowNumber(0)
                .sheet("Por Semana")
                .doReadSync();
        int filaCicloSemanal = filasSemanales.stream()
                .filter(fila -> "CICLO 1".equals(fila.get(0)))
                .mapToInt(filasSemanales::indexOf)
                .findFirst()
                .orElseThrow();
        int filaCiclo2Semanal = filasSemanales.stream()
                .filter(fila -> "CICLO 2".equals(fila.get(0)))
                .mapToInt(filasSemanales::indexOf)
                .findFirst()
                .orElseThrow();
        int filaSeccionSemanal = filasSemanales.stream()
                .filter(fila -> "S01".equals(fila.get(5)))
                .mapToInt(filasSemanales::indexOf)
                .findFirst()
                .orElseThrow();
        assertTrue(filaCicloSemanal < filaSeccionSemanal, filasSemanales.toString());
        assertTrue(filaCiclo2Semanal > filaCicloSemanal, filasSemanales.toString());
        assertTrue(filasSemanales.toString().contains("MARTES"));
        assertTrue(filasSemanales.toString().contains("09:00"));

        try (var workbook = WorkbookFactory.create(new ByteArrayInputStream(archivo))) {
            var sheet = workbook.getSheet("Profesores");
            var filaEncabezadoCiclo = sheet.getRow(filaCiclo);
            assertTrue(filaEncabezadoCiclo.getCell(0).getCellStyle().getAlignment()
                    == HorizontalAlignment.CENTER);
            assertTrue(workbook.getFontAt(
                    filaEncabezadoCiclo.getCell(0).getCellStyle().getFontIndexAsInt())
                    .getFontHeightInPoints() > 11);
            assertTrue(sheet.getRow(filaCiclo + 1).getCell(0).getStringCellValue().isBlank());
            assertTrue(sheet.getRow(filaCiclo + 2).getCell(0).getStringCellValue().isBlank());
        }
    }

    private OfertaCurso oferta(Planificacion planificacion, Curso curso) {
        OfertaCurso oferta = new OfertaCurso();
        asignarId(oferta);
        oferta.setPlanificacion(planificacion);
        oferta.setCurso(curso);
        return oferta;
    }

    private Curso curso(Escuela escuela, Short anioPlan, String codigo, String nombre) {
        PlanCurricular plan = new PlanCurricular();
        plan.setAreaEscuela(escuela);
        plan.setAnio(anioPlan);
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
