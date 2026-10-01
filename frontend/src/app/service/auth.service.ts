import { Injectable } from '@angular/core';

const SESSION_KEY = 'authenticatedUser';
const TOKEN_KEY = 'basicAuthToken';

/**
 * 登录状态 + Basic 认证凭据的唯一来源。
 *
 * 和 class2 的 HardcodedAuthenticationService 一样是"假登录"（用户名密码写死），
 * 但多做了一件事：登录成功时把 Basic token 存下来，供 HTTP 拦截器取用。
 * class2 是在拦截器里把用户名密码又写死了一遍 —— 两处硬编码，容易改漏。
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  // 后端 application.properties 里配置的账号，必须与之一致
  private readonly validUsername = 'alice';
  private readonly validPassword = 'dummy';

  authenticate(username: string, password: string): boolean {
    if (username === this.validUsername && password === this.validPassword) {
      // 'Basic ' + base64(username:password) —— 这就是 HTTP Basic 认证头的全部内容
      const token = 'Basic ' + window.btoa(`${username}:${password}`);
      sessionStorage.setItem(SESSION_KEY, username);
      sessionStorage.setItem(TOKEN_KEY, token);
      return true;
    }
    return false;
  }

  get username(): string | null {
    return this.isBrowser ? sessionStorage.getItem(SESSION_KEY) : null;
  }

  get basicAuthToken(): string | null {
    return this.isBrowser ? sessionStorage.getItem(TOKEN_KEY) : null;
  }

  isUserLoggedIn(): boolean {
    return this.username !== null;
  }

  logout(): void {
    sessionStorage.removeItem(SESSION_KEY);
    sessionStorage.removeItem(TOKEN_KEY);
  }

  // sessionStorage 只存在于浏览器；SSR/预渲染时 window 是 undefined
  private get isBrowser(): boolean {
    return typeof window !== 'undefined';
  }
}
