import { Injectable, signal } from '@angular/core';
@Injectable({ providedIn: 'root' })
export class ThemeService {
  dark = signal(false);
  constructor() {
    const stored = localStorage.getItem('daylight-theme');
    this.dark.set(
      stored
        ? stored === 'dark'
        : (window.matchMedia?.('(prefers-color-scheme: dark)').matches ?? false),
    );
    this.apply();
  }
  toggle(): void {
    this.dark.update((d) => !d);
    this.apply();
  }
  private apply(): void {
    document.documentElement.dataset['theme'] = this.dark() ? 'dark' : 'light';
    localStorage.setItem('daylight-theme', this.dark() ? 'dark' : 'light');
  }
}
