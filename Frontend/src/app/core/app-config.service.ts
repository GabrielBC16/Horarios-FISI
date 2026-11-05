import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { firstValueFrom } from 'rxjs';

interface AppConfig {
  apiBaseUrl: string;
}

@Injectable({ providedIn: 'root' })
export class AppConfigService {
  private readonly http = inject(HttpClient);
  private config?: AppConfig;

  get apiBaseUrl(): string {
    if (!this.config) {
      throw new Error('La configuración de la API todavía no se ha cargado.');
    }

    return this.config.apiBaseUrl;
  }

  async load(): Promise<void> {
    const config = await firstValueFrom(this.http.get<Partial<AppConfig>>('/config.json'));
    const apiBaseUrl = config.apiBaseUrl?.trim().replace(/\/+$/, '');

    if (!apiBaseUrl) {
      throw new Error('La propiedad apiBaseUrl debe definirse en public/config.json.');
    }

    this.config = { apiBaseUrl };
  }
}
