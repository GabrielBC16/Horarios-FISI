import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { AuthService } from '../../core/auth.service';
import { PlanningApiService } from '../../core/planning-api.service';
import { Horario, Oferta, Seccion } from '../../models/api.model';

@Component({
  selector: 'app-programming',
  imports: [FormsModule, RouterLink],
  templateUrl: './programming.html',
  styleUrl: './programming.scss',
})
export class ProgrammingPage {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly api = inject(PlanningApiService);
  private readonly auth = inject(AuthService);

  readonly planId =
    this.route.snapshot.paramMap.get('id') ||
    this.route.snapshot.queryParamMap.get('planificacion') ||
    '';
  readonly ofertas = signal<Oferta[]>([]);
  readonly error = signal('');
  readonly saved = signal(true);
  readonly saving = signal(false);
  private removed: Seccion[] = [];
  private removedSchedules: { sectionId: string; item: Horario }[] = [];

  constructor() {
    if (this.planId) {
      this.load();
    } else {
      this.error.set('No se encontró la planificación.');
    }
  }

  load() {
    this.api.programacion(this.planId).subscribe({
      next: (response) =>
        this.ofertas.set(
          this.shareSections(response).sort(
            (a, b) => a.curso.ciclo - b.curso.ciclo || a.curso.codigo.localeCompare(b.curso.codigo),
          ),
        ),      error: (error: unknown) =>
        this.error.set(this.errorMessage(error, 'No se pudo cargar la programación.')),
    });
  }

  dirty() {
    this.saved.set(false);
  }

  addSection(oferta: Oferta) {
    const section: Seccion = {
      codigo: `S${String(oferta.secciones.length + 1).padStart(2, '0')}`,
      profesor: null,
      capacidad: oferta.capacidadAplicada,
      cuposPlan2018: 0,
      cuposPlan2023: 0,
      estado: 'ACTIVA',
      horarios: [],
      local: true,
    };
    this.ofertas.update((offers) =>
      offers.map((item) =>
        item.id === oferta.id ? { ...item, secciones: [...item.secciones, section] } : item,
      ),
    );
    this.dirty();
  }

  removeSection(oferta: Oferta, index: number) {
    const section = oferta.secciones[index];
    if (!section) {
      return;
    }
    if (section.id && !this.removed.some((removed) => removed.id === section.id)) {
      this.removed.push(section);
    }
    this.ofertas.update((offers) =>
      offers.map((item) => ({
        ...item,
        secciones: item.secciones.filter((candidate) =>
          section.id ? candidate.id !== section.id : candidate !== section,
        ),
      })),
    );
    this.dirty();
  }

  addSchedule(section: Seccion) {
    section.horarios.push({
      dia: 1,
      horaInicio: '08:00',
      horaFin: '10:00',
      local: true,
    });
    this.dirty();
  }

  removeSchedule(section: Seccion, index: number) {
    const [item] = section.horarios.splice(index, 1);
    if (item?.id && section.id) {
      this.removedSchedules.push({ sectionId: section.id, item });
    }
    this.dirty();
  }

  async save() {
    const usuarioId = this.auth.userId();
    if (!usuarioId) {
      this.error.set('Falta el UUID del usuario activo.');
      return;
    }

    this.saving.set(true);
    this.error.set('');
    try {
      const offers = this.ofertas();
      for (const offer of offers) {
        const updated = await firstValueFrom(
          this.api.actualizarOferta(this.planId, offer, usuarioId),
        );
        offer.version = updated.version;
      }

      for (const section of this.uniqueSections(offers)) {
        if (section.local) {
          const created = await firstValueFrom(
            this.api.crearSeccion(
              this.planId,
              this.canonicalOfferId(offers, section),
              section,
              usuarioId,
            ),
          );
          section.id = created.id;
          section.version = created.version;
          section.local = false;
        } else {
          const updated = await firstValueFrom(
            this.api.actualizarSeccion(this.planId, section, usuarioId),
          );
          section.version = updated.version;
        }

        for (const schedule of section.horarios) {
          if (schedule.local) {
            const created = await firstValueFrom(
              this.api.crearHorario(this.planId, section.id!, schedule, usuarioId),
            );
            schedule.id = created.id;
            schedule.version = created.version;
            schedule.local = false;
          } else if (schedule.id) {
            const updated = await firstValueFrom(
              this.api.actualizarHorario(this.planId, section.id!, schedule, usuarioId),
            );
            schedule.version = updated.version;
          }
        }
      }

      for (const { sectionId, item } of this.removedSchedules) {
        await firstValueFrom(this.api.eliminarHorario(this.planId, sectionId, item, usuarioId));
      }
      this.removedSchedules = [];

      for (const section of this.removed) {
        await firstValueFrom(this.api.retirarSeccion(this.planId, section, usuarioId));
      }
      this.removed = [];
      this.saved.set(true);
      this.load();
    } catch (error: unknown) {
      this.error.set(
        this.errorMessage(
          error,
          'No se pudieron guardar todos los cambios. Recarga y vuelve a intentarlo.',
        ),
      );
    } finally {
      this.saving.set(false);
    }
  }

  goExport() {
    this.router.navigate(['/planificaciones', this.planId, 'exportar']);
  }

  private shareSections(offers: Oferta[]): Oferta[] {
    const sectionsById = new Map<string, Seccion>();
    return offers.map((offer) => ({
      ...offer,
      secciones: offer.secciones.map((section) => {
        if (!section.id) {
          return section;
        }
        const existing = sectionsById.get(section.id);
        if (existing) {
          return existing;
        }
        sectionsById.set(section.id, section);
        return section;
      }),
    }));
  }

  private uniqueSections(offers: Oferta[]): Seccion[] {
    const seenIds = new Set<string>();
    const seenLocalSections = new Set<Seccion>();
    const sections: Seccion[] = [];
    for (const offer of offers) {
      for (const section of offer.secciones) {
        if (section.id) {
          if (seenIds.has(section.id)) {
            continue;
          }
          seenIds.add(section.id);
        } else {
          if (seenLocalSections.has(section)) {
            continue;
          }
          seenLocalSections.add(section);
        }
        sections.push(section);
      }
    }
    return sections;
  }

  private canonicalOfferId(offers: Oferta[], section: Seccion): string {
    const sectionOwner = offers.find((offer) => offer.secciones.includes(section));
    if (!sectionOwner) {
      throw new Error('No se encontró la oferta asociada a la sección nueva.');
    }
    return sectionOwner.id;
  }

  private errorMessage(error: unknown, fallback: string): string {
    if (
      typeof error === 'object' &&
      error !== null &&
      'error' in error &&
      typeof error.error === 'object' &&
      error.error !== null &&
      'message' in error.error &&
      typeof error.error.message === 'string'
    ) {
      return error.error.message;
    }
    return fallback;
  }
}


