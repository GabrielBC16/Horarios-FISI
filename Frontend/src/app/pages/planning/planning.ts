import { Component, computed, inject, signal } from '@angular/core';
import * as XLSX from 'xlsx';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { PlanningStateService } from '../../core/planning-state.service';
import { CourseDemand, Section } from '../../models/planning.model';

@Component({ selector: 'app-planning', imports: [FormsModule, RouterLink], templateUrl: './planning.html', styleUrl: './planning.scss' })
export class PlanningPage {
  readonly state = inject(PlanningStateService);
  private readonly router = inject(Router);
  step = signal(1);
  fileName = signal('');
  period = '2026-I';
  selectedSection = signal<Section | null>(null);
  readonly planning = this.state.planning;
  readonly validRows = computed(() => this.state.validDemand());
  readonly invalidRows = computed(() => this.state.invalidDemand());

  begin() { this.state.start(this.period); this.step.set(2); }
  chooseFile(event: Event) { const file = (event.target as HTMLInputElement).files?.[0]; if (file) this.fileName.set(file.name); }
  importPdf() { if (this.fileName()) this.step.set(3); }
  saveDemand(row: CourseDemand) { this.state.updateDemand({ ...row, students: Number(row.students), capacity: Number(row.capacity), proposedSections: Number(row.proposedSections) }); }
  generateSections() { this.step.set(4); }
  editSection(section: Section) { this.selectedSection.set({ ...section }); }
  saveSection() { const section = this.selectedSection(); if (section) { this.state.updateSection(section); this.selectedSection.set(null); } }
  validate() { this.state.validateSections(); this.step.set(5); }
  export() { this.state.markExported(); const csv = ['Curso,Sección,Alumnos,Capacidad,Profesor,Horario,Aula', ...this.planning().sections.map(s => `${s.courseName},${s.code},${s.students},${s.capacity},${s.professor},${s.day} ${s.time},${s.room}`)].join('\n'); const url = URL.createObjectURL(new Blob([csv], {type:'text/csv'})); const a=document.createElement('a'); a.href=url; a.download=`programacion-${this.planning().period}.csv`; a.click(); URL.revokeObjectURL(url); }
  back() { this.router.navigateByUrl('/dashboard'); }
}

