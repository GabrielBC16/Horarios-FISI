package fisi.edu.pe.SPA.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import fisi.edu.pe.SPA.domain.entity.Convalidacion;
import fisi.edu.pe.SPA.domain.entity.Curso;
import fisi.edu.pe.SPA.domain.entity.Importacion;
import fisi.edu.pe.SPA.domain.entity.OfertaCurso;
import fisi.edu.pe.SPA.domain.entity.Seccion;
import fisi.edu.pe.SPA.domain.enums.EstadoImportacion;
import fisi.edu.pe.SPA.domain.enums.EstadoSeccion;
import fisi.edu.pe.SPA.domain.enums.SituacionOferta;
import fisi.edu.pe.SPA.dto.importacion.AplicacionImportacionDto;
import fisi.edu.pe.SPA.dto.importacion.FilaImportacionDto;
import fisi.edu.pe.SPA.repository.ConvalidacionRespository;
import fisi.edu.pe.SPA.repository.CursoRepository;
import fisi.edu.pe.SPA.repository.ImportacionRepository;
import fisi.edu.pe.SPA.repository.OfertaCursoRepository;
import fisi.edu.pe.SPA.repository.SeccionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class CalcularOfertaAcademicaService {
    private static final Short PLAN_2018 = (short) 2018;
    private static final Short PLAN_2023 = (short) 2023;

    @Value("${app.oferta.capacidad-seccion-default:35}")
    private int capacidadSeccionDefault = 35;
    private final ObjectMapper objectMapper;
    private final ImportacionRepository importacionRepository;
    private final CursoRepository cursoRepository;
    private final ConvalidacionRespository convalidacionRepository;
    private final OfertaCursoRepository ofertaCursoRepository;
    private final SeccionRepository seccionRepository;
    private final AuditoriaPlanificacionService auditoriaService;

    public CalcularOfertaAcademicaService(
            ObjectMapper objectMapper,
            ImportacionRepository importacionRepository,
            CursoRepository cursoRepository,
            ConvalidacionRespository convalidacionRepository,
            OfertaCursoRepository ofertaCursoRepository,
            SeccionRepository seccionRepository,
            AuditoriaPlanificacionService auditoriaService) {
        this.objectMapper = objectMapper;
        this.importacionRepository = importacionRepository;
        this.cursoRepository = cursoRepository;
        this.convalidacionRepository = convalidacionRepository;
        this.ofertaCursoRepository = ofertaCursoRepository;
        this.seccionRepository = seccionRepository;
        this.auditoriaService = auditoriaService;
    }

    @Transactional
    public AplicacionImportacionDto procesarImportacion(UUID importacionId) {
        if (capacidadSeccionDefault <= 0) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "app.oferta.capacidad-seccion-default debe ser mayor que cero.");
        }
        Importacion importacion = importacionRepository.findById(importacionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Importación no encontrada."));
        if (importacion.getEstado() != EstadoImportacion.VALIDADA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Solo se puede confirmar una importación validada.");
        }
        auditoriaService.establecerContexto(
                importacion.getImportadoPor(), "Aplicación de importación de demanda académica");

        Map<UUID, Integer> demandas = obtenerDemandas(importacion);
        Map<UUID, OfertaDemanda> ofertasActualizadas = new LinkedHashMap<>();
        for (Map.Entry<UUID, Integer> demanda : demandas.entrySet()) {
            Curso curso = cursoRepository.findById(demanda.getKey()).orElseThrow(
                    () -> new ResponseStatusException(HttpStatus.CONFLICT,
                            "El curso ya no existe: " + demanda.getKey()));
            Short anioPlan = curso.getPlan().getAnio();
            if (!PLAN_2018.equals(anioPlan) && !PLAN_2023.equals(anioPlan)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "El cálculo de cupos compartidos solo admite cursos de planes 2018 y 2023.");
            }
            OfertaCurso oferta = ofertaCursoRepository
                    .findByPlanificacion_IdAndCurso_Id(importacion.getPlanificacion().getId(), curso.getId())
                    .orElseGet(OfertaCurso::new);
            oferta.setPlanificacion(importacion.getPlanificacion());
            oferta.setCurso(curso);
            oferta.setImportacion(importacion);
            oferta.setEstudiantesEstimados(demanda.getValue());
            oferta.setCapacidadAplicada(capacidadSeccionDefault);
            oferta.setSituacion(SituacionOferta.OFERTAR);
            oferta = ofertaCursoRepository.save(oferta);
            ofertasActualizadas.put(curso.getId(), new OfertaDemanda(curso, oferta, demanda.getValue()));
        }

        Map<UUID, UUID> curso2018A2023 = obtenerEquivalencias();
        Map<UUID, List<OfertaDemanda>> demandasPorGrupo = new LinkedHashMap<>();
        for (OfertaDemanda oferta : ofertasActualizadas.values()) {
            UUID grupoId = grupoDeCurso(oferta.curso(), curso2018A2023);
            demandasPorGrupo.computeIfAbsent(grupoId, ignored -> new ArrayList<>()).add(oferta);
        }

        Map<UUID, List<OfertaCurso>> ofertasPorGrupo = new LinkedHashMap<>();
        for (OfertaCurso oferta : ofertaCursoRepository
                .findByPlanificacion_Id(importacion.getPlanificacion().getId())) {
            UUID grupoId = grupoDeCurso(oferta.getCurso(), curso2018A2023);
            ofertasPorGrupo.computeIfAbsent(grupoId, ignored -> new ArrayList<>()).add(oferta);
        }

        int seccionesPropuestas = 0;
        for (Map.Entry<UUID, List<OfertaDemanda>> grupo : demandasPorGrupo.entrySet()) {
            List<OfertaDemanda> ofertasDelGrupo = grupo.getValue();
            List<OfertaCurso> ofertasRelacionadas = ofertasPorGrupo.getOrDefault(
                    grupo.getKey(), ofertasDelGrupo.stream().map(OfertaDemanda::oferta).toList());
            OfertaCurso ofertaAncla = elegirOfertaAncla(ofertasDelGrupo);
            int estudiantes2018 = sumarDemandaPorPlan(ofertasDelGrupo, PLAN_2018);
            int estudiantes2023 = sumarDemandaPorPlan(ofertasDelGrupo, PLAN_2023);
            int estudiantesTotales = sumarDemandas(estudiantes2018, estudiantes2023);
            List<Seccion> existentes = seccionesExistentes(ofertasRelacionadas);
            int cantidadPropuesta = sincronizarSecciones(
                    ofertaAncla, existentes, estudiantes2018, estudiantes2023, estudiantesTotales);

            for (OfertaDemanda oferta : ofertasDelGrupo) {
                oferta.oferta().setSeccionesPropuestas(cantidadPropuesta);
                ofertaCursoRepository.save(oferta.oferta());
            }
            seccionesPropuestas += cantidadPropuesta;
        }

        importacion.setEstado(EstadoImportacion.APLICADA);
        return new AplicacionImportacionDto(importacion.getId(), ofertasActualizadas.size(), seccionesPropuestas);
    }

    private Map<UUID, Integer> obtenerDemandas(Importacion importacion) {
        Map<UUID, Integer> demandas = new LinkedHashMap<>();
        for (FilaImportacionDto fila : obtenerFilas(importacion)) {
            if (!fila.valida()) {
                continue;
            }
            if (fila.cursoId() == null) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "La fila válida no tiene un curso asociado: " + fila.codigo());
            }
            if (fila.estudiantesEstimados() == null || fila.estudiantesEstimados() <= 0) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "La demanda debe ser mayor que cero para el curso: " + fila.codigo());
            }
            try {
                demandas.merge(fila.cursoId(), fila.estudiantesEstimados(), Math::addExact);
            } catch (ArithmeticException exception) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "La demanda total excede el límite permitido para el curso: " + fila.codigo());
            }
        }
        return demandas;
    }

    private List<FilaImportacionDto> obtenerFilas(Importacion importacion) {
        Object filas = importacion.getDetalleValidacion() == null ? null
                : importacion.getDetalleValidacion().get("filas");
        if (filas == null) {
            return List.of();
        }
        return objectMapper.convertValue(filas, new TypeReference<List<FilaImportacionDto>>() {
        });
    }

    private Map<UUID, UUID> obtenerEquivalencias() {
        Map<UUID, UUID> curso2018A2023 = new LinkedHashMap<>();
        for (Convalidacion convalidacion : convalidacionRepository.findAll()) {
            curso2018A2023.put(
                    convalidacion.getCursoConvalidado2018().getId(),
                    convalidacion.getCurso2023().getId());
        }
        return curso2018A2023;
    }

    private UUID grupoDeCurso(Curso curso, Map<UUID, UUID> curso2018A2023) {
        if (PLAN_2018.equals(curso.getPlan().getAnio())) {
            return curso2018A2023.getOrDefault(curso.getId(), curso.getId());
        }
        return curso.getId();
    }

    private OfertaCurso elegirOfertaAncla(List<OfertaDemanda> ofertas) {
        return ofertas.stream()
                .filter(oferta -> PLAN_2023.equals(oferta.curso().getPlan().getAnio()))
                .map(OfertaDemanda::oferta)
                .findFirst()
                .orElseGet(() -> ofertas.get(0).oferta());
    }

    private int sumarDemandaPorPlan(List<OfertaDemanda> ofertas, Short anioPlan) {
        long total = ofertas.stream()
                .filter(oferta -> anioPlan.equals(oferta.curso().getPlan().getAnio()))
                .mapToLong(OfertaDemanda::estudiantes)
                .sum();
        if (total > Integer.MAX_VALUE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "La demanda total del grupo supera el límite permitido.");
        }
        return (int) total;
    }

    private int sumarDemandas(int estudiantes2018, int estudiantes2023) {
        try {
            return Math.addExact(estudiantes2018, estudiantes2023);
        } catch (ArithmeticException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "La demanda total del grupo supera el límite permitido.");
        }
    }

    private List<Seccion> seccionesExistentes(List<OfertaCurso> ofertas) {
        List<UUID> ofertaIds = ofertas.stream().map(OfertaCurso::getId).toList();
        return ofertaIds.isEmpty()
                ? List.of()
                : seccionRepository.findByOfertaCurso_IdIn(ofertaIds);
    }

    private int sincronizarSecciones(
            OfertaCurso ofertaAncla,
            List<Seccion> existentes,
            int estudiantes2018,
            int estudiantes2023,
            int estudiantesTotales) {
        Map<Integer, Seccion> seccionesPorNumero = new LinkedHashMap<>();
        existentes.stream()
                .sorted(Comparator
                        .comparing((Seccion seccion) ->
                                !seccion.getOfertaCurso().getId().equals(ofertaAncla.getId()))
                        .thenComparingInt(seccion -> numeroDeSeccion(seccion.getCodigo())))
                .forEach(seccion -> {
                    int numero = numeroDeSeccion(seccion.getCodigo());
                    if (numero != Integer.MAX_VALUE) {
                        seccionesPorNumero.putIfAbsent(numero, seccion);
                    }
                });

        List<Seccion> ordenadas = seccionesPorNumero.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(Map.Entry::getValue)
                .toList();
        List<Seccion> activas = new ArrayList<>();
        long capacidadAcumulada = 0;
        for (Seccion seccion : ordenadas) {
            if (capacidadAcumulada >= estudiantesTotales) {
                break;
            }
            validarCapacidad(seccion);
            activas.add(seccion);
            capacidadAcumulada += seccion.getCapacidad();
        }
        while (capacidadAcumulada < estudiantesTotales) {
            Seccion seccion = new Seccion();
            seccion.setCapacidad(capacidadSeccionDefault);
            activas.add(seccion);
            capacidadAcumulada += capacidadSeccionDefault;
        }

        int estudiantesRestantes2018 = estudiantes2018;
        int estudiantesRestantes2023 = estudiantes2023;
        for (int indice = 0; indice < activas.size(); indice++) {
            Seccion seccion = activas.get(indice);
            int ocupacion = (int) Math.min(
                    seccion.getCapacidad(), (long) estudiantesRestantes2018 + estudiantesRestantes2023);
            int cupos2018 = distribuirCuposPlan2018(
                    ocupacion, estudiantes2018, estudiantes2018 + (long) estudiantes2023,
                    estudiantesRestantes2018, estudiantesRestantes2023);
            int cupos2023 = ocupacion - cupos2018;

            seccion.setOfertaCurso(ofertaAncla);
            seccion.setCodigo("S%02d".formatted(indice + 1));
            seccion.setEstado(EstadoSeccion.ACTIVA);
            seccion.setCuposPlan2018(cupos2018);
            seccion.setCuposPlan2023(cupos2023);
            seccionRepository.save(seccion);
            estudiantesRestantes2018 -= cupos2018;
            estudiantesRestantes2023 -= cupos2023;
        }

        for (Seccion seccion : existentes) {
            if (!activas.contains(seccion)) {
                retirarSeccion(seccion);
            }
        }
        return activas.size();
    }

    private int distribuirCuposPlan2018(
            int ocupacion,
            int demanda2018,
            long demandaTotal,
            int restante2018,
            int restante2023) {
        if (demandaTotal == 0) {
            return 0;
        }
        long proporcional = (long) ocupacion * demanda2018 / demandaTotal;
        int minimo = Math.max(0, ocupacion - restante2023);
        int maximo = Math.min(ocupacion, restante2018);
        return (int) Math.max(minimo, Math.min(proporcional, maximo));
    }

    private void validarCapacidad(Seccion seccion) {
        if (seccion.getCapacidad() == null || seccion.getCapacidad() <= 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "La capacidad de cada sección debe ser mayor que cero.");
        }
    }

    private void retirarSeccion(Seccion seccion) {
        seccion.setEstado(EstadoSeccion.RETIRADA);
        seccion.setCuposPlan2018(0);
        seccion.setCuposPlan2023(0);
        seccionRepository.save(seccion);
    }

    private int numeroDeSeccion(String codigo) {
        try {
            return Integer.parseInt(codigo.replaceFirst("^S", ""));
        } catch (NumberFormatException exception) {
            return Integer.MAX_VALUE;
        }
    }

    private record OfertaDemanda(Curso curso, OfertaCurso oferta, int estudiantes) {
    }
}
