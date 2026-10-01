import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

/**
 * 组件类不要叫 Error —— 它会遮蔽 JS 内置的 Error 构造函数，是个坏习惯。
 * 这里改名为 NotFound。
 */
@Component({
  selector: 'app-not-found',
  imports: [RouterLink],
  templateUrl: './error.html',
  styleUrl: './error.css',
})
export class NotFound {
  errorCode = 404;
  errorMessage = 'Page Not Found';
}
