import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { API_URL } from '../app.constants';
import { Backup, Project, Task } from '../model/task';
@Injectable({ providedIn: 'root' })
export class ApiService {
  private http = inject(HttpClient);
  tasks(trash = false) {
    return this.http.get<Task[]>(`${API_URL}/api/tasks?trash=${trash}`);
  }
  projects() {
    return this.http.get<Project[]>(`${API_URL}/api/projects`);
  }
  create(task: Task) {
    return this.http.post<Task>(`${API_URL}/api/tasks`, task);
  }
  update(task: Task) {
    return this.http.put<Task>(`${API_URL}/api/tasks/${task.id}`, task);
  }
  trash(id: number) {
    return this.http.delete<void>(`${API_URL}/api/tasks/${id}`);
  }
  restore(id: number) {
    return this.http.post<Task>(`${API_URL}/api/tasks/${id}/restore`, {});
  }
  purge(id: number) {
    return this.http.delete<void>(`${API_URL}/api/tasks/${id}/permanent`);
  }
  bulk(ids: number[], action: string) {
    return this.http.post<Task[]>(`${API_URL}/api/tasks/bulk`, { ids, action });
  }
  createProject(name: string, color: string) {
    return this.http.post<Project>(`${API_URL}/api/projects`, { name, color });
  }
  updateProject(project: Project) {
    return this.http.put<Project>(`${API_URL}/api/projects/${project.id}`, project);
  }
  deleteProject(id: number) {
    return this.http.delete<void>(`${API_URL}/api/projects/${id}`);
  }
  export() {
    return this.http.get<Backup>(`${API_URL}/api/backup`);
  }
  import(backup: Backup) {
    return this.http.post<{ tasks: number; projects: number }>(
      `${API_URL}/api/backup/import`,
      backup,
    );
  }
  changePassword(currentPassword: string, newPassword: string) {
    return this.http.post<void>(`${API_URL}/auth/password`, { currentPassword, newPassword });
  }
  recoveryKey(currentPassword: string) {
    return this.http.post<{ recoveryCode: string }>(`${API_URL}/auth/recovery-key`, {
      currentPassword,
    });
  }
}
