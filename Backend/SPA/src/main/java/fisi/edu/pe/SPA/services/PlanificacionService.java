package fisi.edu.pe.SPA.services;

import fisi.edu.pe.SPA.domain.entity.Escuela;
import fisi.edu.pe.SPA.domain.entity.PeriodoAcademico;
import fisi.edu.pe.SPA.domain.entity.Planificacion;
import fisi.edu.pe.SPA.domain.entity.Usuario;
import fisi.edu.pe.SPA.dto.planificacion.CrearPlanificacionRequest;
import fisi.edu.pe.SPA.dto.planificacion.PlanificacionDto;
import fisi.edu.pe.SPA.repository.EscuelaRepository;
import fisi.edu.pe.SPA.repository.PeriodoAcademicoRepository;
import fisi.edu.pe.SPA.repository.PlanificacionRepository;
import fisi.edu.pe.SPA.repository.UsuarioRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PlanificacionService {
    private final PlanificacionRepository planificacionRepository;
    private final EscuelaRepository escuelaRepository;
    private final PeriodoAcademicoRepository periodoRepository;
    private final UsuarioRepository usuarioRepository;
    private final AuditoriaPlanificacionService auditoriaService;

    public PlanificacionService(
            PlanificacionRepository planificacionRepository,
            EscuelaRepository escuelaRepository,
            PeriodoAcademicoRepository periodoRepository,
            UsuarioRepository usuarioRepository,
            AuditoriaPlanificacionService auditoriaService) {
        this.planificacionRepository = planificacionRepository;
        this.escuelaRepository = escuelaRepository;
        this.periodoRepository = periodoRepository;
        this.usuarioRepository = usuarioRepository;
        this.auditoriaService = auditoriaService;
    }

    @Transactional
    public PlanificacionDto crear(CrearPlanificacionRequest request) {
        validarRequest(request);
        Escuela escuela = escuelaRepository.findById(request.escuelaId())
                .orElseThrow(() -> noEncontrado("Escuela"));
        PeriodoAcademico periodo = periodoRepository.findById(request.periodoAcademicoId())
                .orElseThrow(() -> noEncontrado("Periodo académico"));
        Usuario usuario = usuarioActivo(request.usuarioId());

        if (planificacionRepository
                .findByAreaEscuela_IdAndPeriodoAcademico_Id(escuela.getId(), periodo.getId())
                .isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ya existe una planificación para la escuela y el periodo indicados.");
        }

        auditoriaService.establecerContexto(usuario, "Creación de planificación académica");
        Planificacion planificacion = new Planificacion();
        planificacion.setAreaEscuela(escuela);
        planificacion.setPeriodoAcademico(periodo);
        planificacion.setCreadoPor(usuario);
        try {
            return convertir(planificacionRepository.saveAndFlush(planificacion));
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ya existe una planificación para la escuela y el periodo indicados.", exception);
        }
    }

    @Transactional(readOnly = true)
    public PlanificacionDto obtener(UUID id) {
        return convertir(planificacionRepository.findById(id)
                .orElseThrow(() -> noEncontrado("Planificación")));
    }

    @Transactional(readOnly = true)
    public List<PlanificacionDto> listarPorEscuela(UUID escuelaId) {
        if (!escuelaRepository.existsById(escuelaId)) {
            throw noEncontrado("Escuela");
        }
        return planificacionRepository.findByAreaEscuela_IdOrderByPeriodoAcademico_CodigoDesc(escuelaId).stream()
                .map(this::convertir)
                .toList();
    }

    private Usuario usuarioActivo(UUID usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> noEncontrado("Usuario"));
        if (!Boolean.TRUE.equals(usuario.getActivo())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "El usuario está inactivo.");
        }
        return usuario;
    }

    private void validarRequest(CrearPlanificacionRequest request) {
        if (request == null || request.escuelaId() == null
                || request.periodoAcademicoId() == null || request.usuarioId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Debe indicar escuelaId, periodoAcademicoId y usuarioId.");
        }
    }

    private PlanificacionDto convertir(Planificacion planificacion) {
        return new PlanificacionDto(
                planificacion.getId(),
                planificacion.getAreaEscuela().getId(),
                planificacion.getAreaEscuela().getNombre(),
                planificacion.getPeriodoAcademico().getId(),
                planificacion.getPeriodoAcademico().getCodigo(),
                planificacion.getCreadoPor().getId(),
                planificacion.getEstado(),
                planificacion.getVersionRegistro());
    }

    private ResponseStatusException noEncontrado(String recurso) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, recurso + " no encontrado.");
    }
}
