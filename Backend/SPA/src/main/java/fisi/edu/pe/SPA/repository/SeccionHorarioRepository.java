package fisi.edu.pe.SPA.repository;

import fisi.edu.pe.SPA.domain.entity.SeccionHorario;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SeccionHorarioRepository extends JpaRepository<SeccionHorario, UUID> {
    List<SeccionHorario> findBySeccion_IdOrderByDiaAscHoraInicioAsc(UUID seccionId);
    List<SeccionHorario> findBySeccion_IdInOrderByDiaAscHoraInicioAsc(Collection<UUID> seccionIds);
}
