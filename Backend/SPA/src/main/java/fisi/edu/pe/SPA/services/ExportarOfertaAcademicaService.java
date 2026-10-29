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
import com.alibaba.excel.write.handler.context.CellWriteHandlerContext;
import com.alibaba.excel.write.handler.CellWriteHandler;
import com.alibaba.excel.write.handler.SheetWriteHandler;
import com.alibaba.excel.write.metadata.holder.WriteSheetHolder;
import com.alibaba.excel.write.metadata.holder.WriteWorkbookHolder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Set;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;

@Service
public class ExportarOfertaAcademicaService {
    private static final Short PLAN_2018 = (short) 2018;
    private static final Short PLAN_2023 = (short) 2023;
    private static final List<String> DIAS = List.of(
            "LUNES", "MARTES", "MIÉRCOLES", "JUEVES", "VIERNES", "SÁBADO", "DOMINGO");

    private final PlanificacionRepository planificacionRepository;
    private final OfertaCursoRepository ofertaCursoRepository;
    private final ConvalidacionRespository convalidacionRepository;
    private final SeccionRepository seccionRepository;
    private final SeccionHorarioRepository horarioRepository;

    private static final int COLS_PROFESORES = 9;
    private static final int[] ANCHOS_PROFESORES = { 10, 7, 14, 12, 12, 40, 12, 12, 12 };
    private static final int[] ANCHOS_SEMANAL = { 8, 14, 12, 12, 50, 12, 38, 12, 12, 12 };

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
            Hoja profesores = construirHojaProfesores(grupos.values(), horarios);
            writer.write(profesores.datos,
                    EasyExcel.writerSheet("Profesores")
                            .needHead(false)
                            .useDefaultStyle(false)
                            .registerWriteHandler(new FormatoHoja(profesores))
                            .build());
            Hoja semanal = construirHojaSemanal(grupos.values(), horarios);
            writer.write(semanal.datos,
                    EasyExcel.writerSheet("Por Semana")
                            .needHead(false)
                            .useDefaultStyle(false)
                            .registerWriteHandler(new FormatoHoja(semanal))
                            .build());
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

    private Hoja construirHojaProfesores(
            Iterable<GrupoCurso> grupos,
            Map<UUID, List<SeccionHorario>> horarios) {
        Hoja hoja = new Hoja(ANCHOS_PROFESORES, Set.of(5));
        for (GrupoCurso grupo : ordenar(grupos)) {
            if (!grupo.cursos2018.isEmpty()) {
                encabezadoCurso(hoja, "2018", grupo.codigos2018(), grupo.nombres2018());
            }
            if (grupo.curso2023 != null) {
                encabezadoCurso(hoja, "2023", grupo.codigo2023(), grupo.nombre2023());
            }
            hoja.fila(TipoFila.TABLA_HEADER,
                    "GRUPO", "TIPO", "DÍA", "HORA INICIO", "HORA FIN", "DOCENTE",
                    "PLAN 2018", "PLAN 2023", "TOTAL");

            List<Seccion> secciones = seccionesOrdenadas(grupo);
            if (secciones.isEmpty()) {
                hoja.fila(TipoFila.DETALLE, "", "", "", "", "", "", "", "", "");
            }
            int totalCurso = 0;
            for (Seccion seccion : secciones) {
                int cupos2018 = valorOZero(seccion.getCuposPlan2018());
                int cupos2023 = valorOZero(seccion.getCuposPlan2023());
                totalCurso += cupos2018 + cupos2023;
                List<SeccionHorario> bloques = horarios.getOrDefault(seccion.getId(), List.of());
                int n = Math.max(1, bloques.size());
                int primera = hoja.siguiente();
                for (int i = 0; i < n; i++) {
                    SeccionHorario b = bloques.isEmpty() ? null : bloques.get(i);
                    boolean first = i == 0;
                    hoja.fila(TipoFila.DETALLE,
                            first ? seccion.getCodigo() : "",
                            "",
                            b == null ? "" : DIAS.get(b.getDia() - 1),
                            b == null ? "" : b.getHoraInicio().toString(),
                            b == null ? "" : b.getHoraFin().toString(),
                            first && seccion.getProfesor() != null ? seccion.getProfesor() : "",
                            first ? Integer.toString(cupos2018) : "",
                            first ? Integer.toString(cupos2023) : "",
                            first ? Integer.toString(cupos2018 + cupos2023) : "");
                }
                for (int col : new int[] { 0, 5, 6, 7, 8 }) {
                    hoja.merge(primera, primera + n - 1, col, col);
                }
            }
            if (!secciones.isEmpty()) {
                hoja.fila(TipoFila.TOTAL, "", "", "", "", "", "", "", "TOTAL CURSO",
                        Integer.toString(totalCurso));
            }
            hoja.fila(TipoFila.SEPARADOR, "", "", "", "", "", "", "", "", "");
        }
        return hoja;
    }

    private void encabezadoCurso(Hoja hoja, String plan, String codigo, String nombre) {
        int r = hoja.siguiente();
        hoja.fila(TipoFila.CURSO_HEADER,
                "PLAN: " + plan, "", "CÓDIGO: " + codigo, "", "", "CURSO: " + nombre,
                "CRÉDITOS: ", "", "COD PREREQ: ");
        hoja.merge(r, r, 0, 1);
        hoja.merge(r, r, 2, 4);
        hoja.merge(r, r, 6, 7);
    }

    private Hoja construirHojaSemanal(
            Iterable<GrupoCurso> grupos,
            Map<UUID, List<SeccionHorario>> horarios) {
        Hoja hoja = new Hoja(ANCHOS_SEMANAL, Set.of(4, 6));
        hoja.fila(TipoFila.TABLA_HEADER,
                "CICLO", "DÍA", "HORA INICIO", "HORA FIN", "CURSO", "SECCIÓN", "PROFESOR",
                "PLAN 2018", "PLAN 2023", "TOTAL");
        for (GrupoCurso grupo : ordenar(grupos)) {
            for (Seccion seccion : seccionesOrdenadas(grupo)) {
                int cupos2018 = valorOZero(seccion.getCuposPlan2018());
                int cupos2023 = valorOZero(seccion.getCuposPlan2023());
                String profesor = seccion.getProfesor() == null ? "" : seccion.getProfesor();
                List<SeccionHorario> bloques = horarios.getOrDefault(seccion.getId(), List.of());
                if (bloques.isEmpty()) {
                    hoja.fila(TipoFila.DETALLE,
                            grupo.ciclo().toString(), "", "", "",
                            grupo.nombreCurso(), seccion.getCodigo(), profesor,
                            Integer.toString(cupos2018), Integer.toString(cupos2023),
                            Integer.toString(cupos2018 + cupos2023));
                    continue;
                }
                for (SeccionHorario b : bloques) {
                    hoja.fila(TipoFila.DETALLE,
                            grupo.ciclo().toString(),
                            DIAS.get(b.getDia() - 1),
                            b.getHoraInicio().toString(),
                            b.getHoraFin().toString(),
                            grupo.nombreCurso(), seccion.getCodigo(), profesor,
                            Integer.toString(cupos2018), Integer.toString(cupos2023),
                            Integer.toString(cupos2018 + cupos2023));
                }
            }
        }
        return hoja;
    }

    private List<GrupoCurso> ordenar(Iterable<GrupoCurso> grupos) {
        List<GrupoCurso> ordenados = new ArrayList<>();
        grupos.forEach(ordenados::add);
        ordenados.sort(Comparator.comparing(GrupoCurso::ciclo).thenComparing(GrupoCurso::codigoOrden));
        return ordenados;
    }

    private List<Seccion> seccionesOrdenadas(GrupoCurso grupo) {
        return grupo.secciones().stream()
                .sorted(Comparator.comparing(Seccion::getCodigo, Comparator.nullsLast(String::compareTo)))
                .toList();
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

// Clases auxiliares
final class Hoja {
    final List<List<String>> datos = new ArrayList<>();
    final List<TipoFila> tipos = new ArrayList<>();
    final List<CellRangeAddress> merges = new ArrayList<>();
    final int[] anchos;
    final Set<Integer> columnasIzquierda;

    Hoja(int[] anchos, Set<Integer> columnasIzquierda) {
        this.anchos = anchos;
        this.columnasIzquierda = columnasIzquierda;
    }

    void fila(TipoFila tipo, String... valores) {
        datos.add(new ArrayList<>(Arrays.asList(valores)));
        tipos.add(tipo);
    }

    int siguiente() {
        return datos.size();
    }

    void merge(int r1, int r2, int c1, int c2) {
        if (r1 != r2 || c1 != c2) {
            merges.add(new CellRangeAddress(r1, r2, c1, c2));
        }
    }
}

/** Aplica anchos, combinaciones y estilos según el tipo de fila. */
final class FormatoHoja implements SheetWriteHandler, CellWriteHandler {
    private final Hoja hoja;
    private final Map<String, CellStyle> cache = new HashMap<>();

    FormatoHoja(Hoja hoja) {
        this.hoja = hoja;
    }

    @Override
    public void afterSheetCreate(WriteWorkbookHolder workbookHolder, WriteSheetHolder sheetHolder) {
        Sheet sheet = sheetHolder.getSheet();
        for (int i = 0; i < hoja.anchos.length; i++) {
            sheet.setColumnWidth(i, hoja.anchos[i] * 256);
        }
        hoja.merges.forEach(sheet::addMergedRegion);
    }

    @Override
    public void afterCellDispose(CellWriteHandlerContext context) {
        Cell cell = context.getCell();
        if (cell == null || cell.getRowIndex() >= hoja.tipos.size()) {
            return;
        }
        TipoFila tipo = hoja.tipos.get(cell.getRowIndex());
        int col = cell.getColumnIndex();
        if (tipo == TipoFila.SEPARADOR || (tipo == TipoFila.TOTAL && col < 7)) {
            return;
        }
        boolean izquierda = tipo == TipoFila.CURSO_HEADER || hoja.columnasIzquierda.contains(col);
        cell.setCellStyle(estilo(cell.getSheet().getWorkbook(), tipo, izquierda));
    }

    private CellStyle estilo(Workbook wb, TipoFila tipo, boolean izquierda) {
        return cache.computeIfAbsent(tipo + ":" + izquierda, k -> {
            CellStyle s = wb.createCellStyle();
            s.setBorderTop(BorderStyle.THIN);
            s.setBorderBottom(BorderStyle.THIN);
            s.setBorderLeft(BorderStyle.THIN);
            s.setBorderRight(BorderStyle.THIN);
            s.setVerticalAlignment(VerticalAlignment.CENTER);
            s.setAlignment(izquierda ? HorizontalAlignment.LEFT : HorizontalAlignment.CENTER);
            s.setWrapText(true);
            if (tipo != TipoFila.DETALLE) {
                Font bold = wb.createFont();
                bold.setBold(true);
                s.setFont(bold);
            }
            if (tipo == TipoFila.CURSO_HEADER) {
                s.setFillForegroundColor(IndexedColors.PALE_BLUE.getIndex());
                s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            } else if (tipo == TipoFila.TABLA_HEADER) {
                s.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
                s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            } else if (tipo == TipoFila.TOTAL) {
                s.setAlignment(HorizontalAlignment.CENTER);
            }
            return s;
        });
    }
}