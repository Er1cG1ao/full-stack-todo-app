import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API_URL } from '../../app.constants';
import { Todo } from '../../model/todo';

/**
 * 和后端 TodoResource 一一对应的数据访问层。
 *
 * Java 类比：这就是前端的 Repository / FeignClient —— 组件不碰 URL，只调方法。
 *
 * providedIn: 'root' == Spring 里的单例 @Service，全应用共用一个实例。
 */
@Injectable({ providedIn: 'root' })
export class TodoDataService {
  // inject() 是 v14+ 的写法，等价于构造函数注入，但不用写 constructor
  private http = inject(HttpClient);

  /** GET /users/{username}/todos */
  retrieveAllTodos(username: string): Observable<Todo[]> {
    return this.http.get<Todo[]>(`${API_URL}/users/${username}/todos`);
  }

  /** GET /users/{username}/todos/{id} */
  retrieveTodo(username: string, id: number): Observable<Todo> {
    return this.http.get<Todo>(`${API_URL}/users/${username}/todos/${id}`);
  }

  /** POST /users/{username}/todos */
  createTodo(username: string, todo: Todo): Observable<Todo> {
    return this.http.post<Todo>(`${API_URL}/users/${username}/todos`, todo);
  }

  /** PUT /users/{username}/todos/{id} */
  updateTodo(username: string, id: number, todo: Todo): Observable<Todo> {
    return this.http.put<Todo>(`${API_URL}/users/${username}/todos/${id}`, todo);
  }

  /** DELETE /users/{username}/todos/{id} */
  deleteTodo(username: string, id: number): Observable<void> {
    return this.http.delete<void>(`${API_URL}/users/${username}/todos/${id}`);
  }
}
