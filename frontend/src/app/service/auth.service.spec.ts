import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { Router } from '@angular/router';
import { vi } from 'vitest';
import { AuthService } from './auth.service';
import { sessionInterceptor } from './http/session-interceptor';
import { API_URL } from '../app.constants';

describe('Server authentication', () => {
  let auth: AuthService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(withInterceptors([sessionInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    auth = TestBed.inject(AuthService);
    http = TestBed.inject(HttpTestingController);
    vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
  });

  afterEach(() => http.verify());

  function csrf(token = 'fresh-token'): void {
    const request = http.expectOne(`${API_URL}/auth/csrf`);
    expect(request.request.withCredentials).toBe(true);
    request.flush({ headerName: 'X-CSRF-TOKEN', token });
  }

  it('uses server login, CSRF and session cookies without storing credentials', () => {
    auth.authenticate('new_user', 'secure-password').subscribe();
    csrf();
    const login = http.expectOne(`${API_URL}/auth/login`);
    expect(login.request.withCredentials).toBe(true);
    expect(login.request.headers.get('X-CSRF-TOKEN')).toBe('fresh-token');
    expect(login.request.body.get('username')).toBe('new_user');
    expect(auth.isUserLoggedIn()).toBe(false);
    login.flush(null, { status: 204, statusText: 'No Content' });
    http.expectOne(`${API_URL}/auth/me`).flush({ username: 'new_user' });
    expect(auth.username).toBe('new_user');
    expect(sessionStorage.getItem('basicAuthToken')).toBeNull();
    expect(sessionStorage.getItem('authenticatedUser')).toBeNull();
  });

  it('does not authenticate after an incorrect password', () => {
    let status = 0;
    auth
      .authenticate('new_user', 'wrong-password')
      .subscribe({ error: (error) => (status = error.status) });
    csrf();
    http
      .expectOne(`${API_URL}/auth/login`)
      .flush(
        { message: 'Invalid username or password' },
        { status: 401, statusText: 'Unauthorized' },
      );
    expect(status).toBe(401);
    expect(auth.isUserLoggedIn()).toBe(false);
    http.expectNone(`${API_URL}/auth/me`);
  });

  it('restores a server session and invalidates it on logout with a fresh CSRF token', () => {
    auth.restoreSession().subscribe();
    http.expectOne(`${API_URL}/auth/me`).flush({ username: 'new_user' });
    auth.logout().subscribe();
    csrf('rotated-token');
    const logout = http.expectOne(`${API_URL}/auth/logout`);
    expect(logout.request.headers.get('X-CSRF-TOKEN')).toBe('rotated-token');
    logout.flush(null, { status: 204, statusText: 'No Content' });
    expect(auth.isUserLoggedIn()).toBe(false);
  });

  it('clears expired sessions on a protected request', () => {
    auth.restoreSession().subscribe();
    http.expectOne(`${API_URL}/auth/me`).flush({ username: 'new_user' });
    auth.restoreSession().subscribe({ error: () => {} });
    http.expectOne(`${API_URL}/auth/me`).flush({}, { status: 401, statusText: 'Unauthorized' });
    expect(auth.isUserLoggedIn()).toBe(false);
  });

  it('registers new credentials with CSRF and surfaces username conflicts', () => {
    let status = 0;
    auth
      .register('new_user', 'secure-password')
      .subscribe({ error: (error) => (status = error.status) });
    csrf();
    const registration = http.expectOne(`${API_URL}/auth/register`);
    expect(registration.request.body).toEqual({
      username: 'new_user',
      password: 'secure-password',
    });
    registration.flush(
      { message: 'Username is already taken' },
      { status: 409, statusText: 'Conflict' },
    );
    expect(status).toBe(409);
    expect(auth.isUserLoggedIn()).toBe(false);
  });

  it('sends fresh CSRF, cookies and the browser calendar zone on task writes', () => {
    TestBed.inject(HttpClient).post('/api/tasks', { description: 'Calendar task' }).subscribe();
    csrf('calendar-token');
    const request = http.expectOne('/api/tasks');
    expect(request.request.withCredentials).toBe(true);
    expect(request.request.headers.get('X-CSRF-TOKEN')).toBe('calendar-token');
    expect(request.request.headers.get('X-Time-Zone')).toBe(
      Intl.DateTimeFormat().resolvedOptions().timeZone,
    );
    request.flush({});
  });

  it('never attaches credentials or CSRF tokens to another server', () => {
    TestBed.inject(HttpClient).post('https://other.example/action', {}).subscribe();
    const external = http.expectOne('https://other.example/action');
    expect(external.request.withCredentials).toBe(false);
    expect(external.request.headers.has('X-CSRF-TOKEN')).toBe(false);
    expect(external.request.headers.has('X-Time-Zone')).toBe(false);
    http.expectNone(`${API_URL}/auth/csrf`);
    external.flush({});
  });
});
