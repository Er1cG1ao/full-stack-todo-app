import { Component, OnInit, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../service/auth.service';

@Component({
  selector: 'app-logout',
  imports: [RouterLink],
  templateUrl: './logout.html',
  styleUrl: './logout.css',
})
export class Logout implements OnInit {
  private auth = inject(AuthService);

  ngOnInit(): void {
    // 清掉 sessionStorage 里的用户名和 Basic token，后续请求就不再带 Authorization 头
    this.auth.logout();
  }
}
