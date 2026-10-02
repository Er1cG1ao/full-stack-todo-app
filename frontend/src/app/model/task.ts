export type Priority = 'LOW' | 'MEDIUM' | 'HIGH';
export type Recurrence = 'NONE' | 'DAILY' | 'WEEKLY' | 'MONTHLY';
export interface Subtask {
  id: string;
  title: string;
  done: boolean;
}
export interface Task {
  id: number;
  description: string;
  targetDate: string | null;
  done: boolean;
  notes: string;
  priority: Priority;
  project: string;
  tags: string;
  recurrence: Recurrence;
  starred: boolean;
  subtasks: Subtask[];
  version: number;
  createdAt: string;
  updatedAt: string;
  completedAt: string | null;
  deletedAt: string | null;
}
export interface Project {
  id: number;
  name: string;
  color: string;
}
export interface Backup {
  formatVersion: number;
  exportedAt: string;
  username: string;
  tasks: Task[];
  projects: Pick<Project, 'name' | 'color'>[];
}
export function dateKey(date = new Date()): string {
  return [
    date.getFullYear(),
    String(date.getMonth() + 1).padStart(2, '0'),
    String(date.getDate()).padStart(2, '0'),
  ].join('-');
}
export function newTask(project = '', date: string | null = null): Task {
  return {
    id: 0,
    description: '',
    targetDate: date,
    done: false,
    notes: '',
    priority: 'MEDIUM',
    project,
    tags: '',
    recurrence: 'NONE',
    starred: false,
    subtasks: [],
    version: 0,
    createdAt: '',
    updatedAt: '',
    completedAt: null,
    deletedAt: null,
  };
}
export function filterTasks(
  tasks: Task[],
  view: string,
  search: string,
  priority: string,
  status: string,
  tag: string,
  today: string,
): Task[] {
  return tasks.filter((task) => {
    if (view === 'trash' ? !task.deletedAt : !!task.deletedAt) return false;
    if (
      view !== 'trash' &&
      view !== 'completed' &&
      status !== 'all' &&
      task.done !== (status === 'done')
    )
      return false;
    if (view === 'completed' && !task.done) return false;
    if (view === 'inbox' && task.project) return false;
    if (view === 'today' && (!task.targetDate || task.targetDate > today)) return false;
    if (view === 'upcoming' && (!task.targetDate || task.targetDate <= today)) return false;
    if (view === 'starred' && !task.starred) return false;
    if (view.startsWith('project:') && task.project !== view.slice(8)) return false;
    if (priority && task.priority !== priority) return false;
    if (
      tag &&
      !task.tags
        .split(',')
        .map((t) => t.trim())
        .includes(tag)
    )
      return false;
    const haystack = [
      task.description,
      task.notes,
      task.tags,
      task.project,
      ...task.subtasks.map((s) => s.title),
    ]
      .join(' ')
      .toLowerCase();
    return !search.trim() || haystack.includes(search.trim().toLowerCase());
  });
}
