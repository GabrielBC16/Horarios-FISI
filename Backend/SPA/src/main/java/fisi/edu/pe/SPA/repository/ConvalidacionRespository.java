package fisi.edu.pe.SPA.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import fisi.edu.pe.SPA.domain.entity.Convalidacion;

public interface ConvalidacionRespository extends JpaRepository<Convalidacion, UUID> {

    List<Convalidacion> findByCurso2023_Id(UUID curso2023Id);

    List<Convalidacion> findByCursoConvalidado2018_Id(UUID curso2018Id);

    boolean existsByCursoConvalidado2018_Id(UUID curso2018Id);

    boolean existsByCursoConvalidado2018_IdAndIdNot(UUID curso2018Id, UUID id);

    @Query("""
            select convalidacion from Convalidacion convalidacion
            join fetch convalidacion.cursoConvalidado2018 curso2018
            join fetch convalidacion.curso2023 curso2023
            order by curso2018.ciclo, curso2018.codigo
            """)
    List<Convalidacion> listarTodas();

    @Query("""
            select convalidacion from Convalidacion convalidacion
            join fetch convalidacion.cursoConvalidado2018 curso2018
            join fetch convalidacion.curso2023 curso2023
            where curso2018.ciclo = :ciclo
            order by curso2018.codigo
            """)
    List<Convalidacion> listarPorCiclo2018(@Param("ciclo") Short ciclo);

}
