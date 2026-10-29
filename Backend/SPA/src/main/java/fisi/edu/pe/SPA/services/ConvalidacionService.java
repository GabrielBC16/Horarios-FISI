package fisi.edu.pe.SPA.services;

import fisi.edu.pe.SPA.domain.entity.Convalidacion;
import fisi.edu.pe.SPA.domain.entity.Curso;
import fisi.edu.pe.SPA.dto.convalidacion.ConvalidacionDto;
import fisi.edu.pe.SPA.dto.convalidacion.ConvalidacionRequestDto;
import fisi.edu.pe.SPA.dto.convalidacion.CursoReferenciaDto;
import fisi.edu.pe.SPA.repository.ConvalidacionRespository;
import fisi.edu.pe.SPA.repository.CursoRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ConvalidacionService {
    private static final Short PLAN_2018 = (short) 2018;
    private static final Short PLAN_2023 = (short) 2023;

    private final ConvalidacionRespository convalidacionRepository;
    private final CursoRepository cursoRepository;

    public ConvalidacionService(
            ConvalidacionRespository convalidacionRepository,
            CursoRepository cursoRepository) {
        this.convalidacionRepository = convalidacionRepository;
        this.cursoRepository = cursoRepository;
    }

    @Transactional
    public ConvalidacionDto crear(ConvalidacionRequestDto request) {
        CursosEquivalentes cursos = resolverCursos(request);
        if (convalidacionRepository.existsByCursoConvalidado2018_Id(cursos.curso2018().getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El curso del plan 2018 ya tiene una convalidación.");
        }
        return guardar(new Convalidacion(), cursos);
    }

    @Transactional
    public ConvalidacionDto actualizar(UUID id, ConvalidacionRequestDto request) {
        Convalidacion convalidacion = convalidacionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Convalidación no encontrada."));
        CursosEquivalentes cursos = resolverCursos(request);
        if (convalidacionRepository.existsByCursoConvalidado2018_IdAndIdNot(
                cursos.curso2018().getId(), id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El curso del plan 2018 ya tiene otra convalidación.");
        }
        return guardar(convalidacion, cursos);
    }

    @Transactional(readOnly = true)
    public List<ConvalidacionDto> listarTodas() {
        return convalidacionRepository.listarTodas().stream()
                .map(this::convertir)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ConvalidacionDto> listarPorCiclo2018(Short ciclo) {
        if (ciclo == null || ciclo < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El ciclo debe ser un número positivo.");
        }
        return convalidacionRepository.listarPorCiclo2018(ciclo).stream()
                .map(this::convertir)
                .toList();
    }

    private CursosEquivalentes resolverCursos(ConvalidacionRequestDto request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Debe enviar los cursos de ambos planes.");
        }

        Curso curso2018 = resolverCurso(request.curso2018(), PLAN_2018, request.escuelaId());
        Curso curso2023 = resolverCurso(request.curso2023(), PLAN_2023, request.escuelaId());
        if (!curso2018.getPlan().getAreaEscuela().getId()
                .equals(curso2023.getPlan().getAreaEscuela().getId())) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Los cursos deben pertenecer a la misma escuela.");
        }
        return new CursosEquivalentes(curso2018, curso2023);
    }

    private Curso resolverCurso(CursoReferenciaDto referencia, Short anioPlan, UUID escuelaId) {
        if (referencia == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Debe indicar el curso del plan " + anioPlan + ".");
        }

        String codigo = limpiar(referencia.codigo());
        String nombre = limpiar(referencia.nombre());
        if ((codigo == null) == (nombre == null)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Indique exactamente uno de estos campos: codigo o nombre.");
        }

        List<Curso> encontrados = codigo != null
                ? cursoRepository.buscarActivosPorAnioYCodigo(
                        anioPlan, codigo, referencia.planId(), escuelaId)
                : cursoRepository.buscarActivosPorAnioYNombre(
                        anioPlan, nombre, referencia.planId(), escuelaId);

        if (encontrados.isEmpty()) {
            String identificador = codigo != null ? "código " + codigo : "nombre " + nombre;
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "No existe un curso activo con " + identificador + " en el plan " + anioPlan + ".");
        }
        if (encontrados.size() > 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "La referencia identifica varios cursos del plan " + anioPlan
                            + "; indique planId o escuelaId, o use el código si buscó por nombre.");
        }
        return encontrados.get(0);
    }

    private ConvalidacionDto guardar(Convalidacion convalidacion, CursosEquivalentes cursos) {
        convalidacion.setCursoConvalidado2018(cursos.curso2018());
        convalidacion.setCurso2023(cursos.curso2023());
        Convalidacion guardada = convalidacionRepository.save(convalidacion);
        return convertir(guardada);
    }

    private ConvalidacionDto convertir(Convalidacion convalidacion) {
        return new ConvalidacionDto(
                convalidacion.getId(),
                convertirCurso(convalidacion.getCursoConvalidado2018()),
                convertirCurso(convalidacion.getCurso2023()));
    }

    private ConvalidacionDto.CursoDto convertirCurso(Curso curso) {
        return new ConvalidacionDto.CursoDto(
                curso.getId(),
                curso.getCodigo(),
                curso.getNombre(),
                curso.getCiclo(),
                curso.getPlan().getAnio(),
                curso.getPlan().getAreaEscuela().getId());
    }

    private String limpiar(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    private record CursosEquivalentes(Curso curso2018, Curso curso2023) {
    }
}
