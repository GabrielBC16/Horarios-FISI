package fisi.edu.pe.SPA.services;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.exception.ExcelGenerateException;
import fisi.edu.pe.SPA.domain.entity.Convalidacion;
import fisi.edu.pe.SPA.domain.entity.Curso;
import fisi.edu.pe.SPA.domain.entity.OfertaCurso;
import fisi.edu.pe.SPA.domain.entity.Planificacion;
import fisi.edu.pe.SPA.domain.entity.Seccion;
import fisi.edu.pe.SPA.domain.entity.SeccionHorario;
import fisi.edu.pe.SPA.domain.enums.EstadoSeccion;
import fisi.edu.pe.SPA.repository.ConvalidacionRespository;
import fisi.edu.pe.SPA.repository.OfertaCursoRepository;
import fisi.edu.pe.SPA.repository.PlanificacionRepository;
import fisi.edu.pe.SPA.repository.SeccionHorarioRepository;
import fisi.edu.pe.SPA.repository.SeccionRepository;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ExportarOfertaAcademicaService {
    private static final Short PLAN_2018 = (short) 2018;
    private static final Short PLAN_2023 = (short) 2023;
    private static final List<List<String>> PROFESORES_HEAD = List.of(
            List.of("CICLO PLAN 2018"),
            List.of("CÓDIGO PLAN 2018"),
            List.of("CURSO PLAN 2018"),
            List.of("CICLO PLAN 2023"),
            List.of("CÓDIGO PLAN 2023"),
            List.of("CURSO PLAN 2023"),
            List.of("SECCIÓN"),
            List.of("PROFESOR"),
            List.of("HORARIO"),
            List.of("PLAN 2018"),
            List.of("PLAN 2023"),
            List.of("TOTAL"));
    private static final List<List<String>> SEMANAL_HEAD = List.of(
            List.of("CICLO"),
            List.of("DÍA"),
            List.of("HORA INICIO"),
            List.of("HORA FIN"),
            List.of("CURSO"),
            List.of("SECCIÓN"),
            List.of("PROFESOR"),
            List.of("PLAN 2018"),
            List.of("PLAN 2023"),
            List.of("TOTAL"));
    private static final List<String> DIAS = List.of(
            "LUNES", "MARTES", "MIÉRCOLES", "JUEVES", "VIERNES", "SÁBADO", "DOMINGO");

    private final PlanificacionRepository planificacionRepository;
    private final OfertaCursoRepository ofertaCursoRepository;
    private final ConvalidacionRespository convalidacionRepository;
    private final SeccionRepository seccionRepository;
    private final SeccionHorarioRepository horarioRepository;

    public ExportarOfertaAcademicaService(
            PlanificacionRepository planificacionRepository,
            OfertaCursoRepository ofertaCursoRepository,
            ConvalidacionRespository convalidacionRepository,
            SeccionRepository seccionRepository,
            SeccionHorarioRepository horarioRepository) {
        this.planificacionRepository = planificacionRepository;
        this.ofertaCursoRepository = ofertaCursoRepository;
        this.convalidacionRepository = convalidacionRepository;
        this.seccionRepository = seccionRepository;
        this.horarioRepository = horarioRepository;
    }

    @Transactional(readOnly = true)
    public byte[] exportar(UUID planificacionId) {
        Planificacion planificacion = planificacionRepository.findById(planificacionId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Planificación no encontrada."));
        List<OfertaCurso> ofertas = ofertaCursoRepository.findByPlanificacion_Id(planificacion.getId());
        Map<UUID, Convalidacion> convalidacionesPorCurso2018 = new LinkedHashMap<>();
        for (Convalidacion convalidacion : convalidacionRepository.listarTodas()) {
            convalidacionesPorCurso2018.put(
                    convalidacion.getCursoConvalidado2018().getId(), convalidacion);
        }

        Map<UUID, GrupoCurso> grupos = crearGrupos(ofertas, convalidacionesPorCurso2018);
        cargarSecciones(ofertas, grupos, convalidacionesPorCurso2018);
        Map<UUID, List<SeccionHorario>> horarios = cargarHorarios(grupos.values());

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try {
            var writer = EasyExcel.write(output).build();
            writer.write(construirFilasProfesores(grupos.values(), horarios),
                    EasyExcel.writerSheet("Profesores").head(PROFESORES_HEAD).build());
            writer.write(construirFilasSemanal(grupos.values(), horarios),
                    EasyExcel.writerSheet("Por Semana").head(SEMANAL_HEAD).build());
            writer.finish();
            return output.toByteArray();
        } catch (ExcelGenerateException exception) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo generar el Excel de la oferta académica.",
                    exception);
        }
    }

    private Map<UUID, GrupoCurso> crearGrupos(
            List<OfertaCurso> ofertas,
            Map<UUID, Convalidacion> convalidacionesPorCurso2018) {
        Map<UUID, GrupoCurso> grupos = new LinkedHashMap<>();
        for (OfertaCurso oferta : ofertas) {
            Curso curso = oferta.getCurso();
            Short anioPlan = curso.getPlan().getAnio();
            UUID grupoId = curso.getId();
            Convalidacion convalidacion = null;
            if (PLAN_2018.equals(anioPlan)) {
                convalidacion = convalidacionesPorCurso2018.get(curso.getId());
                if (convalidacion != null) {
                    grupoId = convalidacion.getCurso2023().getId();
                }
            }

            GrupoCurso grupo = grupos.computeIfAbsent(grupoId, GrupoCurso::new);
            if (PLAN_2018.equals(anioPlan)) {
                grupo.agregarCurso2018(curso);
                if (convalidacion != null) {
                    grupo.setCurso2023(convalidacion.getCurso2023());
                }
            } else if (PLAN_2023.equals(anioPlan)) {
                grupo.setCurso2023(curso);
            }
        }
        for (Convalidacion convalidacion : convalidacionesPorCurso2018.values()) {
            GrupoCurso grupo = grupos.get(convalidacion.getCurso2023().getId());
            if (grupo != null) {
                grupo.agregarCurso2018(convalidacion.getCursoConvalidado2018());
                grupo.setCurso2023(convalidacion.getCurso2023());
            }
        }
        return grupos;
    }

    private void cargarSecciones(
            List<OfertaCurso> ofertas,
            Map<UUID, GrupoCurso> grupos,
            Map<UUID, Convalidacion> convalidacionesPorCurso2018) {
        if (ofertas.isEmpty()) {
            return;
        }
        List<UUID> ofertaIds = ofertas.stream().map(OfertaCurso::getId).toList();
        for (Seccion seccion : seccionRepository.findByOfertaCurso_IdIn(ofertaIds)) {
            if (seccion.getEstado() != EstadoSeccion.ACTIVA) {
                continue;
            }
            Curso curso = seccion.getOfertaCurso().getCurso();
            UUID grupoId = curso.getId();
            if (PLAN_2018.equals(curso.getPlan().getAnio())) {
                Convalidacion convalidacion = convalidacionesPorCurso2018.get(curso.getId());
                if (convalidacion != null) {
                    grupoId = convalidacion.getCurso2023().getId();
                }
            }
            GrupoCurso grupo = grupos.get(grupoId);
            if (grupo != null) {
                grupo.agregarSeccion(seccion);
            }
        }
    }

    private Map<UUID, List<SeccionHorario>> cargarHorarios(Iterable<GrupoCurso> grupos) {
        List<UUID> seccionIds = new ArrayList<>();
        grupos.forEach(grupo -> grupo.secciones().forEach(seccion -> seccionIds.add(seccion.getId())));
        if (seccionIds.isEmpty()) {
            return Map.of();
        }
        return horarioRepository.findBySeccion_IdInOrderByDiaAscHoraInicioAsc(seccionIds).stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        horario -> horario.getSeccion().getId(),
                        LinkedHashMap::new,
                        java.util.stream.Collectors.toList()));
    }

    private List<List<String>> construirFilasProfesores(
            Iterable<GrupoCurso> grupos,
            Map<UUID, List<SeccionHorario>> horarios) {
        List<GrupoCurso> ordenados = new ArrayList<>();
        grupos.forEach(ordenados::add);
        ordenados.sort(Comparator
                .comparing(GrupoCurso::ciclo)
                .thenComparing(GrupoCurso::codigoOrden));

        List<List<String>> filas = new ArrayList<>();
        for (GrupoCurso grupo : ordenados) {
            List<Seccion> secciones = grupo.secciones().stream()
                    .sorted(Comparator.comparing(
                            Seccion::getCodigo, Comparator.nullsLast(String::compareTo)))
                    .toList();
            filas.add(fila(
                    grupo.ciclos2018(), grupo.codigos2018(), grupo.nombres2018(),
                    grupo.ciclo2023(), grupo.codigo2023(), grupo.nombre2023(),
                    "", "", "", "", "", ""));
            filas.add(fila(
                    "", "", "", "", "", "",
                    "SECCIÓN", "PROFESOR", "HORARIO", "PLAN 2018", "PLAN 2023", "TOTAL"));
            if (secciones.isEmpty()) {
                filas.add(fila("", "", "", "", "", "", "", "", "", "", "", ""));
            } else {
                for (Seccion seccion : secciones) {
                    int cupos2018 = valorOZero(seccion.getCuposPlan2018());
                    int cupos2023 = valorOZero(seccion.getCuposPlan2023());
                    String horario = horarios.getOrDefault(seccion.getId(), List.of()).stream()
                            .map(this::formatearHorario)
                            .reduce((a, b) -> a + "; " + b)
                            .orElse("");
                    filas.add(fila(
                            "", "", "", "", "", "",
                            seccion.getCodigo(),
                            seccion.getProfesor() == null ? "" : seccion.getProfesor(),
                            horario,
                            Integer.toString(cupos2018),
                            Integer.toString(cupos2023),
                            Integer.toString(cupos2018 + cupos2023)));
                }
            }
        }
        return filas;
    }

    private List<List<String>> construirFilasSemanal(
            Iterable<GrupoCurso> grupos,
            Map<UUID, List<SeccionHorario>> horarios) {
        List<GrupoCurso> ordenados = new ArrayList<>();
        grupos.forEach(ordenados::add);
        ordenados.sort(Comparator
                .comparing(GrupoCurso::ciclo)
                .thenComparing(GrupoCurso::codigoOrden));

        List<List<String>> filas = new ArrayList<>();
        for (GrupoCurso grupo : ordenados) {
            for (Seccion seccion : grupo.secciones().stream()
                    .sorted(Comparator.comparing(
                            Seccion::getCodigo, Comparator.nullsLast(String::compareTo)))
                    .toList()) {
                int cupos2018 = valorOZero(seccion.getCuposPlan2018());
                int cupos2023 = valorOZero(seccion.getCuposPlan2023());
                List<SeccionHorario> bloques = horarios.getOrDefault(seccion.getId(), List.of());
                if (bloques.isEmpty()) {
                    filas.add(fila(
                            grupo.ciclo().toString(), "", "", "",
                            grupo.nombreCurso(), seccion.getCodigo(),
                            seccion.getProfesor() == null ? "" : seccion.getProfesor(),
                            Integer.toString(cupos2018), Integer.toString(cupos2023),
                            Integer.toString(cupos2018 + cupos2023)));
                    continue;
                }
                for (SeccionHorario bloque : bloques) {
                    filas.add(fila(
                            grupo.ciclo().toString(),
                            DIAS.get(bloque.getDia() - 1),
                            bloque.getHoraInicio().toString(),
                            bloque.getHoraFin().toString(),
                            grupo.nombreCurso(),
                            seccion.getCodigo(),
                            seccion.getProfesor() == null ? "" : seccion.getProfesor(),
                            Integer.toString(cupos2018),
                            Integer.toString(cupos2023),
                            Integer.toString(cupos2018 + cupos2023)));
                }
            }
        }
        return filas;
    }

    private String formatearHorario(SeccionHorario horario) {
        return DIAS.get(horario.getDia() - 1) + " "
                + horario.getHoraInicio() + "-" + horario.getHoraFin();
    }

    private List<String> fila(String... valores) {
        return new ArrayList<>(List.of(valores));
    }

    private int valorOZero(Integer valor) {
        return valor == null ? 0 : valor;
    }

    private static final class GrupoCurso {
        private final UUID id;
        private final Map<UUID, Curso> cursos2018 = new LinkedHashMap<>();
        private final Map<UUID, Seccion> secciones = new LinkedHashMap<>();
        private Curso curso2023;

        private GrupoCurso(UUID id) {
            this.id = id;
        }

        private void agregarCurso2018(Curso curso) {
            cursos2018.put(curso.getId(), curso);
        }

        private void agregarSeccion(Seccion seccion) {
            secciones.put(seccion.getId(), seccion);
        }

        private void setCurso2023(Curso curso) {
            curso2023 = curso;
        }

        private List<Seccion> secciones() {
            return List.copyOf(secciones.values());
        }

        private Short ciclo() {
            if (curso2023 != null && curso2023.getCiclo() != null) {
                return curso2023.getCiclo();
            }
            return cursos2018.values().stream()
                    .map(Curso::getCiclo)
                    .filter(ciclo -> ciclo != null)
                    .min(Short::compareTo)
                    .orElse(Short.MAX_VALUE);
        }

        private String codigoOrden() {
            if (curso2023 != null) {
                return curso2023.getCodigo();
            }
            return cursos2018.values().stream()
                    .map(Curso::getCodigo)
                    .min(String::compareTo)
                    .orElse(id.toString());
        }

        private String ciclos2018() {
            return cursos2018.values().stream()
                    .map(Curso::getCiclo)
                    .filter(ciclo -> ciclo != null)
                    .distinct()
                    .sorted()
                    .map(String::valueOf)
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("");
        }

        private String codigos2018() {
            return cursos2018.values().stream()
                    .map(Curso::getCodigo)
                    .sorted()
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("");
        }

        private String nombres2018() {
            return cursos2018.values().stream()
                    .map(Curso::getNombre)
                    .sorted()
                    .reduce((a, b) -> a + " / " + b)
                    .orElse("");
        }

        private String ciclo2023() {
            return curso2023 == null || curso2023.getCiclo() == null
                    ? ""
                    : curso2023.getCiclo().toString();
        }

        private String codigo2023() {
            return curso2023 == null ? "" : curso2023.getCodigo();
        }

        private String nombre2023() {
            return curso2023 == null ? "" : curso2023.getNombre();
        }

        private String nombreCurso() {
            if (curso2023 != null) {
                return curso2023.getNombre();
            }
            return nombres2018();
        }
    }
}
