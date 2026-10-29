package fisi.edu.pe.SPA.persistence;

import fisi.edu.pe.SPA.domain.entity.*;
import fisi.edu.pe.SPA.repository.*;
import java.util.List;
import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;

import static org.junit.jupiter.api.Assertions.*;

/** Valida Hibernate y las consultas de repositorio sin conectarse a Supabase. */
class JpaModelTests {
    private static final List<Class<?>> ENTITIES = List.of(
        Area.class, Escuela.class, Dac.class, Vicedecanato.class, OtraArea.class,
        Usuario.class, PlanCurricular.class, Curso.class, PeriodoAcademico.class,
        Planificacion.class, Importacion.class, OfertaCurso.class, Convalidacion.class, Seccion.class,
        SeccionHorario.class, IntercambioExcel.class, CambioPlanificacion.class
    );

    private static final List<Class<?>> REPOSITORIES = List.of(
        AreaRepository.class, EscuelaRepository.class, DacRepository.class,
        VicedecanatoRepository.class, OtraAreaRepository.class,
        UsuarioRepository.class, PlanCurricularRepository.class, CursoRepository.class,
        PeriodoAcademicoRepository.class, PlanificacionRepository.class,
        ImportacionRepository.class, OfertaCursoRepository.class,
        ConvalidacionRespository.class, SeccionRepository.class,
        SeccionHorarioRepository.class, IntercambioExcelRepository.class,
        CambioPlanificacionRepository.class
    );

    @Test
    void buildsHibernateMappingsAndAllRepositoryQueriesWithoutDatabase() {
        StandardServiceRegistry registry = new StandardServiceRegistryBuilder()
            .applySetting("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect")
            .applySetting("hibernate.boot.allow_jdbc_metadata_access", false)
            .applySetting("hibernate.connection.provider_class",
                "org.hibernate.engine.jdbc.connections.internal.UserSuppliedConnectionProviderImpl")
            .applySetting("hibernate.hbm2ddl.auto", "none")
            .build();

        try {
            MetadataSources sources = new MetadataSources(registry);
            ENTITIES.forEach(sources::addAnnotatedClass);
            try (SessionFactory factory = sources.buildMetadata().buildSessionFactory();
                 var entityManager = factory.createEntityManager()) {
                assertEquals(ENTITIES.size(), factory.getMetamodel().getEntities().size());
                JpaRepositoryFactory repositoryFactory = new JpaRepositoryFactory(entityManager);
                for (Class<?> repository : REPOSITORIES) {
                    assertNotNull(repositoryFactory.getRepository(repository), repository.getSimpleName());
                }
            }
        } finally {
            StandardServiceRegistryBuilder.destroy(registry);
        }
    }

    @Test
    void buildsConvalidacionRepositoryQueriesWithoutDatabase() {
        StandardServiceRegistry registry = new StandardServiceRegistryBuilder()
            .applySetting("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect")
            .applySetting("hibernate.boot.allow_jdbc_metadata_access", false)
            .applySetting("hibernate.connection.provider_class",
                "org.hibernate.engine.jdbc.connections.internal.UserSuppliedConnectionProviderImpl")
            .applySetting("hibernate.hbm2ddl.auto", "none")
            .build();

        try {
            MetadataSources sources = new MetadataSources(registry);
            ENTITIES.forEach(sources::addAnnotatedClass);
            try (SessionFactory factory = sources.buildMetadata().buildSessionFactory();
                 var entityManager = factory.createEntityManager()) {
                JpaRepositoryFactory repositoryFactory = new JpaRepositoryFactory(entityManager);
                assertNotNull(repositoryFactory.getRepository(CursoRepository.class));
                assertNotNull(repositoryFactory.getRepository(ConvalidacionRespository.class));
            }
        } finally {
            StandardServiceRegistryBuilder.destroy(registry);
        }
    }
}
