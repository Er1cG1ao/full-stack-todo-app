import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { Todo as TodoModel } from '../model/todo';
import { TodoDataService } from '../service/data/todo-data.service';
import { AuthService } from '../service/auth.service';

@Component({
  selector: 'app-todo',
  imports: [FormsModule, RouterLink],
  templateUrl: './todo.html',
  styleUrl: './todo.css',
})
export class Todo implements OnInit {
  id = -1;
  todo: TodoModel = { id: -1, description: '', targetDate: '', done: false };
  errorMessage = '';

  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private todoService = inject(TodoDataService);
  private auth = inject(AuthService);
  private changeDetector = inject(ChangeDetectorRef);

  ngOnInit(): void {
    // 路由参数永远是字符串，别忘了转数字
    this.id = Number(this.route.snapshot.params['id']);

    if (this.id === -1) {
      // 新建：给一个空表单，targetDate 默认今天
      this.todo = {
        id: -1,
        description: '',
        targetDate: new Date().toISOString().slice(0, 10),
        done: false,
      };
      return;
    }

    // 编辑：先去后端把这条记录读回来
    this.todoService.retrieveTodo(this.username, this.id).subscribe({
      next: (todo) => {
        // 后端给的是 "2026-08-01T00:00:00.000+00:00"，<input type="date"> 只认 "2026-08-01"
        this.todo = { ...todo, targetDate: todo.targetDate?.slice(0, 10) ?? '' };
        this.changeDetector.markForCheck();
      },
      error: (error) => this.handleError(error),
    });
  }

  /** 同一个表单，两种请求：id 为 -1 走 POST 新建，否则走 PUT 更新 */
  saveTodo(): void {
    const request$ =
      this.id === -1
        ? this.todoService.createTodo(this.username, this.todo)
        : this.todoService.updateTodo(this.username, this.id, this.todo);

    request$.subscribe({
      next: () => this.router.navigate(['/todos']),
      error: (error) => this.handleError(error),
    });
  }

  private get username(): string {
    return this.auth.username ?? '';
  }

  private handleError(error: HttpErrorResponse): void {
    this.errorMessage =
      error.status === 0
        ? '连不上后端，请确认 Spring Boot 已启动在 http://localhost:8080'
        : `请求失败：${error.status} ${error.error?.message ?? error.message}`;
    this.changeDetector.markForCheck();
  }
}
