import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withFetch, withInterceptors } from '@angular/common/http';
import { routes } from './app.routes';
import { sessionInterceptor } from './service/http/session-interceptor';

/**
 * class2 的 app.module.ts 做的三件事，在 standalone 时代都搬到了这里：
 *   imports: [HttpClientModule]                        -> provideHttpClient()
 *   providers: [{provide: HTTP_INTERCEPTORS, ...}]      -> withInterceptors([...])
 *   imports: [AppRoutingModule]                         -> provideRouter(routes)
 * FormsModule 不再全局注册，改由需要它的组件自己 imports。
 */
export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    provideHttpClient(
      withFetch(), // 底层用 fetch 而不是 XHR，SSR 友好
      withInterceptors([sessionInterceptor]),
    ),
  ],
};
