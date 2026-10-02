import { dateKey, filterTasks, newTask, Task } from './task';
describe('Task views', () => {
  const today = '2026-10-02';
  const make = (id: number, properties: Partial<Task> = {}): Task => ({
    ...newTask(),
    id,
    description: 'Task ' + id,
    ...properties,
  });
  const tasks = [
    make(1),
    make(2, { targetDate: today, priority: 'HIGH', starred: true, tags: 'work, focus' }),
    make(3, { targetDate: '2026-10-01', project: 'Work', notes: 'Launch notes' }),
    make(4, {
      targetDate: '2026-10-03',
      subtasks: [{ id: 'a', title: 'Review proposal', done: false }],
    }),
    make(5, { done: true, completedAt: '2026-10-02T10:00:00Z' }),
    make(6, { deletedAt: '2026-10-02T10:00:00Z' }),
  ];
  const ids = (view: string, search = '', priority = '', status = 'active', tag = '') =>
    filterTasks(tasks, view, search, priority, status, tag, today).map((t) => t.id);
  it('separates active, completed and trashed tasks', () => {
    expect(ids('all')).toEqual([1, 2, 3, 4]);
    expect(ids('completed')).toEqual([5]);
    expect(ids('trash')).toEqual([6]);
    expect(ids('all', '', '', 'all')).toEqual([1, 2, 3, 4, 5]);
  });
  it('includes overdue tasks in today and only future dates in upcoming', () => {
    expect(ids('today')).toEqual([2, 3]);
    expect(ids('upcoming')).toEqual([4]);
  });
  it('supports inbox, projects and starred views', () => {
    expect(ids('project:Work')).toEqual([3]);
    expect(ids('starred')).toEqual([2]);
    expect(ids('inbox')).toEqual([1, 2, 4]);
  });
  it('searches notes and subtasks without case sensitivity', () => {
    expect(ids('all', ' LAUNCH ')).toEqual([3]);
    expect(ids('all', 'proposal')).toEqual([4]);
  });
  it('combines exact tags with priority and status', () => {
    expect(ids('all', '', 'HIGH', 'active', 'focus')).toEqual([2]);
    expect(ids('all', '', 'LOW', 'active', 'focus')).toEqual([]);
    expect(ids('all', '', '', 'active', 'wor')).toEqual([]);
  });
  it('creates independent drafts with no due date', () => {
    const a = newTask(),
      b = newTask();
    a.subtasks.push({ id: 'a', title: 'Private', done: false });
    expect(b.subtasks).toEqual([]);
    expect(b.targetDate).toBeNull();
    expect(b.priority).toBe('MEDIUM');
  });
  it('uses the local calendar date rather than UTC for date keys', () => {
    expect(dateKey(new Date(2026, 9, 2, 23, 30))).toBe(today);
  });
});
