package fisi.edu.pe.SPA.repository;

import fisi.edu.pe.SPA.domain.entity.Seccion;
import fisi.edu.pe.SPA.domain.enums.EstadoSeccion;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SeccionRepository extends JpaRepository<Seccion, UUID> {
    List<Seccion> findByOfertaCurso_Id(UUID ofertaCursoId);
    List<Seccion> findByOfertaCurso_IdIn(Collection<UUID> ofertaCursoIds);
    List<Seccion> findByOfertaCurso_IdAndEstado(UUID ofertaCursoId, EstadoSeccion estado);
    boolean existsByOfertaCurso_IdAndCodigoIgnoreCase(UUID ofertaCursoId, String codigo);
}
