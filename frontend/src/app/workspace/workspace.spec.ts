import { TestBed, ComponentFixture } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';
import { Workspace } from './workspace';
import { ApiService } from '../service/api.service';
import { AuthService } from '../service/auth.service';
import { ThemeService } from '../service/theme.service';
import { newTask } from '../model/task';

describe('Workspace interactions', () => {
  let fixture: ComponentFixture<Workspace>;
  let component: Workspace;
  const task = {
    ...newTask(),
    id: 1,
    description: 'Finish project',
    createdAt: '2026-10-02T10:00:00Z',
  };
  const api = {
    tasks: vi.fn(),
    projects: vi.fn(),
    create: vi.fn(),
    update: vi.fn(),
    trash: vi.fn(),
    restore: vi.fn(),
    bulk: vi.fn(),
  };
  beforeEach(async () => {
    localStorage.clear();
    vi.clearAllMocks();
    api.tasks.mockImplementation((trash = false) => of(trash ? [] : [{ ...task }]));
    api.projects.mockReturnValue(of([{ id: 1, name: 'Work', color: '#708965' }]));
    api.create.mockReturnValue(of({ ...task, id: 2 }));
    api.update.mockReturnValue(of(task));
    api.trash.mockReturnValue(of(null));
    api.restore.mockReturnValue(of(task));
    api.bulk.mockReturnValue(of([]));
    await TestBed.configureTestingModule({
      imports: [Workspace],
      providers: [
        provideRouter([]),
        { provide: ApiService, useValue: api },
        { provide: AuthService, useValue: { username: 'preview_user' } },
        { provide: ThemeService, useValue: { dark: () => false, toggle: vi.fn() } },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(Workspace);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });
  afterEach(() => fixture.destroy());
  it('renders the authenticated dashboard, task and navigation', () => {
    const text = fixture.nativeElement.textContent;
    expect(text).toContain('Your day, your way.');
    expect(text).toContain('Finish project');
    expect(text).toContain('preview_user');
    expect(component.loading()).toBe(false);
    expect(fixture.nativeElement.querySelectorAll('.task-row').length).toBe(1);
  });
  it('quick-adds trimmed titles to the current project', () => {
    component.navigate('project:Work');
    component.quickTitle = '  A new idea  ';
    component.addQuick();
    expect(api.create).toHaveBeenCalledWith(
      expect.objectContaining({ description: 'A new idea', project: 'Work', targetDate: null }),
    );
    expect(component.quickTitle).toBe('');
    expect(component.toast()).toBe('Task added');
  });
  it('adds today dates when quick-adding in Today', () => {
    component.navigate('today');
    component.quickTitle = 'Today task';
    component.addQuick();
    expect(api.create).toHaveBeenCalledWith(
      expect.objectContaining({ targetDate: component.today() }),
    );
  });
  it('does not send empty titles', () => {
    component.quickTitle = '  ';
    component.addQuick();
    expect(api.create).not.toHaveBeenCalled();
  });
  it('completion keeps the version and metadata', () => {
    component.toggleDone(task);
    expect(api.update).toHaveBeenCalledWith({ ...task, done: true });
  });
  it('soft deletion offers a working undo action', () => {
    component.trash(task);
    expect(api.trash).toHaveBeenCalledWith(1);
    expect(component.undoAction).not.toBeNull();
    component.undo();
    expect(api.restore).toHaveBeenCalledWith(1);
  });
  it('shows server errors and keeps conflict messages after refreshing', () => {
    api.update.mockReturnValue(
      throwError(() => ({ status: 409, error: { message: 'Changed in another window' } })),
    );
    component.toggleDone(task);
    expect(component.error()).toBe('Changed in another window');
    expect(component.modalError()).toBe('Changed in another window');
    expect(component.busy()).toBe(false);
  });
  it('caps bulk selection at 100 and resets it when navigating', () => {
    component.tasks.set(Array.from({ length: 101 }, (_, i) => ({ ...task, id: i + 1 })));
    component.selectAll();
    expect(component.selected().size).toBe(100);
    component.toggleSelection(1);
    expect(component.selected().size).toBe(100);
    component.navigate('inbox');
    expect(component.selected().size).toBe(0);
  });
  it('search and board mode render filtered tasks', () => {
    component.query.set('not found');
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('No matching tasks');
    component.query.set('');
    component.setLayout('board');
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelectorAll('.board-column').length).toBe(3);
    expect(fixture.nativeElement.querySelectorAll('.board-card').length).toBe(1);
  });
});
