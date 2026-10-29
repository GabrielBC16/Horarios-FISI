package fisi.edu.pe.SPA.domain.entity;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import fisi.edu.pe.SPA.domain.enums.TipoArea;

@Entity
@DiscriminatorValue("ESCUELA")
public class Escuela extends Area {
    public Escuela() {}

    @Override
    public TipoArea getTipo() { return TipoArea.ESCUELA; }
}
