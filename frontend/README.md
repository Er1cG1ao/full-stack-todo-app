# Todo App Frontend

Angular frontend for the full-stack Todo application.

## Requirements

- Node.js 22.22.3+, 24.15.0+, or 26+
- npm 11+
- The companion backend running at `http://localhost:8080`

## Run

```bash
npm ci
npm start
```

Open `http://localhost:4200` and use the demo credentials `alice` / `dummy`.

## Verify

```bash
npm test -- --watch=false
npm run build
```

The application uses client-side rendering because its login state is stored in browser
session storage.
