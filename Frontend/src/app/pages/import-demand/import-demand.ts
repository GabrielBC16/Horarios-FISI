import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth.service';
import { PlanningApiService } from '../../core/planning-api.service';
import { LoadingProgressComponent, ProgressStage } from '../../shared/loading-progress/loading-progress';

@Component({
  selector: 'app-import-demand',
  imports: [RouterLink, LoadingProgressComponent],
  templateUrl: './import-demand.html',
  styleUrl: './import-demand.scss',
})
export class ImportDemandPage {
  private readonly route = inject(ActivatedRoute);
  private readonly api = inject(PlanningApiService);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  readonly planId = this.route.snapshot.paramMap.get('id')!;
  readonly file = signal<File | null>(null);
  readonly error = signal('');
  readonly loading = signal(false);
  readonly importStages: ProgressStage[] = [
    { afterSeconds: 0, label: 'Cargando el archivo', detail: 'El PDF se está enviando de forma segura al servidor.' },
    { afterSeconds: 10, label: 'Leyendo el documento', detail: 'Se están detectando las tablas y filas de demanda.' },
    { afterSeconds: 45, label: 'Identificando cursos y planes', detail: 'Se contrastan códigos, planes curriculares y cantidades.' },
    { afterSeconds: 90, label: 'Preparando la vista previa', detail: 'La extracción continúa; este documento puede tardar unos minutos.' },
  ];

  select(event: Event) { this.file.set((event.target as HTMLInputElement).files?.[0] || null); }

  upload() {
    if (!this.file() || !this.auth.userId()) return;
    this.error.set('');
    this.loading.set(true);
    this.api.importarDemanda(this.planId, this.auth.userId(), this.file()!).subscribe({
      next: (result) => {
        this.loading.set(false);
        this.router.navigate(['/importaciones', result.importacionId, 'revision'], {
          queryParams: { planificacion: this.planId },
        });
      },
      error: (error) => {
        this.error.set(error.error?.message || 'No se pudo procesar el PDF.');
        this.loading.set(false);
      },
    });
  }
}
