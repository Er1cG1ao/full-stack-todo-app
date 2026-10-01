import { Routes } from '@angular/router';
import { Login } from './login/login';
import { Welcome } from './welcome/welcome';
import { ListTodos } from './list-todos/list-todos';
import { Todo } from './todo/todo';
import { Logout } from './logout/logout';
import { NotFound } from './error/error';
import { authGuard } from './service/auth-guard';

export const routes: Routes = [
  { path: '', redirectTo: '/login', pathMatch: 'full' },
  { path: 'login', component: Login },
  { path: 'welcome/:name', component: Welcome, canActivate: [authGuard] },
  { path: 'todos', component: ListTodos, canActivate: [authGuard] },
  // 注意：具体路径 'todos/:id' 写在 'todos' 之后，Angular 按顺序匹配第一个命中的
  { path: 'todos/:id', component: Todo, canActivate: [authGuard] },
  { path: 'logout', component: Logout, canActivate: [authGuard] },
  { path: '**', component: NotFound },
];
