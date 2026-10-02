import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { Icon } from '../ui/icon';
import { finalize, switchMap, tap } from 'rxjs';
import { AuthService } from '../service/auth.service';

@Component({
  selector: 'app-login',
  imports: [FormsModule, Icon],
  templateUrl: './login.html',
  styleUrl: './login.css',
})
export class Login {
  username = '';
  password = '';
  confirmPassword = '';
  registering = false;
  recovering = false;
  recoveryInput = '';
  recoveryCode = signal('');
  recoverySaved = false;
  busy = signal(false);
  errorMessage = signal('');
  passwordChanged = inject(ActivatedRoute).snapshot.queryParamMap.has('passwordChanged');
  private router = inject(Router);
  private auth = inject(AuthService);

  handleLogin(): void {
    if (this.busy()) return;
    this.errorMessage.set('');
    if ((this.registering || this.recovering) && this.password !== this.confirmPassword) {
      this.errorMessage.set('Passwords do not match.');
      return;
    }
    this.busy.set(true);
    const username = this.username.trim();
    const action = this.recovering
      ? this.auth
          .recover(username, this.recoveryInput.trim(), this.password)
          .pipe(switchMap(() => this.auth.authenticate(username, this.password)))
      : this.registering
        ? this.auth.register(username, this.password).pipe(
            tap((user) => this.recoveryCode.set(user.recoveryCode ?? '')),
            switchMap(() => this.auth.authenticate(username, this.password)),
          )
        : this.auth.authenticate(username, this.password);
    action.pipe(finalize(() => this.busy.set(false))).subscribe({
      next: (user) => {
        this.password = '';
        this.confirmPassword = '';
        this.recoveryInput = '';
        if (!this.recoveryCode()) void this.router.navigate(['/app']);
      },
      error: (error) => {
        this.errorMessage.set(
          error.status === 0
            ? 'Cannot reach the server. Please try again.'
            : (error.error?.message ?? 'Unable to sign in. Please try again.'),
        );
      },
    });
  }

  toggleMode(): void {
    if (this.recovering) {
      this.recovering = false;
      this.registering = true;
    }
    this.registering = !this.registering;
    this.password = '';
    this.confirmPassword = '';
    this.errorMessage.set('');
  }
  recoverMode(): void {
    this.recovering = true;
    this.registering = false;
    this.password = '';
    this.confirmPassword = '';
    this.errorMessage.set('');
  }
  continue(): void {
    this.recoveryCode.set('');
    void this.router.navigate(['/app']);
  }
}
