import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { HelloWorldBean, WelcomeDataService } from '../service/data/welcome-data.service';

@Component({
  selector: 'app-welcome',
  imports: [RouterLink],
  templateUrl: './welcome.html',
  styleUrl: './welcome.css',
})
export class Welcome implements OnInit {
  name = '';
  messageFromService = '';
  errorFromService = '';

  private route = inject(ActivatedRoute);
  private service = inject(WelcomeDataService);
  private changeDetector = inject(ChangeDetectorRef);

  ngOnInit(): void {
    this.name = this.route.snapshot.params['name'];
  }

  /**
   * 前后端联通的最小验证：点一下按钮去后端拿一句话。
   *
   * 重点讲三件事：
   *  1. 调 service 方法只是"拿到 Observable"，此刻请求还没发出去
   *  2. subscribe() 才真正发请求 —— Observable 是 cold 的
   *  3. 回调是异步的，next 里的代码晚于函数最后一行执行
   */
  getWelcomeMessage(): void {
    this.service.executeHelloWorldServiceWithPathVariable(this.name).subscribe({
      next: (response) => this.handleSuccess(response),
      error: (error) => this.handleError(error),
    });

    console.log('这行会先打印 —— 说明请求是异步的');
  }

  private handleSuccess(response: HelloWorldBean): void {
    this.messageFromService = response.message;
    this.errorFromService = '';
    this.changeDetector.markForCheck();
  }

  private handleError(error: HttpErrorResponse): void {
    this.messageFromService = '';
    // error.error 是后端返回的响应体；连不上后端时 status 为 0
    this.errorFromService =
      error.status === 0
        ? '连不上后端，请确认 Spring Boot 已启动在 http://localhost:8080'
        : `请求失败：${error.status} ${error.error?.message ?? error.message}`;
    this.changeDetector.markForCheck();
  }
}
