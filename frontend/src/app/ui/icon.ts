import { Component, input } from '@angular/core';
const paths: Record<string, string> = {
  sun: 'M12 3v2m0 14v2M3 12h2m14 0h2M5.6 5.6 7 7m10 10 1.4 1.4M5.6 18.4 7 17m10-10 1.4-1.4M16 12a4 4 0 1 1-8 0 4 4 0 0 1 8 0',
  inbox: 'M4 4h16v16H4zM4 13h5l1 3h4l1-3h5',
  calendar: 'M4 6h16v15H4zM8 3v6m8-6v6M4 11h16M8 15h3',
  arrow: 'm9 5 7 7-7 7',
  list: 'M8 6h13M8 12h13M8 18h13M3 6h.01M3 12h.01M3 18h.01',
  check: 'm5 12 4 4L19 6',
  plus: 'M12 5v14M5 12h14',
  search: 'm21 21-5-5M18 10a8 8 0 1 1-16 0 8 8 0 0 1 16 0',
  star: 'm12 3 2.8 5.8 6.4.9-4.6 4.5 1.1 6.4-5.7-3-5.7 3 1.1-6.4-4.6-4.5 6.4-.9z',
  trash: 'M3 6h18M9 6V3h6v3M5 6l1 15h12l1-15M10 10v7m4-7v7',
  folder: 'M3 6h7l2 3h9v11H3z',
  flag: 'M5 21V3h14l-3 5 3 5H5',
  close: 'm6 6 12 12M6 18 18 6',
  more: 'M4 12h.01M12 12h.01M20 12h.01',
  logout: 'M9 3H4v18h5m5-15 6 6-6 6M8 12h12',
  settings:
    'M12 8a4 4 0 1 1 0 8 4 4 0 0 1 0-8M12 2v3m0 14v3M2 12h3m14 0h3M5 5l2 2m10 10 2 2M5 19l2-2M17 7l2-2',
  repeat: 'm17 2 4 4-4 4M3 11V9a3 3 0 0 1 3-3h15M7 22l-4-4 4-4m14-1v2a3 3 0 0 1-3 3H3',
  download: 'M12 3v12m-5-5 5 5 5-5M4 16v5h16v-5',
  upload: 'M12 16V4m-5 5 5-5 5 5M4 16v5h16v-5',
  board: 'M3 3h7v18H3zM14 3h7v12h-7z',
  moon: 'M20 14a8 8 0 0 1-10-10 9 9 0 1 0 10 10',
  menu: 'M3 6h18M3 12h18M3 18h18',
  tag: 'M3 3h8l10 10-8 8L3 11zM7 7h.01',
  undo: 'm7 3-4 4 4 4M3 7h10a7 7 0 1 1 0 14',
  lock: 'M5 10h14v11H5zM8 10V6a4 4 0 0 1 8 0v4',
};
@Component({
  selector: 'app-icon',
  template: `<svg
    viewBox="0 0 24 24"
    fill="none"
    stroke="currentColor"
    stroke-width="1.7"
    stroke-linecap="round"
    stroke-linejoin="round"
    aria-hidden="true"
  >
    <path [attr.d]="path()"></path>
  </svg>`,
  styles: `
    :host {
      display: inline-flex;
      width: 20px;
      height: 20px;
      flex-shrink: 0;
    }
    svg {
      width: 100%;
      height: 100%;
    }
  `,
})
export class Icon {
  name = input('sun');
  path = () => paths[this.name()] ?? paths['list'];
}
