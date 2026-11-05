import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { AuthService } from '../../core/auth.service';
import { PlanningApiService } from '../../core/planning-api.service';
import { Horario, Oferta, Seccion } from '../../models/api.model';
import {
  LoadingProgressComponent,
  ProgressStage,
} from '../../shared/loading-progress/loading-progress';

interface GrupoOferta {
  id: string;
  curso: Oferta['curso'];
  ofertaPrincipal: Oferta;
  ofertas: Oferta[];
  secciones: Seccion[];
  planesLabel: string;
}

@Component({
  selector: 'app-programming',
  imports: [FormsModule, RouterLink, LoadingProgressComponent],
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
  readonly lastSavedAt = signal<Date | null>(null);
  readonly searchQuery = signal('');
  readonly expandedCycles = signal<Set<number>>(new Set());
  readonly lastSavedLabel = computed(() => {
    const date = this.lastSavedAt();
    return date
      ? new Intl.DateTimeFormat('es-PE', {
          hour: '2-digit',
          minute: '2-digit',
          second: '2-digit',
        }).format(date)
      : null;
  });

  readonly confirmationStages: ProgressStage[] = [
    {
      afterSeconds: 0,
      label: 'Confirmando los cambios',
      detail: 'Se inicia el guardado de cambios.',
    },
    {
      afterSeconds: 3,
      label: 'Validando la programación y los cupos',
      detail: 'Validando...',
    },
    {
      afterSeconds: 5,
      label: 'Guardando los cambios',
      detail: 'Guardando...',
    },
  ];

  readonly grupos = computed(() => {
    const ofertasPorGrupo = new Map<string, Oferta[]>();
    for (const oferta of this.ofertas()) {
      const grupoId = oferta.grupoCursoId || oferta.curso.id;
      const ofertas = ofertasPorGrupo.get(grupoId) ?? [];
      ofertas.push(oferta);
      ofertasPorGrupo.set(grupoId, ofertas);
    }

    return Array.from(ofertasPorGrupo, ([id, ofertas]): GrupoOferta => {
      ofertas.sort((a, b) => b.curso.planAnio - a.curso.planAnio);
      const ofertaPrincipal = ofertas.find((oferta) => oferta.curso.id === id) ?? ofertas[0];
      const seccionesPorId = new Map<string, Seccion>();
      const seccionesLocales = new Set<Seccion>();
      for (const oferta of ofertas) {
        for (const seccion of oferta.secciones) {
          if (seccion.id) {
            seccionesPorId.set(seccion.id, seccion);
          } else {
            seccionesLocales.add(seccion);
          }
        }
      }

      return {
        id,
        curso: ofertaPrincipal.curso,
        ofertaPrincipal,
        ofertas,
        secciones: [...seccionesPorId.values(), ...seccionesLocales],
        planesLabel: [...new Set(ofertas.map((oferta) => oferta.curso.planAnio))]
          .sort((a, b) => b - a)
          .join(' y '),
      };
    });
  });
  readonly ciclos = computed(() => {
    const query = this.normalize(this.searchQuery());
    const matchingGroups = this.grupos().filter(
      (group) =>
        !query ||
        group.ofertas.some((oferta) =>
          this.normalize(`${oferta.curso.codigo} ${oferta.curso.nombre}`).includes(query),
        ),
    );
    return Array.from({ length: 10 }, (_, index) => {
      const ciclo = index + 1;
      return { ciclo, grupos: matchingGroups.filter((group) => group.curso.ciclo === ciclo) };
    });
  });
  readonly cursosVisibles = computed(() =>
    this.ciclos().reduce((total, ciclo) => total + ciclo.grupos.length, 0),
  );
  private removed: Seccion[] = [];
  private removedSchedules: { sectionId: string; item: Horario }[] = [];
  private readonly modifiedOfferIds = new Set<string>();
  private readonly modifiedSections = new Set<Seccion>();
  private readonly modifiedSchedules = new Set<Horario>();

  constructor() {
    this.restoreLastSavedAt();
    if (this.planId) {
      this.load();
    } else {
      this.error.set('No se encontró la planificación.');
    }
  }

  load() {
    this.api.programacion(this.planId).subscribe({
      next: (response) => {
        this.ofertas.set(
          this.shareSections(response).sort(
            (a, b) => a.curso.ciclo - b.curso.ciclo || a.curso.codigo.localeCompare(b.curso.codigo),
          ),
        );
        this.resetChangeTracking();
      },
      error: (error: unknown) =>
        this.error.set(this.errorMessage(error, 'No se pudo cargar la programación.')),
    });
  }

  setSearch(query: string) {
    this.searchQuery.set(query);
  }

  toggleCycle(cycle: number) {
    this.expandedCycles.update((current) => {
      const next = new Set(current);
      next.has(cycle) ? next.delete(cycle) : next.add(cycle);
      return next;
    });
  }

  isCycleOpen(cycle: { ciclo: number; grupos: GrupoOferta[] }) {
    return this.searchQuery().trim()
      ? cycle.grupos.length > 0
      : this.expandedCycles().has(cycle.ciclo);
  }

  dirtyOffer(oferta: Oferta) {
    this.modifiedOfferIds.add(oferta.id);
    this.dirty();
  }

  dirtySection(section: Seccion) {
    if (!section.local) this.modifiedSections.add(section);
    this.dirty();
  }

  dirtySchedule(schedule: Horario) {
    if (!schedule.local) this.modifiedSchedules.add(schedule);
    this.dirty();
  }

  private dirty() {
    this.saved.set(false);
  }

  addSection(grupo: GrupoOferta) {
    const section: Seccion = {
      codigo: `S${String(grupo.secciones.length + 1).padStart(2, '0')}`,
      profesor: null,
      capacidad: grupo.ofertaPrincipal.capacidadAplicada,
      cuposPlan2018: 0,
      cuposPlan2023: 0,
      estado: 'ACTIVA',
      horarios: [],
      local: true,
    };
    this.ofertas.update((offers) =>
      offers.map((item) =>
        item.id === grupo.ofertaPrincipal.id
          ? { ...item, secciones: [...item.secciones, section] }
          : item,
      ),
    );
    this.dirty();
  }

  removeSection(grupo: GrupoOferta, index: number) {
    const section = grupo.secciones[index];
    if (!section) {
      return;
    }
    if (section.id && !this.removed.some((removed) => removed.id === section.id)) {
      this.removed.push(section);
      this.removedSchedules = this.removedSchedules.filter(
        (entry) => entry.sectionId !== section.id,
      );
    }
    this.modifiedSections.delete(section);
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
    if (item) this.modifiedSchedules.delete(item);
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
      const offersToSave = offers.filter((offer) => this.modifiedOfferIds.has(offer.id));
      for (const offer of offersToSave) {
        const updated = await firstValueFrom(
          this.api.actualizarOferta(this.planId, offer, usuarioId),
        );
        offer.version = updated.version;
        this.modifiedOfferIds.delete(offer.id);
      }

      for (const section of this.uniqueSections(offers)) {
        const isNew = Boolean(section.local);
        const isModified = this.modifiedSections.has(section);
        if (isNew) {
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
        } else if (isModified) {
          const updated = await firstValueFrom(
            this.api.actualizarSeccion(this.planId, section, usuarioId),
          );
          section.version = updated.version;
          this.modifiedSections.delete(section);
        }

        for (const schedule of section.horarios) {
          if (schedule.local) {
            const created = await firstValueFrom(
              this.api.crearHorario(this.planId, section.id!, schedule, usuarioId),
            );
            schedule.id = created.id;
            schedule.version = created.version;
            schedule.local = false;
          } else if (this.modifiedSchedules.has(schedule)) {
            const updated = await firstValueFrom(
              this.api.actualizarHorario(this.planId, section.id!, schedule, usuarioId),
            );
            schedule.version = updated.version;
            this.modifiedSchedules.delete(schedule);
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
      this.modifiedOfferIds.clear();
      this.modifiedSections.clear();
      this.modifiedSchedules.clear();
      this.saved.set(true);
      this.recordLastSavedAt();
      this.ofertas.update((offers) => [...offers]);
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

  private resetChangeTracking() {
    this.modifiedOfferIds.clear();
    this.modifiedSections.clear();
    this.modifiedSchedules.clear();
    this.removed = [];
    this.removedSchedules = [];
    this.saved.set(true);
  }

  private recordLastSavedAt() {
    const date = new Date();
    this.lastSavedAt.set(date);
    localStorage.setItem(this.lastSavedStorageKey(), date.toISOString());
  }

  private restoreLastSavedAt() {
    const value = localStorage.getItem(this.lastSavedStorageKey());
    if (!value) {
      return;
    }
    const date = new Date(value);
    if (!Number.isNaN(date.getTime())) {
      this.lastSavedAt.set(date);
    }
  }

  private lastSavedStorageKey() {
    return `horarios_planificacion_${this.planId}_ultimo_guardado`;
  }

  private normalize(value: string) {
    return value
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .toLowerCase()
      .trim();
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
