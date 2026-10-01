import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_URL } from '../../app.constants';

/** 后端 /hello-world-bean 返回的 JSON 形状：{ "message": "..." } */
export interface HelloWorldBean {
  message: string;
}

/**
 * 第一个"打通前后端"的最小 service —— 课上先用它验证链路，再讲 Todo 的增删改查。
 */
@Injectable({ providedIn: 'root' })
export class WelcomeDataService {
  private http = inject(HttpClient);

  /** GET /hello-world-bean */
  executeHelloWorldBeanService(): Observable<HelloWorldBean> {
    return this.http.get<HelloWorldBean>(`${API_URL}/hello-world-bean`);
  }

  /** GET /hello-world/path-variable/{name} —— 对应后端的 @PathVariable */
  executeHelloWorldServiceWithPathVariable(name: string): Observable<HelloWorldBean> {
    return this.http.get<HelloWorldBean>(`${API_URL}/hello-world/path-variable/${name}`);
  }
}
