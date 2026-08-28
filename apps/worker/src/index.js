const apiBaseUrl = process.env.API_BASE_URL ?? 'http://api:8080';
const projectsRoot = process.env.PROJECTS_ROOT ?? '/playwright-projects';

console.log('[playops-worker] started');
console.log(`[playops-worker] apiBaseUrl=${apiBaseUrl}`);
console.log(`[playops-worker] projectsRoot=${projectsRoot}`);

setInterval(() => {
  console.log('[playops-worker] polling execution queue...');
}, 30_000);
