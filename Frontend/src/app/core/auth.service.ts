import { HttpClient } from '@angular/common/http';
import { inject, Injectable, signal } from '@angular/core';
import { map } from 'rxjs';
import { CrearUsuarioRequest, LoginRequest, Usuario } from '../models/api.model';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly base = 'http://localhost:8080/api/auth';
  private readonly http = inject(HttpClient);
  readonly user = signal(localStorage.getItem('horarios_user') || '');
  readonly userId = signal(localStorage.getItem('horarios_user_id') || '');

  login(email: string, clave: string) {
    const request: LoginRequest = { email, clave };
    return this.http.post<Usuario>(`${this.base}/login`, request).pipe(
      map((usuario) => {
        localStorage.setItem('horarios_user', usuario.nombre);
        localStorage.setItem('horarios_user_id', usuario.usuarioId);
        localStorage.setItem('horarios_user_identifier', usuario.identificador);
        localStorage.setItem('horarios_user_role', usuario.rol);
        this.user.set(usuario.nombre);
        this.userId.set(usuario.usuarioId);
        return usuario;
      }),
    );
  }

  crearUsuario(request: CrearUsuarioRequest) {
    return this.http
      .post(`${this.base}/crearUsuario`, request, { responseType: 'text' })
      .pipe(map((response) => response.trim().toLowerCase() === 'true'));
  }

  logout() {
    localStorage.removeItem('horarios_user');
    localStorage.removeItem('horarios_user_id');
    localStorage.removeItem('horarios_user_identifier');
    localStorage.removeItem('horarios_user_role');
    this.user.set('');
    this.userId.set('');
  }
}
