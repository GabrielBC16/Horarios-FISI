package fisi.edu.pe.SPA.repository;

import fisi.edu.pe.SPA.domain.entity.SeccionHorario;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SeccionHorarioRepository extends JpaRepository<SeccionHorario, UUID> {
    List<SeccionHorario> findBySeccion_IdOrderByDiaAscHoraInicioAsc(UUID seccionId);
    List<SeccionHorario> findBySeccion_IdInOrderByDiaAscHoraInicioAsc(Collection<UUID> seccionIds);

    @Modifying
    @Query("""
            delete from SeccionHorario horario
            where horario.seccion.id in (
                select seccion.id
                from Seccion seccion
                where seccion.ofertaCurso.planificacion.id = :planificacionId
            )
            """)
    int deleteAllByPlanificacionId(@Param("planificacionId") UUID planificacionId);

    @Modifying
    @Query("""
            delete from SeccionHorario horario
            where horario.seccion.id in (
                select seccion.id
                from Seccion seccion
                where seccion.ofertaCurso.id in :ofertaCursoIds
            )
            """)
    int deleteAllByOfertaCursoIds(@Param("ofertaCursoIds") List<UUID> ofertaCursoIds);
}
