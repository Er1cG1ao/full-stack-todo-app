import { Injectable, inject, signal } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, switchMap, tap } from 'rxjs';
import { API_URL } from '../app.constants';

export interface AuthUser {
  username: string;
  recoveryCode?: string;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);
  private currentUser = signal<string | null>(null);

  constructor() {
    sessionStorage.removeItem('authenticatedUser');
    sessionStorage.removeItem('basicAuthToken');
  }

  get username(): string | null {
    return this.currentUser();
  }
  isUserLoggedIn(): boolean {
    return this.username !== null;
  }

  authenticate(username: string, password: string): Observable<AuthUser> {
    const body = new HttpParams().set('username', username).set('password', password);
    return this.http
      .post<void>(`${API_URL}/auth/login`, body)
      .pipe(switchMap(() => this.restoreSession()));
  }

  register(username: string, password: string): Observable<AuthUser> {
    return this.http.post<AuthUser>(`${API_URL}/auth/register`, { username, password });
  }
  recover(username: string, recoveryCode: string, newPassword: string): Observable<void> {
    return this.http.post<void>(`${API_URL}/auth/recover`, { username, recoveryCode, newPassword });
  }

  restoreSession(): Observable<AuthUser> {
    return this.http
      .get<AuthUser>(`${API_URL}/auth/me`)
      .pipe(tap((user) => this.currentUser.set(user.username)));
  }

  clearSession(): void {
    this.currentUser.set(null);
    sessionStorage.removeItem('authenticatedUser');
    sessionStorage.removeItem('basicAuthToken');
  }

  logout(): Observable<void> {
    return this.http.post<void>(`${API_URL}/auth/logout`, {}).pipe(tap(() => this.clearSession()));
  }
}
