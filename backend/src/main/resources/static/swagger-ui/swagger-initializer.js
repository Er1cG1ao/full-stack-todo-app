window.ui = SwaggerUIBundle({
  url: '/openapi.yaml',
  dom_id: '#swagger-ui',
  presets: [SwaggerUIBundle.presets.apis],
  layout: 'BaseLayout',
  deepLinking: true,
  docExpansion: 'list',
  defaultModelsExpandDepth: 1,
  tryItOutEnabled: true,
  persistAuthorization: false,
  displayRequestDuration: true,
  requestInterceptor: async (request) => {
    const url = new URL(request.url, window.location.href);
    const method = (request.method || 'GET').toUpperCase();
    if (url.origin !== window.location.origin) return request;
    request.credentials = 'same-origin';
    if (!['GET', 'HEAD', 'OPTIONS', 'TRACE'].includes(method)) {
      const response = await fetch('/auth/csrf', { credentials: 'same-origin' });
      if (!response.ok) throw new Error('Unable to obtain a CSRF token');
      const csrf = await response.json();
      request.headers = request.headers || {};
      request.headers[csrf.headerName] = csrf.token;
    }
    return request;
  }
});
