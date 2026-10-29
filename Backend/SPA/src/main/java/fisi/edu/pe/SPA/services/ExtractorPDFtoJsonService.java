package fisi.edu.pe.SPA.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import fisi.edu.pe.SPA.domain.entity.Curso;
import fisi.edu.pe.SPA.domain.entity.Importacion;
import fisi.edu.pe.SPA.domain.entity.PlanCurricular;
import fisi.edu.pe.SPA.domain.entity.Planificacion;
import fisi.edu.pe.SPA.domain.entity.Usuario;
import fisi.edu.pe.SPA.domain.enums.EstadoImportacion;
import fisi.edu.pe.SPA.dto.importacion.DemandaExtraidaDto;
import fisi.edu.pe.SPA.dto.importacion.FilaImportacionDto;
import fisi.edu.pe.SPA.dto.importacion.VistaPreviaImportacionDto;
import fisi.edu.pe.SPA.repository.CursoRepository;
import fisi.edu.pe.SPA.repository.ImportacionRepository;
import fisi.edu.pe.SPA.repository.PlanCurricularRepository;
import fisi.edu.pe.SPA.repository.PlanificacionRepository;
import fisi.edu.pe.SPA.repository.UsuarioRepository;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ExtractorPDFtoJsonService {
    private static final Duration EXTRACTOR_TIMEOUT = Duration.ofSeconds(20);
    private static final String PLAN_CURRICULAR_OBSOLETO = "2015";

    private final ObjectMapper objectMapper;
    private final ImportacionRepository importacionRepository;
    private final PlanificacionRepository planificacionRepository;
    private final UsuarioRepository usuarioRepository;
    private final PlanCurricularRepository planCurricularRepository;
    private final CursoRepository cursoRepository;
    private final AuditoriaPlanificacionService auditoriaService;
    private final Path directorioImportaciones;
    private final String ejecutablePython;
    private final String scriptExtractor;

    public ExtractorPDFtoJsonService(
            ObjectMapper objectMapper,
            ImportacionRepository importacionRepository,
            PlanificacionRepository planificacionRepository,
            UsuarioRepository usuarioRepository,
            PlanCurricularRepository planCurricularRepository,
            CursoRepository cursoRepository,
            AuditoriaPlanificacionService auditoriaService,
            @Value("${app.importacion.directorio:uploads/importaciones}") String directorioImportaciones,
            @Value("${app.extractor.python:python}") String ejecutablePython,
            @Value("${app.extractor.script:../extraccion/script_Pdf.py}") String scriptExtractor) {
        this.objectMapper = objectMapper;
        this.importacionRepository = importacionRepository;
        this.planificacionRepository = planificacionRepository;
        this.usuarioRepository = usuarioRepository;
        this.planCurricularRepository = planCurricularRepository;
        this.cursoRepository = cursoRepository;
        this.auditoriaService = auditoriaService;
        this.directorioImportaciones = Path.of(directorioImportaciones).toAbsolutePath().normalize();
        this.ejecutablePython = ejecutablePython;
        this.scriptExtractor = scriptExtractor;
    }

    @Transactional
    public VistaPreviaImportacionDto extraerYValidar(UUID planificacionId, UUID usuarioId, MultipartFile archivo) {
        System.out.println("1. Validando archivo PDF");
        validarPdf(archivo);
        Planificacion planificacion = planificacionRepository.findById(planificacionId)
                .orElseThrow(() -> noEncontrado("Planificación"));
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> noEncontrado("Usuario"));
        if (!Boolean.TRUE.equals(usuario.getActivo())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "El usuario está inactivo.");
        }

        Path archivoGuardado = guardarArchivo(archivo);
        System.out.println("2. Ejecutando extractor");
        List<DemandaExtraidaDto> demanda = excluirPlanCurricularObsoleto(ejecutarExtractor(archivoGuardado));
        System.out.println("3. Validando demanda");
        List<FilaImportacionDto> filas = validarDemanda(planificacion, demanda);

        Importacion importacion = new Importacion();
        importacion.setPlanificacion(planificacion);
        importacion.setImportadoPor(usuario);
        importacion.setNombreArchivo(nombreSeguro(archivo.getOriginalFilename()));
        importacion.setArchivoUri(archivoGuardado.toString());
        importacion.setTotalRegistros(filas.size());
        importacion.setRegistrosValidos((int) filas.stream().filter(FilaImportacionDto::valida).count());
        importacion.setRegistrosInvalidos((int) filas.stream().filter(f -> !f.valida()).count());
        importacion.setEstado(importacion.getRegistrosInvalidos() < 12
                ? EstadoImportacion.VALIDADA
                : EstadoImportacion.RECHAZADA);
        importacion.setDetalleValidacion(Map.of("filas", filas));
        auditoriaService.establecerContexto(usuario, "Importación de demanda académica");
        importacionRepository.save(importacion);

        System.out.println("4. Devolviendo vista previa");
        return vistaPrevia(importacion, filas);
    }

    @Transactional(readOnly = true)
    public VistaPreviaImportacionDto obtenerVistaPrevia(UUID importacionId) {
        Importacion importacion = obtenerImportacion(importacionId);
        System.out.println("5. Obteniendo vista previa");
        return vistaPrevia(importacion, obtenerFilas(importacion));
    }

    private List<FilaImportacionDto> validarDemanda(Planificacion planificacion, List<DemandaExtraidaDto> demanda) {
        Map<String, PlanCurricular> planesPorAnio = new LinkedHashMap<>();
        for (PlanCurricular plan : planCurricularRepository
                .findByAreaEscuela_IdAndActivoTrue(planificacion.getAreaEscuela().getId())) {
            if (plan.getAnio() != null) {
                planesPorAnio.put(plan.getAnio().toString(), plan);
            }
            planesPorAnio.putIfAbsent(plan.getCodigo().trim(), plan);
            System.out.println("Plan curricular encontrado: " + plan.getCodigo() + " - " + plan.getAnio());
        }
        List<FilaImportacionDto> resultado = new ArrayList<>();
        for (DemandaExtraidaDto registro : demanda) {
            for (Map.Entry<String, Integer> entrada : registro.planes().entrySet()) {
                Integer cantidad = entrada.getValue();
                if (cantidad == null || cantidad == 0)
                    continue;
                PlanCurricular plan = planesPorAnio.get(entrada.getKey());
                if (plan == null) {
                    resultado.add(filaInvalida(registro, entrada.getKey(), cantidad,
                            "No existe el plan curricular " + entrada.getKey() + " en la Escuela."));
                    continue;
                }
                if (registro.codigo() == null || registro.codigo().isBlank()) {
                    resultado.add(
                            filaInvalida(registro, entrada.getKey(), cantidad, "El PDF no contiene código de curso."));
                    continue;
                }
                Curso curso = cursoRepository.findByPlan_IdAndCodigoIgnoreCase(plan.getId(), registro.codigo().trim())
                        .orElse(null);
                if (curso == null) {
                    resultado.add(filaInvalida(registro, entrada.getKey(), cantidad,
                            "No se encontró el código " + registro.codigo().trim() + " en el plan curricular."));
                    System.out.println(
                            "Curso no encontrado: " + registro.codigo().trim() + " en plan: " + entrada.getKey());
                } else if (!Boolean.TRUE.equals(curso.getActivo())) {
                    resultado.add(filaInvalida(registro, entrada.getKey(), cantidad, "El curso está inactivo."));
                } else {
                    resultado.add(new FilaImportacionDto(registro.numero(), registro.codigo(), registro.asignatura(),
                            entrada.getKey(),
                            cantidad, curso.getId(), true, null));
                }
            }
        }
        return resultado;
    }

    private FilaImportacionDto filaInvalida(DemandaExtraidaDto r, String plan, Integer cantidad, String error) {
        return new FilaImportacionDto(r.numero(), r.codigo(), r.asignatura(), plan, cantidad, null, false, error);
    }

    private Path guardarArchivo(MultipartFile archivo) {
        System.out.println("Guardando archivo PDF en: " + directorioImportaciones);
        try {
            Files.createDirectories(directorioImportaciones);
            Path destino = directorioImportaciones.resolve(UUID.randomUUID() + ".pdf");
            try (var entrada = archivo.getInputStream()) {
                Files.copy(entrada, destino, StandardCopyOption.REPLACE_EXISTING);
            }
            System.out.println("Archivo PDF guardado en: " + destino);
            return destino;
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo guardar el PDF.", e);
        }
    }

    private List<DemandaExtraidaDto> ejecutarExtractor(Path archivo) {
        System.out.println("Ejecutando extractor");
        Path script = Path.of(scriptExtractor).toAbsolutePath().normalize();
        if (!Files.isRegularFile(script)) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "No se encontró el script extractor configurado.");
        }

        Path outFile = null;
        Path errFile = null;
        try {
            outFile = Files.createTempFile("extractor-out-", ".json");
            errFile = Files.createTempFile("extractor-err-", ".txt");

            System.out.println("Ejecutando extractor: " + ejecutablePython + " " + script + " --input " + archivo);

            ProcessBuilder pb = new ProcessBuilder(
                    ejecutablePython,
                    "-u", // Python sin buffering
                    script.toString(),
                    "--input", archivo.toString());
            pb.redirectOutput(outFile.toFile());
            pb.redirectError(errFile.toFile());
            pb.environment().put("PYTHONUNBUFFERED", "1");
            pb.environment().put("PYTHONIOENCODING", "utf-8");

            Process proceso = pb.start();

            boolean termino = proceso.waitFor(EXTRACTOR_TIMEOUT.toSeconds(), TimeUnit.SECONDS);
            if (!termino) {
                proceso.destroyForcibly();
                proceso.waitFor(5, TimeUnit.SECONDS);
                throw new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT,
                        "El extractor excedió el tiempo máximo.");
            }

            String salida = Files.readString(outFile, StandardCharsets.UTF_8).trim();
            String errores = Files.readString(errFile, StandardCharsets.UTF_8);

            if (proceso.exitValue() != 0) {
                System.out.println("Errores en el extractor: " + errores);
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "El PDF no pudo procesarse: " + errores);
            }
            if (salida.isEmpty()) {
                System.out.println("stderr del extractor: " + errores); // ayuda a depurar
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "El extractor no produjo salida.");
            }

            System.out.println("Salida del extractor: " + salida);
            return objectMapper.readValue(salida, new TypeReference<List<DemandaExtraidaDto>>() {
            });

        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "No se pudo ejecutar el extractor.", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Ejecución del extractor interrumpida.", e);
        } finally {
            try {
                if (outFile != null)
                    Files.deleteIfExists(outFile);
            } catch (IOException ignored) {
            }
            try {
                if (errFile != null)
                    Files.deleteIfExists(errFile);
            } catch (IOException ignored) {
            }
        }
    }

    private List<FilaImportacionDto> obtenerFilas(Importacion importacion) {
        Object filas = importacion.getDetalleValidacion() == null ? null
                : importacion.getDetalleValidacion().get("filas");
        if (filas == null)
            return List.of();
        return objectMapper.convertValue(filas, new TypeReference<List<FilaImportacionDto>>() {
        });
    }

    private VistaPreviaImportacionDto vistaPrevia(Importacion i, List<FilaImportacionDto> filas) {
        return new VistaPreviaImportacionDto(i.getId(), i.getEstado().name(), i.getTotalRegistros(),
                i.getRegistrosValidos(), i.getRegistrosInvalidos(), filas);
    }

    private List<DemandaExtraidaDto> excluirPlanCurricularObsoleto(List<DemandaExtraidaDto> demanda) {
        List<DemandaExtraidaDto> resultado = new ArrayList<>(demanda.size());
        for (DemandaExtraidaDto registro : demanda) {
            Map<String, Integer> planesVigentes = new LinkedHashMap<>(registro.planes());
            planesVigentes.remove(PLAN_CURRICULAR_OBSOLETO);
            resultado.add(new DemandaExtraidaDto(
                    registro.numero(),
                    registro.codigo(),
                    registro.asignatura(),
                    planesVigentes));
        }
        return resultado;
    }

    private void validarPdf(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()
                || !nombreSeguro(archivo.getOriginalFilename()).toLowerCase().endsWith(".pdf")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe enviar un archivo PDF no vacío.");
        }
    }

    private String nombreSeguro(String nombre) {
        return nombre == null ? "demanda.pdf" : Path.of(nombre).getFileName().toString();
    }

    private Importacion obtenerImportacion(UUID id) {
        return importacionRepository.findById(id).orElseThrow(() -> noEncontrado("Importación"));
    }

    private ResponseStatusException noEncontrado(String recurso) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, recurso + " no encontrada.");
    }
}
