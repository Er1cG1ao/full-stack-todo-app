import { Component, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { ThemeService } from './service/theme.service';
@Component({ selector: 'app-root', imports: [RouterOutlet], templateUrl: './app.html' })
export class App {
  title = 'Daylight';
  theme = inject(ThemeService);
}
