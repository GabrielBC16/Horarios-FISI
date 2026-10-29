import { Injectable, signal } from '@angular/core';
@Injectable({ providedIn: 'root' })
export class AuthService {
  readonly user = signal(localStorage.getItem('horarios_user') || 'Usuario de desarrollo');
  readonly userId = signal(localStorage.getItem('horarios_user_id') || '');
  login(email: string, userId: string) {
    localStorage.setItem('horarios_user', email);
    localStorage.setItem('horarios_user_id', userId);
    this.user.set(email);
    this.userId.set(userId);
  }
  logout() {
    localStorage.removeItem('horarios_user');
    localStorage.removeItem('horarios_user_id');
    this.user.set('Usuario de desarrollo');
    this.userId.set('');
  }
}
