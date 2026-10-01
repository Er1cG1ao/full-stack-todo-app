import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { DatePipe } from '@angular/common';
import { Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { Todo } from '../model/todo';
import { TodoDataService } from '../service/data/todo-data.service';
import { AuthService } from '../service/auth.service';

@Component({
  selector: 'app-list-todos',
  imports: [DatePipe],
  templateUrl: './list-todos.html',
  styleUrl: './list-todos.css',
})
export class ListTodos implements OnInit {
  todos: Todo[] = [];
  message = '';
  errorMessage = '';
  loading = false;

  private todoService = inject(TodoDataService);
  private auth = inject(AuthService);
  private router = inject(Router);
  private changeDetector = inject(ChangeDetectorRef);

  ngOnInit(): void {
    this.refreshTodos();
  }

  /** 数据来源从"组件里写死的数组"换成了后端 —— 这就是本讲的核心变化 */
  refreshTodos(): void {
    this.loading = true;
    this.todoService.retrieveAllTodos(this.username).subscribe({
      next: (todos) => {
        this.todos = todos;
        this.loading = false;
        this.changeDetector.markForCheck();
      },
      error: (error) => this.handleError(error),
    });
  }

  deleteTodo(id: number): void {
    this.todoService.deleteTodo(this.username, id).subscribe({
      next: () => {
        this.message = `Todo ${id} 已删除`;
        // 删完必须重新拉一次列表：后端才是唯一数据源，前端数组只是它的一份副本
        this.refreshTodos();
      },
      error: (error) => this.handleError(error),
    });
  }

  updateTodo(id: number): void {
    this.router.navigate(['/todos', id]);
  }

  /** 用 id = -1 表示"新建"，详情页据此决定是 POST 还是 PUT */
  addTodo(): void {
    this.router.navigate(['/todos', -1]);
  }

  private get username(): string {
    return this.auth.username ?? '';
  }

  private handleError(error: HttpErrorResponse): void {
    this.loading = false;
    this.message = '';
    this.errorMessage =
      error.status === 0
        ? '连不上后端，请确认 Spring Boot 已启动在 http://localhost:8080'
        : `请求失败：${error.status} ${error.error?.message ?? error.message}`;
    this.changeDetector.markForCheck();
  }
}
