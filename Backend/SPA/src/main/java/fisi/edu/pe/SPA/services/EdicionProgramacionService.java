package fisi.edu.pe.SPA.services;

import fisi.edu.pe.SPA.domain.entity.Convalidacion;
import fisi.edu.pe.SPA.domain.entity.OfertaCurso;
import fisi.edu.pe.SPA.domain.entity.Planificacion;
import fisi.edu.pe.SPA.domain.entity.Seccion;
import fisi.edu.pe.SPA.domain.entity.SeccionHorario;
import fisi.edu.pe.SPA.domain.entity.Usuario;
import fisi.edu.pe.SPA.domain.enums.EstadoSeccion;
import fisi.edu.pe.SPA.dto.programacion.ActualizarHorarioRequest;
import fisi.edu.pe.SPA.dto.programacion.ActualizarOfertaRequest;
import fisi.edu.pe.SPA.dto.programacion.ActualizarSeccionRequest;
import fisi.edu.pe.SPA.dto.programacion.OfertaCursoDto;
import fisi.edu.pe.SPA.dto.programacion.HorarioDto;
import fisi.edu.pe.SPA.dto.programacion.HorarioRequest;
import fisi.edu.pe.SPA.dto.programacion.SeccionDto;
import fisi.edu.pe.SPA.dto.programacion.SeccionRequest;
import fisi.edu.pe.SPA.repository.ConvalidacionRespository;
import fisi.edu.pe.SPA.repository.OfertaCursoRepository;
import fisi.edu.pe.SPA.repository.PlanificacionRepository;
import fisi.edu.pe.SPA.repository.SeccionHorarioRepository;
import fisi.edu.pe.SPA.repository.SeccionRepository;
import fisi.edu.pe.SPA.repository.UsuarioRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class EdicionProgramacionService {
    private final PlanificacionRepository planificacionRepository;
    private final OfertaCursoRepository ofertaCursoRepository;
    private final SeccionRepository seccionRepository;
    private final SeccionHorarioRepository horarioRepository;
    private final ConvalidacionRespository convalidacionRepository;
    private final UsuarioRepository usuarioRepository;
    private final AuditoriaPlanificacionService auditoriaService;

    public EdicionProgramacionService(
            PlanificacionRepository planificacionRepository,
            OfertaCursoRepository ofertaCursoRepository,
            SeccionRepository seccionRepository,
            SeccionHorarioRepository horarioRepository,
            ConvalidacionRespository convalidacionRepository,
            UsuarioRepository usuarioRepository,
            AuditoriaPlanificacionService auditoriaService) {
        this.planificacionRepository = planificacionRepository;
        this.ofertaCursoRepository = ofertaCursoRepository;
        this.seccionRepository = seccionRepository;
        this.horarioRepository = horarioRepository;
        this.convalidacionRepository = convalidacionRepository;
        this.usuarioRepository = usuarioRepository;
        this.auditoriaService = auditoriaService;
    }

    @Transactional
    public SeccionDto crearSeccion(UUID planificacionId, UUID ofertaId, SeccionRequest request) {
        validarSeccionRequest(request);
        Planificacion planificacion = bloquearPlanificacion(planificacionId);
        OfertaCurso oferta = ofertaCursoRepository.findById(ofertaId)
                .filter(candidata -> candidata.getPlanificacion().getId().equals(planificacion.getId()))
                .orElseThrow(() -> noEncontrado("Oferta de curso"));
        oferta = ofertaCanonica(oferta, planificacion.getId());
        validarCupos(oferta, request.cuposPlan2018(), request.cuposPlan2023(), request.capacidad());
        String codigo = limpiarCodigo(request.codigo());
        if (seccionRepository.existsByOfertaCurso_IdAndCodigoIgnoreCase(oferta.getId(), codigo)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ya existe una sección con ese código para el curso.");
        }

        Usuario usuario = usuarioActivo(request.usuarioId());
        auditoriaService.establecerContexto(usuario, "Creación manual de sección");
        Seccion seccion = new Seccion();
        seccion.setOfertaCurso(oferta);
        seccion.setCodigo(codigo);
        seccion.setProfesor(limpiarOpcional(request.profesor()));
        seccion.setCapacidad(request.capacidad());
        seccion.setCuposPlan2018(request.cuposPlan2018());
        seccion.setCuposPlan2023(request.cuposPlan2023());
        seccion.setEstado(EstadoSeccion.ACTIVA);
        return convertir(seccionRepository.saveAndFlush(seccion));
    }

    @Transactional
    public SeccionDto actualizarSeccion(
            UUID planificacionId,
            UUID seccionId,
            ActualizarSeccionRequest request) {
        validarActualizarSeccionRequest(request);
        Planificacion planificacion = bloquearPlanificacion(planificacionId);
        Seccion seccion = seccionEnPlan(seccionId, planificacion.getId());
        validarVersion(seccion.getVersionRegistro(), request.version(), "sección");
        validarCupos(seccion.getOfertaCurso(), request.cuposPlan2018(),
                request.cuposPlan2023(), request.capacidad());
        String codigo = limpiarCodigo(request.codigo());
        if (!codigo.equalsIgnoreCase(seccion.getCodigo())
                && seccionRepository.existsByOfertaCurso_IdAndCodigoIgnoreCase(
                        seccion.getOfertaCurso().getId(), codigo)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ya existe una sección con ese código para el curso.");
        }

        Usuario usuario = usuarioActivo(request.usuarioId());
        auditoriaService.establecerContexto(usuario, "Edición de sección");
        seccion.setCodigo(codigo);
        seccion.setProfesor(limpiarOpcional(request.profesor()));
        seccion.setCapacidad(request.capacidad());
        seccion.setCuposPlan2018(request.cuposPlan2018());
        seccion.setCuposPlan2023(request.cuposPlan2023());
        seccion.setEstado(request.estado());
        return convertir(seccionRepository.saveAndFlush(seccion));
    }

    @Transactional
    public SeccionDto retirarSeccion(UUID planificacionId, UUID seccionId, UUID usuarioId, Long version) {
        Planificacion planificacion = bloquearPlanificacion(planificacionId);
        Seccion seccion = seccionEnPlan(seccionId, planificacion.getId());
        validarVersion(seccion.getVersionRegistro(), version, "sección");
        Usuario usuario = usuarioActivo(usuarioId);
        auditoriaService.establecerContexto(usuario, "Retiro de sección");
        seccion.setEstado(EstadoSeccion.RETIRADA);
        seccion.setCuposPlan2018(0);
        seccion.setCuposPlan2023(0);
        return convertir(seccionRepository.saveAndFlush(seccion));
    }

    @Transactional
    public OfertaCursoDto actualizarOferta(
            UUID planificacionId,
            UUID ofertaId,
            ActualizarOfertaRequest request) {
        validarOfertaRequest(request);
        Planificacion planificacion = bloquearPlanificacion(planificacionId);
        OfertaCurso oferta = ofertaCursoRepository.findById(ofertaId)
                .filter(candidata -> candidata.getPlanificacion().getId().equals(planificacion.getId()))
                .orElseThrow(() -> noEncontrado("Oferta de curso"));
        validarVersion(oferta.getVersionRegistro(), request.version(), "oferta");
        Usuario usuario = usuarioActivo(request.usuarioId());
        auditoriaService.establecerContexto(usuario, "Edición manual de oferta de curso");
        oferta.setEstudiantesEstimados(request.estudiantesEstimados());
        oferta.setCapacidadAplicada(request.capacidadAplicada());
        oferta.setSeccionesPropuestas(request.seccionesPropuestas());
        oferta.setSituacion(request.situacion());
        oferta.setMotivoDecision(limpiarOpcional(request.motivoDecision()));
        return convertir(ofertaCursoRepository.saveAndFlush(oferta));
    }

    @Transactional
    public HorarioDto crearHorario(UUID planificacionId, UUID seccionId, HorarioRequest request) {
        validarHorarioRequest(request);
        Planificacion planificacion = bloquearPlanificacion(planificacionId);
        Seccion seccion = seccionEnPlan(seccionId, planificacion.getId());
        validarNoSeSuperpone(seccionId, null, request.dia(), request.horaInicio(), request.horaFin());
        Usuario usuario = usuarioActivo(request.usuarioId());
        auditoriaService.establecerContexto(usuario, "Creación de horario de sección");
        SeccionHorario horario = new SeccionHorario();
        horario.setSeccion(seccion);
        horario.setDia(request.dia());
        horario.setHoraInicio(request.horaInicio());
        horario.setHoraFin(request.horaFin());
        return convertir(horarioRepository.saveAndFlush(horario));
    }

    @Transactional
    public HorarioDto actualizarHorario(
            UUID planificacionId,
            UUID seccionId,
            UUID horarioId,
            ActualizarHorarioRequest request) {
        validarActualizarHorarioRequest(request);
        Planificacion planificacion = bloquearPlanificacion(planificacionId);
        seccionEnPlan(seccionId, planificacion.getId());
        SeccionHorario horario = horarioRepository.findById(horarioId)
                .filter(candidato -> candidato.getSeccion().getId().equals(seccionId))
                .orElseThrow(() -> noEncontrado("Horario"));
        validarVersion(horario.getVersionRegistro(), request.version(), "horario");
        validarNoSeSuperpone(
                seccionId, horarioId, request.dia(), request.horaInicio(), request.horaFin());
        Usuario usuario = usuarioActivo(request.usuarioId());
        auditoriaService.establecerContexto(usuario, "Edición de horario de sección");
        horario.setDia(request.dia());
        horario.setHoraInicio(request.horaInicio());
        horario.setHoraFin(request.horaFin());
        return convertir(horarioRepository.saveAndFlush(horario));
    }

    @Transactional
    public void eliminarHorario(UUID planificacionId, UUID seccionId, UUID horarioId, UUID usuarioId) {
        Planificacion planificacion = bloquearPlanificacion(planificacionId);
        seccionEnPlan(seccionId, planificacion.getId());
        SeccionHorario horario = horarioRepository.findById(horarioId)
                .filter(candidato -> candidato.getSeccion().getId().equals(seccionId))
                .orElseThrow(() -> noEncontrado("Horario"));
        Usuario usuario = usuarioActivo(usuarioId);
        auditoriaService.establecerContexto(usuario, "Eliminación de horario de sección");
        horarioRepository.delete(horario);
        horarioRepository.flush();
    }

    private Planificacion bloquearPlanificacion(UUID planificacionId) {
        return planificacionRepository.findByIdForUpdate(planificacionId)
                .orElseThrow(() -> noEncontrado("Planificación"));
    }

    private Seccion seccionEnPlan(UUID seccionId, UUID planificacionId) {
        Seccion seccion = seccionRepository.findById(seccionId)
                .orElseThrow(() -> noEncontrado("Sección"));
        if (!seccion.getOfertaCurso().getPlanificacion().getId().equals(planificacionId)) {
            throw noEncontrado("Sección");
        }
        return seccion;
    }

    private void validarCupos(OfertaCurso oferta, Integer cupos2018, Integer cupos2023, Integer capacidad) {
        if (capacidad == null || capacidad <= 0
                || cupos2018 == null || cupos2018 < 0
                || cupos2023 == null || cupos2023 < 0
                || (long) cupos2018 + cupos2023 > capacidad) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La capacidad debe ser positiva y los cupos no negativos, sin superar la capacidad.");
        }
        Short anioPlan = oferta.getCurso().getPlan().getAnio();
        if (!Short.valueOf((short) 2018).equals(anioPlan)
                && !Short.valueOf((short) 2023).equals(anioPlan)) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Solo se admiten cursos de planes 2018 y 2023.");
        }
        boolean convalidado = (short) 2018 == anioPlan
                ? !convalidacionRepository.findByCursoConvalidado2018_Id(oferta.getCurso().getId()).isEmpty()
                : !convalidacionRepository.findByCurso2023_Id(oferta.getCurso().getId()).isEmpty();
        if (!convalidado && ((short) 2018 == anioPlan && cupos2023 != 0
                || (short) 2023 == anioPlan && cupos2018 != 0)) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Un curso sin convalidación solo puede tener cupos de su propio plan.");
        }
    }

    private OfertaCurso ofertaCanonica(OfertaCurso oferta, UUID planificacionId) {
        if (!Short.valueOf((short) 2018).equals(oferta.getCurso().getPlan().getAnio())) {
            return oferta;
        }
        return convalidacionRepository.findByCursoConvalidado2018_Id(oferta.getCurso().getId()).stream()
                .findFirst()
                .flatMap(convalidacion -> ofertaCursoRepository.findByPlanificacion_IdAndCurso_Id(
                        planificacionId, convalidacion.getCurso2023().getId()))
                .orElse(oferta);
    }

    private void validarNoSeSuperpone(
            UUID seccionId,
            UUID horarioExcluido,
            Short dia,
            java.time.LocalTime horaInicio,
            java.time.LocalTime horaFin) {
        if (dia == null || dia < 1 || dia > 7 || horaInicio == null || horaFin == null
                || !horaInicio.isBefore(horaFin)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El día debe estar entre 1 y 7 y la hora de inicio debe preceder a la hora de fin.");
        }
        boolean superpuesto = horarioRepository.findBySeccion_IdOrderByDiaAscHoraInicioAsc(seccionId).stream()
                .filter(horario -> !horario.getId().equals(horarioExcluido))
                .anyMatch(horario -> horario.getDia().equals(dia)
                        && horaInicio.isBefore(horario.getHoraFin())
                        && horario.getHoraInicio().isBefore(horaFin));
        if (superpuesto) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El horario se superpone con otro bloque de la sección.");
        }
    }

    private void validarSeccionRequest(SeccionRequest request) {
        if (request == null || request.usuarioId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe indicar usuarioId.");
        }
        validarCodigo(request.codigo());
        validarLongitud(request.profesor(), 200, "profesor");
    }

    private void validarActualizarSeccionRequest(ActualizarSeccionRequest request) {
        if (request == null || request.usuarioId() == null || request.estado() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Debe indicar usuarioId y estado.");
        }
        validarVersionRequerida(request.version(), "sección");
        validarCodigo(request.codigo());
        validarLongitud(request.profesor(), 200, "profesor");
    }

    private void validarOfertaRequest(ActualizarOfertaRequest request) {
        if (request == null || request.usuarioId() == null
                || request.estudiantesEstimados() == null || request.estudiantesEstimados() < 0
                || request.capacidadAplicada() == null || request.capacidadAplicada() <= 0
                || request.seccionesPropuestas() == null || request.seccionesPropuestas() < 0
                || request.situacion() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Indique usuarioId y valores válidos para la oferta.");
        }
        validarVersionRequerida(request.version(), "oferta");
    }

    private void validarHorarioRequest(HorarioRequest request) {
        if (request == null || request.usuarioId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe indicar usuarioId.");
        }
        validarNoSeSuperponeSinConsulta(request.dia(), request.horaInicio(), request.horaFin());
    }

    private void validarActualizarHorarioRequest(ActualizarHorarioRequest request) {
        if (request == null || request.usuarioId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe indicar usuarioId.");
        }
        validarVersionRequerida(request.version(), "horario");
        validarNoSeSuperponeSinConsulta(request.dia(), request.horaInicio(), request.horaFin());
    }

    private void validarNoSeSuperponeSinConsulta(
            Short dia,
            java.time.LocalTime horaInicio,
            java.time.LocalTime horaFin) {
        if (dia == null || dia < 1 || dia > 7 || horaInicio == null || horaFin == null
                || !horaInicio.isBefore(horaFin)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El día debe estar entre 1 y 7 y la hora de inicio debe preceder a la hora de fin.");
        }
    }

    private void validarCodigo(String codigo) {
        if (codigo == null || codigo.isBlank() || codigo.trim().length() > 30) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El código de sección es obligatorio y no debe superar 30 caracteres.");
        }
    }

    private void validarLongitud(String valor, int maximo, String campo) {
        if (valor != null && valor.length() > maximo) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El campo " + campo + " no debe superar " + maximo + " caracteres.");
        }
    }

    private void validarVersionRequerida(Long version, String recurso) {
        if (version == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Debe enviar la versión actual del " + recurso + ".");
        }
    }

    private void validarVersion(Long actual, Long enviada, String recurso) {
        validarVersionRequerida(enviada, recurso);
        if (actual == null || !actual.equals(enviada)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El " + recurso + " fue modificado por otra operación; vuelva a cargarlo.");
        }
    }

    private Usuario usuarioActivo(UUID usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> noEncontrado("Usuario"));
        if (!Boolean.TRUE.equals(usuario.getActivo())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "El usuario está inactivo.");
        }
        return usuario;
    }

    private String limpiarCodigo(String codigo) {
        return codigo.trim();
    }

    private String limpiarOpcional(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    private HorarioDto convertir(SeccionHorario horario) {
        return new HorarioDto(
                horario.getId(),
                horario.getDia(),
                horario.getHoraInicio(),
                horario.getHoraFin(),
                horario.getVersionRegistro());
    }

    private SeccionDto convertir(Seccion seccion) {
        List<HorarioDto> horarios = horarioRepository
                .findBySeccion_IdOrderByDiaAscHoraInicioAsc(seccion.getId())
                .stream()
                .map(this::convertir)
                .toList();
        return new SeccionDto(
                seccion.getId(),
                seccion.getCodigo(),
                seccion.getProfesor(),
                seccion.getCapacidad(),
                seccion.getCuposPlan2018(),
                seccion.getCuposPlan2023(),
                seccion.getEstado(),
                seccion.getVersionRegistro(),
                horarios);
    }

    private OfertaCursoDto convertir(OfertaCurso oferta) {
        return new OfertaCursoDto(
                oferta.getId(),
                oferta.getCurso().getId(),
                oferta.getCurso().getCodigo(),
                oferta.getCurso().getNombre(),
                oferta.getEstudiantesEstimados(),
                oferta.getCapacidadAplicada(),
                oferta.getSeccionesCalculadas(),
                oferta.getSeccionesPropuestas(),
                oferta.getSituacion(),
                oferta.getMotivoDecision(),
                oferta.getVersionRegistro());
    }

    private ResponseStatusException noEncontrado(String recurso) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, recurso + " no encontrado.");
    }
}
