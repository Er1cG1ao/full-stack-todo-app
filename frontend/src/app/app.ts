import { Component, inject } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from './service/auth.service';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
  title = 'Todo Application';

  private auth = inject(AuthService);

  get username(): string {
    return this.auth.username ?? 'alice';
  }

  get isLoggedIn(): boolean {
    return this.auth.isUserLoggedIn();
  }
}
