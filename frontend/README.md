# Daylight frontend

Angular standalone components, signals, strict TypeScript/templates and native dialogs.
Styling uses local CSS and SVG icons; no remote fonts, trackers or image services.

Use Node.js 24.19.0 and npm 11.16.0:

    npm ci
    npm start
    npm run check

The backend must be running on 8080. See proxy.conf.json for development routing,
nginx.conf for container routing, and the [root README](../README.md) for the full setup.
npm run check checks formatting, unit/component tests and the production build.
The workspace is lazy-loaded after authentication.
