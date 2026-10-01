import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../service/auth.service';

@Component({
  selector: 'app-login',
  imports: [FormsModule],
  templateUrl: './login.html',
  styleUrl: './login.css',
})
export class Login {
  username = 'alice';
  password = '';
  errorMessage = '';

  private router = inject(Router);
  private auth = inject(AuthService);

  handleLogin() {
    // 校验逻辑不再写在组件里，而是委托给 AuthService（组件只管界面）
    if (this.auth.authenticate(this.username, this.password)) {
      this.errorMessage = '';
      this.router.navigate(['/welcome', this.username]);
    } else {
      this.errorMessage = 'Invalid username or password. Try alice/dummy';
    }
  }

  clearForm(): void {
    this.username = '';
    this.password = '';
    this.errorMessage = '';
  }
  fillDummyCredentials(): void {
    this.username = 'alice';
    this.password = 'dummy';
    this.errorMessage = '';
  }
}
