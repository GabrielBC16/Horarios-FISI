package fisi.edu.pe.SPA.services;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fisi.edu.pe.SPA.domain.entity.OfertaCurso;
import fisi.edu.pe.SPA.domain.entity.Seccion;
import fisi.edu.pe.SPA.domain.entity.SeccionHorario;
import fisi.edu.pe.SPA.domain.enums.EstadoSeccion;
import fisi.edu.pe.SPA.dto.validacion.OfertaCursoConSeccionesDto;
import fisi.edu.pe.SPA.repository.ConvalidacionRespository;
import fisi.edu.pe.SPA.repository.OfertaCursoRepository;
import fisi.edu.pe.SPA.repository.SeccionHorarioRepository;
import fisi.edu.pe.SPA.repository.SeccionRepository;

@Service
public class ValidarOfertaAcademicaService {

    private static final Short PLAN_2018 = (short) 2018;
    private static final Logger log = LoggerFactory.getLogger(ValidarOfertaAcademicaService.class);
    private final OfertaCursoRepository ofertaCursoRepository;
    private final ConvalidacionRespository convalidacionRepository;
    private final SeccionRepository seccionRepository;
    private final SeccionHorarioRepository horarioRepository;

    public ValidarOfertaAcademicaService(
            OfertaCursoRepository ofertaCursoRepository,
            ConvalidacionRespository convalidacionRepository,
            SeccionRepository seccionRepository,
            SeccionHorarioRepository horarioRepository) {
        this.ofertaCursoRepository = ofertaCursoRepository;
        this.convalidacionRepository = convalidacionRepository;
        this.seccionRepository = seccionRepository;
        this.horarioRepository = horarioRepository;
    }

    @Transactional(readOnly = true)
    public List<OfertaCursoConSeccionesDto> getOfertaAcademicaByPlanificacionId(UUID planificacionId) {
        log.info("Consultando ofertas de cursos para la planificacion {}", planificacionId);
        long inicioConsultaOfertas = System.nanoTime();
        List<OfertaCurso> ofertas = ofertaCursoRepository.findByPlanificacion_Id(planificacionId);
        log.info("Consulta de ofertas finalizada: {} resultados en {} ms",
                ofertas.size(), elapsedMillis(inicioConsultaOfertas));

        if (ofertas.isEmpty()) {
            log.info("No se encontraron ofertas para la planificacion {}", planificacionId);
            return List.of();
        }

        List<UUID> ofertaIds = ofertas.stream().map(OfertaCurso::getId).toList();
        log.info("Consultando secciones asociadas a {} ofertas", ofertaIds.size());
        long inicioConsultaSecciones = System.nanoTime();
        List<Seccion> secciones = seccionRepository.findByOfertaCurso_IdIn(ofertaIds);
        log.info("Consulta de secciones finalizada: {} resultados en {} ms",
                secciones.size(), elapsedMillis(inicioConsultaSecciones));
        Map<UUID, List<fisi.edu.pe.SPA.dto.programacion.HorarioDto>> horariosPorSeccion;
        if (secciones.isEmpty()) {
            horariosPorSeccion = Map.of();
        } else {
            horariosPorSeccion = horarioRepository.findBySeccion_IdInOrderByDiaAscHoraInicioAsc(
                            secciones.stream().map(Seccion::getId).toList())
                    .stream()
                    .collect(Collectors.groupingBy(
                            horario -> horario.getSeccion().getId(),
                            Collectors.mapping(this::toHorarioDto, Collectors.toList())));
        }

        Map<UUID, UUID> curso2018A2023 = convalidacionRepository.findAll().stream()
                .collect(Collectors.toMap(
                        convalidacion -> convalidacion.getCursoConvalidado2018().getId(),
                        convalidacion -> convalidacion.getCurso2023().getId(),
                        (existente, nuevo) -> nuevo,
                        LinkedHashMap::new));
        Map<UUID, List<OfertaCursoConSeccionesDto.SeccionDto>> seccionesPorGrupo = secciones.stream()
                .collect(Collectors.groupingBy(
                        seccion -> grupoDeCurso(
                                seccion.getOfertaCurso().getCurso().getId(),
                                seccion.getOfertaCurso().getCurso().getPlan().getAnio(),
                                curso2018A2023),
                        Collectors.mapping(
                                seccion -> toSeccionDto(seccion, horariosPorSeccion),
                                Collectors.toList())));

        log.info("Preparando la respuesta para {} ofertas", ofertas.size());
        long inicioMapeo = System.nanoTime();
        List<OfertaCursoConSeccionesDto> respuesta = ofertas.stream()
                .map(oferta -> toOfertaDto(
                        oferta,
                        seccionesPorGrupo.getOrDefault(
                                grupoDeCurso(
                                        oferta.getCurso().getId(),
                                        oferta.getCurso().getPlan().getAnio(),
                                        curso2018A2023),
                                List.of())))
                .toList();
        log.info("Respuesta preparada con {} ofertas en {} ms",
                respuesta.size(), elapsedMillis(inicioMapeo));
        return respuesta;
    }

    private OfertaCursoConSeccionesDto toOfertaDto(
            OfertaCurso oferta,
            List<OfertaCursoConSeccionesDto.SeccionDto> secciones) {
        var curso = oferta.getCurso();
        log.debug("Mapeando oferta {} del curso {} con {} secciones",
                oferta.getId(), curso.getCodigo(), secciones.size());
        return new OfertaCursoConSeccionesDto(
                oferta.getId(),
                new OfertaCursoConSeccionesDto.CursoDto(
                        curso.getId(), curso.getCodigo(), curso.getNombre(), curso.getCiclo()),
                oferta.getEstudiantesEstimados(),
                oferta.getCapacidadAplicada(),
                (int) secciones.stream()
                        .filter(seccion -> seccion.estado() == EstadoSeccion.ACTIVA)
                        .count(),
                oferta.getSeccionesPropuestas(),
                oferta.getSituacion(),
                oferta.getMotivoDecision(),
                oferta.getVersionRegistro(),
                secciones);
    }

    private OfertaCursoConSeccionesDto.SeccionDto toSeccionDto(
            Seccion seccion,
            Map<UUID, List<fisi.edu.pe.SPA.dto.programacion.HorarioDto>> horariosPorSeccion) {
        return new OfertaCursoConSeccionesDto.SeccionDto(
                seccion.getId(),
                seccion.getCodigo(),
                seccion.getProfesor(),
                seccion.getCapacidad(),
                seccion.getCuposPlan2018(),
                seccion.getCuposPlan2023(),
                seccion.getEstado(),
                seccion.getVersionRegistro(),
                horariosPorSeccion.getOrDefault(seccion.getId(), List.of()));
    }

    private fisi.edu.pe.SPA.dto.programacion.HorarioDto toHorarioDto(SeccionHorario horario) {
        return new fisi.edu.pe.SPA.dto.programacion.HorarioDto(
                horario.getId(),
                horario.getDia(),
                horario.getHoraInicio(),
                horario.getHoraFin(),
                horario.getVersionRegistro());
    }

    private UUID grupoDeCurso(UUID cursoId, Short anioPlan, Map<UUID, UUID> curso2018A2023) {
        if (PLAN_2018.equals(anioPlan)) {
            return curso2018A2023.getOrDefault(cursoId, cursoId);
        }
        return cursoId;
    }

    private long elapsedMillis(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }

}
