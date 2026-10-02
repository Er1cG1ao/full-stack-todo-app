import { TestBed, ComponentFixture } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';
import { Login } from './login';
import { AuthService } from '../service/auth.service';
describe('Login and account recovery', () => {
  let fixture: ComponentFixture<Login>;
  let component: Login;
  const auth = { register: vi.fn(), authenticate: vi.fn(), recover: vi.fn() };
  beforeEach(async () => {
    vi.clearAllMocks();
    auth.register.mockReturnValue(of({ username: 'alice', recoveryCode: 'DAYLIGHT-test-key' }));
    auth.authenticate.mockReturnValue(of({ username: 'alice' }));
    auth.recover.mockReturnValue(of(null));
    await TestBed.configureTestingModule({
      imports: [Login],
      providers: [provideRouter([]), { provide: AuthService, useValue: auth }],
    }).compileComponents();
    vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    fixture = TestBed.createComponent(Login);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });
  afterEach(() => fixture.destroy());
  it('signs in without storing the password', () => {
    component.username = ' alice ';
    component.password = 'password-123';
    component.handleLogin();
    expect(auth.authenticate).toHaveBeenCalledWith('alice', 'password-123');
    expect(component.password).toBe('');
    expect(TestBed.inject(Router).navigate).toHaveBeenCalledWith(['/app']);
  });
  it('shows a recovery key after signup before entering the workspace', () => {
    component.registering = true;
    component.username = 'alice';
    component.password = component.confirmPassword = 'password-123';
    component.handleLogin();
    fixture.detectChanges();
    expect(component.recoveryCode()).toBe('DAYLIGHT-test-key');
    expect(fixture.nativeElement.textContent).toContain('Keep this safe.');
    expect(TestBed.inject(Router).navigate).not.toHaveBeenCalled();
    expect(fixture.nativeElement.querySelector('.recovery-ack')).not.toBeNull();
    component.continue();
    expect(component.recoveryCode()).toBe('');
  });
  it('requires matching signup and reset passwords', () => {
    component.recoverMode();
    component.password = 'one-password';
    component.confirmPassword = 'different';
    component.handleLogin();
    expect(auth.recover).not.toHaveBeenCalled();
    expect(component.errorMessage()).toContain('do not match');
  });
  it('uses the saved recovery key and signs in with the new password', () => {
    component.recoverMode();
    component.username = 'alice';
    component.recoveryInput = ' DAYLIGHT-test-key ';
    component.password = component.confirmPassword = 'new-password';
    component.handleLogin();
    expect(auth.recover).toHaveBeenCalledWith('alice', 'DAYLIGHT-test-key', 'new-password');
    expect(auth.authenticate).toHaveBeenCalledWith('alice', 'new-password');
    expect(component.recoveryInput).toBe('');
  });
  it('surfaces login errors and clears the busy state', () => {
    auth.authenticate.mockReturnValue(
      throwError(() => ({ status: 429, error: { message: 'Too many attempts' } })),
    );
    component.handleLogin();
    expect(component.errorMessage()).toBe('Too many attempts');
    expect(component.busy()).toBe(false);
  });
});
