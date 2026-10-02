import {
  Component,
  computed,
  signal,
  inject,
  OnInit,
  OnDestroy,
  HostListener,
  ViewChild,
  ElementRef,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DatePipe, NgTemplateOutlet } from '@angular/common';
import { Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { Observable, forkJoin, finalize } from 'rxjs';
import { ApiService } from '../service/api.service';
import { AuthService } from '../service/auth.service';
import { ThemeService } from '../service/theme.service';
import { Task, Project, Backup, dateKey, newTask, filterTasks } from '../model/task';
import { Icon } from '../ui/icon';

@Component({
  selector: 'app-workspace',
  imports: [FormsModule, DatePipe, NgTemplateOutlet, Icon],
  templateUrl: './workspace.html',
  styleUrl: './workspace.css',
})
export class Workspace implements OnInit, OnDestroy {
  api = inject(ApiService);
  auth = inject(AuthService);
  theme = inject(ThemeService);
  router = inject(Router);
  @ViewChild('taskDialog') taskDialog!: ElementRef<HTMLDialogElement>;
  @ViewChild('projectDialog') projectDialog!: ElementRef<HTMLDialogElement>;
  @ViewChild('settingsDialog') settingsDialog!: ElementRef<HTMLDialogElement>;
  @ViewChild('confirmDialog') confirmDialog!: ElementRef<HTMLDialogElement>;
  @ViewChild('quickInput') quickInput!: ElementRef<HTMLInputElement>;
  @ViewChild('searchInput') searchInput!: ElementRef<HTMLInputElement>;
  tasks = signal<Task[]>([]);
  projects = signal<Project[]>([]);
  loading = signal(true);
  busy = signal(false);
  view = signal('all');
  query = signal('');
  priority = signal('');
  status = signal('active');
  tag = signal('');
  sort = signal('due');
  layout = signal(localStorage.getItem('daylight-layout') ?? 'list');
  sidebar = signal(false);
  error = signal('');
  toast = signal('');
  selected = signal<Set<number>>(new Set());
  today = signal(dateKey());
  draft = signal<Task | null>(null);
  quickTitle = '';
  subtaskTitle = '';
  projectName = '';
  projectColor = '#c15d42';
  editingProject: Project | null = null;
  currentPassword = '';
  newPassword = '';
  confirmPassword = '';
  recoveryPassword = '';
  recoveryCode = signal('');
  modalError = signal('');
  confirming = signal({ title: '', message: '', label: 'Confirm' });
  pendingAction: (() => void) | null = null;
  undoAction: (() => void) | null = null;
  private toastTimer?: ReturnType<typeof setTimeout>;
  private dayTimer = setInterval(() => this.today.set(dateKey()), 60000);
  private loadSequence = 0;
  nav = [
    { id: 'all', name: 'All tasks', icon: 'list' },
    { id: 'inbox', name: 'Inbox', icon: 'inbox' },
    { id: 'today', name: 'Today', icon: 'sun' },
    { id: 'upcoming', name: 'Upcoming', icon: 'calendar' },
    { id: 'starred', name: 'Important', icon: 'star' },
  ];
  colors = ['#c15d42', '#708965', '#638eae', '#9b7cad', '#c39947', '#cb798c'];
  active = computed(() => this.tasks().filter((t) => !t.done && !t.deletedAt));
  overdue = computed(
    () => this.active().filter((t) => !!t.targetDate && t.targetDate < this.today()).length,
  );
  completedToday = computed(
    () =>
      this.tasks().filter(
        (t) =>
          !t.deletedAt &&
          t.done &&
          t.completedAt &&
          dateKey(new Date(t.completedAt)) === this.today(),
      ).length,
  );
  dueToday = computed(() => this.active().filter((t) => t.targetDate === this.today()).length);
  progress = computed(() =>
    Math.round(
      (this.completedToday() / Math.max(1, this.completedToday() + this.dueToday())) * 100,
    ),
  );
  tags = computed(() =>
    [
      ...new Set(
        this.tasks()
          .filter((t) => !t.deletedAt)
          .flatMap((t) =>
            t.tags
              .split(',')
              .map((s) => s.trim())
              .filter(Boolean),
          ),
      ),
    ].sort(),
  );
  title = computed(() =>
    this.view().startsWith('project:')
      ? this.view().slice(8)
      : ({
          all: 'Your day, your way.',
          inbox: 'Inbox',
          today: 'Today',
          upcoming: 'Upcoming',
          starred: 'Important',
          completed: 'Completed',
          trash: 'Trash',
        }[this.view()] ?? 'Tasks'),
  );
  subtitle = computed(
    () =>
      ({
        all: 'Make room for what matters. One task at a time.',
        inbox: 'A home for ideas before they become a plan.',
        today: 'A little focus goes a long way.',
        upcoming: 'See what’s ahead. Get a head start.',
        starred: 'Keep your most meaningful work in sight.',
        completed: 'Take a moment to appreciate your progress.',
        trash: 'Deleted tasks stay here until you remove them permanently.',
      })[this.view()] ?? 'A little structure for your bigger plans.',
  );
  filtered = computed(() =>
    filterTasks(
      this.tasks(),
      this.view(),
      this.query(),
      this.priority(),
      this.status(),
      this.tag(),
      this.today(),
    ).sort((a, b) => {
      if (this.sort() === 'title') return a.description.localeCompare(b.description);
      if (this.sort() === 'newest') return b.createdAt.localeCompare(a.createdAt);
      if (this.sort() === 'priority')
        return (
          this.rank(b.priority) - this.rank(a.priority) ||
          a.description.localeCompare(b.description)
        );
      return (
        (a.targetDate ?? '9999').localeCompare(b.targetDate ?? '9999') ||
        this.rank(b.priority) - this.rank(a.priority) ||
        b.id - a.id
      );
    }),
  );
  groups = computed(() => {
    const grouped = new Map<string, Task[]>();
    for (const task of this.filtered()) {
      const label =
        this.view() === 'completed'
          ? 'Finished'
          : this.view() === 'trash'
            ? 'Deleted tasks'
            : task.done
              ? 'Completed'
              : !task.targetDate
                ? 'No date'
                : task.targetDate < this.today()
                  ? 'Overdue'
                  : task.targetDate === this.today()
                    ? 'Today'
                    : 'Upcoming';
      grouped.set(label, [...(grouped.get(label) ?? []), task]);
    }
    return [...grouped.entries()].map(([name, tasks]) => ({ name, tasks }));
  });
  boardColumns = [
    { priority: 'HIGH', label: 'High priority' },
    { priority: 'MEDIUM', label: 'Medium priority' },
    { priority: 'LOW', label: 'Low priority' },
  ];
  ngOnInit() {
    this.load();
  }
  ngOnDestroy() {
    clearInterval(this.dayTimer);
    clearTimeout(this.toastTimer);
  }
  load(clearError = true): void {
    const sequence = ++this.loadSequence;
    this.loading.set(true);
    if (clearError) this.error.set('');
    forkJoin({
      active: this.api.tasks(),
      trash: this.api.tasks(true),
      projects: this.api.projects(),
    })
      .pipe(
        finalize(() => {
          if (sequence === this.loadSequence) this.loading.set(false);
        }),
      )
      .subscribe({
        next: (data) => {
          if (sequence !== this.loadSequence) return;
          this.tasks.set([...data.active, ...data.trash]);
          this.projects.set(data.projects);
          this.selected.set(new Set());
        },
        error: (e) => {
          if (sequence === this.loadSequence) this.error.set(this.message(e));
        },
      });
  }
  navigate(view: string) {
    this.view.set(view);
    this.query.set('');
    this.priority.set('');
    this.tag.set('');
    this.status.set('active');
    this.selected.set(new Set());
    this.sidebar.set(false);
  }
  count(view: string): number {
    return filterTasks(this.tasks(), view, '', '', 'active', '', this.today()).length;
  }
  rank(p: string) {
    return { LOW: 0, MEDIUM: 1, HIGH: 2 }[p] ?? 0;
  }
  newSet() {
    return new Set<number>();
  }
  boardTasks(priority: string) {
    return this.filtered().filter((t) => t.priority === priority);
  }
  projectColorFor(name: string) {
    return this.projects().find((p) => p.name === name)?.color ?? '#81927d';
  }
  tagList(task: Task) {
    return task.tags
      .split(',')
      .map((t) => t.trim())
      .filter(Boolean)
      .slice(0, 3);
  }
  completedSubtasks(task: Task) {
    return task.subtasks.filter((s) => s.done).length;
  }
  dueLabel(date: string | null): string {
    if (!date) return '';
    if (date === this.today()) return 'Today';
    const tomorrow = new Date();
    tomorrow.setDate(tomorrow.getDate() + 1);
    if (date === dateKey(tomorrow)) return 'Tomorrow';
    return new Date(date + 'T12:00:00').toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric',
    });
  }
  setLayout(layout: string) {
    this.layout.set(layout);
    localStorage.setItem('daylight-layout', layout);
  }
  toggleSelection(id: number) {
    if (!this.selected().has(id) && this.selected().size >= 100) {
      this.notify('Select up to 100 tasks at a time.');
      return;
    }
    this.selected.update((set) => {
      const next = new Set(set);
      next.has(id) ? next.delete(id) : next.add(id);
      return next;
    });
  }
  selectAll() {
    const ids = this.filtered()
      .slice(0, 100)
      .map((t) => t.id);
    this.selected.set(this.selected().size === ids.length ? new Set() : new Set(ids));
    if (this.filtered().length > 100) this.notify('Selected the first 100 tasks.');
  }
  perform<T>(
    operation: Observable<T>,
    message: string,
    undo?: () => void,
    after?: () => void,
  ): void {
    if (this.busy()) return;
    this.busy.set(true);
    this.error.set('');
    this.modalError.set('');
    operation.pipe(finalize(() => this.busy.set(false))).subscribe({
      next: () => {
        this.notify(message, undo);
        after?.();
        this.load();
      },
      error: (e) => {
        this.error.set(this.message(e));
        this.modalError.set(this.message(e));
        if (e.status === 409) this.load(false);
      },
    });
  }
  addQuick() {
    if (!this.quickTitle.trim() || this.busy()) return;
    const task = newTask(
      this.view().startsWith('project:') ? this.view().slice(8) : '',
      this.view() === 'today' ? this.today() : null,
    );
    task.description = this.quickTitle.trim();
    this.perform(this.api.create(task), 'Task added', undefined, () => (this.quickTitle = ''));
  }
  toggleDone(task: Task) {
    this.perform(
      this.api.update({ ...task, done: !task.done }),
      task.done
        ? 'Task reopened'
        : task.recurrence === 'NONE'
          ? 'Nice work. Task completed.'
          : 'Task completed. Next occurrence created.',
    );
  }
  star(task: Task) {
    this.perform(
      this.api.update({ ...task, starred: !task.starred }),
      task.starred ? 'Removed from Important' : 'Added to Important',
    );
  }
  trash(task: Task) {
    this.perform(this.api.trash(task.id), 'Task moved to Trash', () =>
      this.perform(this.api.restore(task.id), 'Task restored'),
    );
  }
  restore(task: Task) {
    this.perform(this.api.restore(task.id), 'Task restored');
  }
  duplicate(task: Task) {
    this.perform(
      this.api.create({
        ...task,
        id: 0,
        done: false,
        description: task.description + ' (copy)',
        subtasks: task.subtasks.map((s) => ({ ...s, done: false })),
      }),
      'Task duplicated',
    );
  }
  bulk(action: string) {
    this.perform(
      this.api.bulk([...this.selected()], action),
      action === 'trash'
        ? 'Tasks moved to Trash'
        : action === 'restore'
          ? 'Tasks restored'
          : 'Tasks updated',
    );
  }
  purge(task: Task) {
    this.confirm(
      'Delete permanently?',
      '“' + task.description + '” will be removed forever. This cannot be undone.',
      'Delete permanently',
      () => this.perform(this.api.purge(task.id), 'Task permanently deleted'),
    );
  }
  edit(task?: Task) {
    this.modalError.set('');
    this.subtaskTitle = '';
    this.draft.set(
      task
        ? structuredClone(task)
        : newTask(
            this.view().startsWith('project:') ? this.view().slice(8) : '',
            this.view() === 'today' ? this.today() : null,
          ),
    );
    this.taskDialog.nativeElement.showModal();
  }
  saveTask() {
    const draft = this.draft();
    if (!draft || !draft.description.trim()) return;
    const task = { ...draft, targetDate: draft.targetDate || null };
    this.perform(
      task.id ? this.api.update(task) : this.api.create(task),
      task.id ? 'Task updated' : 'Task added',
      undefined,
      () => this.taskDialog.nativeElement.close(),
    );
  }
  addSubtask() {
    const title = this.subtaskTitle.trim();
    if (!title) return;
    this.draft.update((t) =>
      t ? { ...t, subtasks: [...t.subtasks, { id: crypto.randomUUID(), title, done: false }] } : t,
    );
    this.subtaskTitle = '';
  }
  removeSubtask(id: string) {
    this.draft.update((t) => (t ? { ...t, subtasks: t.subtasks.filter((s) => s.id !== id) } : t));
  }
  openProject(project?: Project) {
    this.editingProject = project ?? null;
    this.projectName = project?.name ?? '';
    this.projectColor = project?.color ?? this.colors[0];
    this.modalError.set('');
    this.projectDialog.nativeElement.showModal();
  }
  saveProject() {
    const name = this.projectName.trim();
    if (!name) return;
    const old = this.editingProject?.name;
    const operation = this.editingProject
      ? this.api.updateProject({ ...this.editingProject, name, color: this.projectColor })
      : this.api.createProject(name, this.projectColor);
    this.perform(operation, 'Project saved', undefined, () => {
      this.projectDialog.nativeElement.close();
      if (!old || this.view() === 'project:' + old) this.navigate('project:' + name);
    });
  }
  deleteProject() {
    const project = this.editingProject;
    if (!project) return;
    this.projectDialog.nativeElement.close();
    this.confirm(
      'Remove this project?',
      'All tasks in “' + project.name + '” will move to your Inbox. Your tasks will be kept.',
      'Remove project',
      () => {
        this.perform(this.api.deleteProject(project.id), 'Project removed', undefined, () =>
          this.navigate('inbox'),
        );
      },
    );
  }
  openSettings() {
    this.recoveryPassword = '';
    this.recoveryCode.set('');
    this.modalError.set('');
    this.currentPassword = '';
    this.newPassword = '';
    this.confirmPassword = '';
    this.settingsDialog.nativeElement.showModal();
  }
  changePassword() {
    if (this.newPassword !== this.confirmPassword) {
      this.modalError.set('New passwords do not match.');
      return;
    }
    if (this.busy()) return;
    this.busy.set(true);
    this.api
      .changePassword(this.currentPassword, this.newPassword)
      .pipe(finalize(() => this.busy.set(false)))
      .subscribe({
        next: () => {
          this.auth.clearSession();
          this.settingsDialog.nativeElement.close();
          void this.router.navigate(['/login'], { queryParams: { passwordChanged: true } });
        },
        error: (e) => this.modalError.set(this.message(e)),
      });
  }
  generateRecoveryKey() {
    if (this.busy() || !this.recoveryPassword) return;
    this.busy.set(true);
    this.modalError.set('');
    this.api
      .recoveryKey(this.recoveryPassword)
      .pipe(finalize(() => this.busy.set(false)))
      .subscribe({
        next: (response) => {
          this.recoveryCode.set(response.recoveryCode);
          this.recoveryPassword = '';
        },
        error: (e) => this.modalError.set(this.message(e)),
      });
  }
  exportBackup() {
    this.api.export().subscribe({
      next: (backup) => {
        const url = URL.createObjectURL(
          new Blob([JSON.stringify(backup, null, 2)], { type: 'application/json' }),
        );
        const link = document.createElement('a');
        link.href = url;
        link.download = 'daylight-backup-' + this.today() + '.json';
        link.click();
        URL.revokeObjectURL(url);
        this.notify('Your backup has been downloaded.');
      },
      error: (e) => this.modalError.set(this.message(e)),
    });
  }
  async importBackup(event: Event) {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;
    if (file.size > 5 * 1024 * 1024) {
      this.modalError.set('Choose a backup smaller than 5 MB.');
      input.value = '';
      return;
    }
    try {
      const backup = JSON.parse(await file.text()) as Backup;
      if (
        backup.formatVersion !== 1 ||
        !Array.isArray(backup.tasks) ||
        !Array.isArray(backup.projects)
      )
        throw new Error('Invalid backup');
      this.settingsDialog.nativeElement.close();
      this.confirm(
        'Import backup?',
        backup.tasks.length +
          ' tasks will be added to your account. Existing data stays intact. Importing twice creates duplicate tasks.',
        'Import tasks',
        () => this.perform(this.api.import(backup), 'Backup imported'),
      );
    } catch {
      this.modalError.set('This file is not a valid Daylight backup.');
    }
    input.value = '';
  }
  logout() {
    if (this.busy()) return;
    this.busy.set(true);
    this.auth
      .logout()
      .pipe(finalize(() => this.busy.set(false)))
      .subscribe({
        next: () => void this.router.navigate(['/login']),
        error: (e) => this.error.set(this.message(e)),
      });
  }
  confirm(title: string, message: string, label: string, action: () => void) {
    this.confirming.set({ title, message, label });
    this.pendingAction = action;
    this.confirmDialog.nativeElement.showModal();
  }
  confirmAction() {
    this.confirmDialog.nativeElement.close();
    this.pendingAction?.();
    this.pendingAction = null;
  }
  notify(message: string, undo?: () => void) {
    clearTimeout(this.toastTimer);
    this.toast.set(message);
    this.undoAction = undo ?? null;
    this.toastTimer = setTimeout(() => {
      this.toast.set('');
      this.undoAction = null;
    }, 6000);
  }
  undo() {
    this.undoAction?.();
    this.undoAction = null;
    this.toast.set('');
  }
  message(e: HttpErrorResponse): string {
    return e.status === 0
      ? 'Cannot connect right now. Check your connection and try again.'
      : (e.error?.message ?? 'Something went wrong. Please try again.');
  }
  @HostListener('document:keydown', ['$event']) keydown(event: KeyboardEvent) {
    if (document.querySelector('dialog[open]')) return;
    const element = event.target as HTMLElement;
    if (['INPUT', 'TEXTAREA', 'SELECT'].includes(element.tagName) || element.isContentEditable)
      return;
    if (event.key === 'n') {
      event.preventDefault();
      this.edit();
    }
    if (event.key === '/') {
      event.preventDefault();
      this.searchInput.nativeElement.focus();
    }
    if (event.key === 'Escape') {
      this.selected.set(new Set());
      this.sidebar.set(false);
    }
  }
}
