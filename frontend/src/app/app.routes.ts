import { Routes } from '@angular/router';
import { Login } from './login/login';
import { authGuard } from './service/auth-guard';
export const routes: Routes = [
  { path: '', redirectTo: 'app', pathMatch: 'full' },
  { path: 'login', component: Login, title: 'Welcome · Daylight' },
  {
    path: 'app',
    loadComponent: () => import('./workspace/workspace').then((m) => m.Workspace),
    canActivate: [authGuard],
    title: 'Your workspace · Daylight',
  },
  { path: 'welcome/:name', redirectTo: 'app' },
  { path: 'todos', redirectTo: 'app' },
  { path: 'todos/:id', redirectTo: 'app' },
  { path: '**', redirectTo: 'app' },
];
