import { Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { PlanningApiService } from '../../core/planning-api.service';
import { VistaPreviaImportacion } from '../../models/api.model';
import {
  LoadingProgressComponent,
  ProgressStage,
} from '../../shared/loading-progress/loading-progress';

@Component({
  selector: 'app-review-import',
  imports: [RouterLink, LoadingProgressComponent],
  templateUrl: './review-import.html',
  styleUrl: './review-import.scss',
})
export class ReviewImportPage {
  private readonly route = inject(ActivatedRoute);
  private readonly api = inject(PlanningApiService);
  private readonly router = inject(Router);
  readonly id = this.route.snapshot.paramMap.get('id')!;
  readonly planId = this.route.snapshot.queryParamMap.get('planificacion')!;
  readonly preview = signal<VistaPreviaImportacion | null>(null);
  readonly error = signal('');
  readonly confirming = signal(false);
  readonly valid = computed(() => this.preview()?.filas.filter((item) => item.valida) || []);
  readonly invalid = computed(() => this.preview()?.filas.filter((item) => !item.valida) || []);
  readonly confirmationStages: ProgressStage[] = [
    {
      afterSeconds: 0,
      label: 'Confirmando la importación',
      detail: 'Se inicia la generación de la oferta académica.',
    },
    {
      afterSeconds: 15,
      label: 'Calculando oferta por curso',
      detail: 'Se aplican la demanda y capacidad configuradas para cada curso.',
    },
    {
      afterSeconds: 75,
      label: 'Generando secciones propuestas',
      detail: 'Se crean las secciones y cupos que podrás editar después.',
    },
    {
      afterSeconds: 150,
      label: 'Consolidando la programación',
      detail: 'El proceso continúa. En documentos grandes puede tardar cerca de tres minutos.',
    },
  ];
  constructor() {
    this.api.obtenerImportacion(this.id).subscribe({
      next: (result) => this.preview.set(result),
      error: () => this.error.set('No se pudo recuperar la importación.'),
    });
  }
  confirm() {
    this.error.set('');
    this.confirming.set(true);
    this.api.confirmarImportacion(this.id).subscribe({
      next: () => {
        this.confirming.set(false);
        this.router.navigate(['/planificaciones', this.planId, 'programacion']);
      },
      error: (error) => {
        this.error.set(error.error?.message || 'No se pudo confirmar.');
        this.confirming.set(false);
      },
    });
  }
}
