import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from '../auth.service';

/**
 * 函数式 HTTP 拦截器（v15+ 推荐写法，替代 class2 里的 HttpInterceptor 类 + HTTP_INTERCEPTORS 数组）。
 *
 * Java 类比：Spring MVC 的 HandlerInterceptor / Servlet Filter，
 * 只不过这里拦的是"出站"请求，统一给每个请求盖上 Authorization 章。
 *
 * 关键点：HttpRequest 是不可变的，必须 clone() 出一个新对象再改。
 */
export const basicAuthInterceptor: HttpInterceptorFn = (request, next) => {
  const token = inject(AuthService).basicAuthToken;

  // 未登录（比如 /hello-world-bean 的匿名调用）就原样放行
  if (!token) {
    return next(request);
  }

  const authorized = request.clone({
    setHeaders: { Authorization: token },
  });

  return next(authorized);
};
