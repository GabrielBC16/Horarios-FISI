package fisi.edu.pe.SPA.domain.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "convalidacion", schema = "academico")
public class Convalidacion extends UuidEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "curso_2023_id", nullable = false)
    private Curso curso2023;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "curso_2018_id", nullable = false)
    private Curso cursoConvalidado2018;

    public Curso getCurso2023() {
        return curso2023;
    }

    public void setCurso2023(Curso curso2023) {
        this.curso2023 = curso2023;
    }

    public Curso getCursoConvalidado2018() {
        return cursoConvalidado2018;
    }

    public void setCursoConvalidado2018(Curso cursoConvalidado2018) {
        this.cursoConvalidado2018 = cursoConvalidado2018;
    }
}
