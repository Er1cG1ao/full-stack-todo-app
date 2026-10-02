import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';
import { catchError, map, of } from 'rxjs';

/**
 * 函数式路由守卫（v15+ 推荐写法，替代 class2 里 implements CanActivate 的 RouteGuardService）。
 *
 * Java 类比：Spring Security 的 filter chain —— 没登录就别想进这个 URL。
 * 注意这只是"前端体验层"的拦截，真正的权限校验必须在后端做。
 */
export const authGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);

  return auth.restoreSession().pipe(
    map(() => true),
    catchError(() => {
      auth.clearSession();
      return of(router.createUrlTree(['/login']));
    }),
  );
};
