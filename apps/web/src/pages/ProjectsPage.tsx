import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { AgGridReact } from 'ag-grid-react';
import type { ColDef, ICellRendererParams } from 'ag-grid-community';
import { AllCommunityModule, ModuleRegistry } from 'ag-grid-community';
import {
  BarChart3,
  FileCode,
  FolderKanban,
  History,
  Loader2,
  Pencil,
  Play,
  Plus,
  RefreshCw,
  Search,
  SlidersHorizontal,
  Trash2,
} from 'lucide-react';
import { api } from '@/api/client';
import { projectTabPath } from '@/config/projectWorkspace';
import type { DockerStatus, ExecutionStatus, Project, ProjectFormData, RunnerActivity } from '@/types';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { ProjectFormDialog } from '@/components/ProjectFormDialog';
import { EnvVariablesDialog } from '@/components/project/EnvVariablesDialog';
import {
  configuredEnvCount as countConfiguredEnvVariables,
  isRequiredEnvMissing,
  resolveEnvRequirementState,
} from '@/lib/envVariables';
import { confirmPlaywrightVersion } from '@/lib/projectRuntime';
import { cn } from '@/lib/utils';

ModuleRegistry.registerModules([AllCommunityModule]);

type ProjectFilter = 'ALL' | 'RUNNING' | 'TESTING' | 'ERROR' | 'EPHEMERAL';

const dockerStatusLabel: Record<DockerStatus, string> = {
  RUNNING: '실행 중',
  STOPPED: '중지',
  ERROR: '오류',
  NOT_CONFIGURED: '미설정',
};

const dockerStatusColor: Record<DockerStatus, string> = {
  RUNNING: 'bg-success/10 text-success',
  STOPPED: 'bg-muted text-muted-foreground',
  ERROR: 'bg-destructive/10 text-destructive',
  NOT_CONFIGURED: 'bg-muted text-muted-foreground',
};

const runnerActivityLabel: Record<RunnerActivity, string> = {
  IDLE: '대기',
  TESTING: '테스트 중',
  UNAVAILABLE: '중지',
};

const runnerActivityColor: Record<RunnerActivity, string> = {
  IDLE: 'bg-success/10 text-success',
  TESTING: 'bg-primary/10 text-primary',
  UNAVAILABLE: 'bg-muted text-muted-foreground',
};

const serverTypeLabel: Record<Project['serverType'], string> = {
  DEV: '개발',
  TEST: '테스트',
  PROD: '운영',
};

const serverTypeColor: Record<Project['serverType'], string> = {
  DEV: 'bg-sky-100 text-sky-700 dark:bg-sky-500/15 dark:text-sky-300',
  TEST: 'bg-warning/10 text-warning',
  PROD: 'bg-rose-100 text-rose-700 dark:bg-rose-500/15 dark:text-rose-300',
};

const executionStatusLabel: Record<ExecutionStatus, string> = {
  SCHEDULED: '예약',
  PENDING: '대기',
  RUNNING: '실행 중',
  CANCEL_REQUESTED: '중단 중',
  CANCELLED: '중단',
  PASSED: '성공',
  FAILED: '실패',
  ERROR: '오류',
};

const executionStatusColor: Record<ExecutionStatus, string> = {
  SCHEDULED: 'bg-ai-accent/10 text-ai-accent',
  PENDING: 'bg-muted text-muted-foreground',
  RUNNING: 'bg-primary/10 text-primary',
  CANCEL_REQUESTED: 'bg-warning/10 text-warning',
  CANCELLED: 'bg-muted text-muted-foreground',
  PASSED: 'bg-success/10 text-success',
  FAILED: 'bg-destructive/10 text-destructive',
  ERROR: 'bg-orange-100 text-orange-700 dark:bg-orange-500/15 dark:text-orange-300',
};

function configuredEnvCount(project: Project): number {
  return countConfiguredEnvVariables(project.envVariables);
}

function envButtonState(project: Project) {
  return resolveEnvRequirementState(project.loginEnvRequired, project.envVariables);
}

function envButtonTitle(project: Project): string {
  const count = configuredEnvCount(project);
  const state = envButtonState(project);
  if (state === 'required-missing') return '필수 환경변수 입력 필요';
  if (state === 'required-configured') return `필수 환경변수 ${count}개 입력됨`;
  if (state === 'optional-configured') return `환경변수 ${count}개 입력됨`;
  return '환경변수';
}

function isRunnerReady(project: Project) {
  return project.dockerStatus === 'RUNNING'
    || (project.dockerEnabled && project.runnerLifecycle === 'EPHEMERAL');
}

function StatusBadge({
  className,
  children,
}: {
  className: string;
  children: React.ReactNode;
}) {
  return (
    <span className={cn('inline-flex whitespace-nowrap rounded-full px-2 py-0.5 text-xs font-medium', className)}>
      {children}
    </span>
  );
}

function RunnerStatusBadge({ project }: { project: Project }) {
  if (project.dockerStatus === 'RUNNING') {
    const activity = project.runnerActivity ?? 'IDLE';
    return <StatusBadge className={runnerActivityColor[activity]}>{runnerActivityLabel[activity]}</StatusBadge>;
  }

  if (project.dockerEnabled && (project.runnerLifecycle === 'EPHEMERAL' || !project.dockerContainerId)) {
    return <StatusBadge className="bg-muted text-muted-foreground">없음</StatusBadge>;
  }

  return <StatusBadge className={dockerStatusColor[project.dockerStatus]}>{dockerStatusLabel[project.dockerStatus]}</StatusBadge>;
}

function ServerTypeBadge({ serverType }: { serverType: Project['serverType'] }) {
  return <StatusBadge className={serverTypeColor[serverType]}>{serverTypeLabel[serverType]}</StatusBadge>;
}

function LifecycleBadge({ project }: { project: Project }) {
  return (
    <StatusBadge className={project.runnerLifecycle === 'EPHEMERAL' ? 'bg-ai-accent/10 text-ai-accent' : 'bg-muted text-muted-foreground'}>
      {project.runnerLifecycle === 'EPHEMERAL' ? '일회용' : '상주'}
    </StatusBadge>
  );
}

function LatestExecutionBadge({ status }: { status: ExecutionStatus | null }) {
  if (!status) {
    return <span className="text-xs text-muted-foreground">-</span>;
  }
  return <StatusBadge className={executionStatusColor[status]}>{executionStatusLabel[status]}</StatusBadge>;
}

function formatExecutionAt(value: string | null) {
  if (!value) return '-';
  return new Date(value).toLocaleString('ko-KR');
}

function formatDuration(durationMs: number | null) {
  if (durationMs == null) return '-';
  const seconds = durationMs / 1000;
  if (seconds < 60) return `${seconds.toFixed(1)}s`;
  const minutes = Math.floor(seconds / 60);
  const rest = Math.round(seconds % 60);
  return `${minutes}m ${rest}s`;
}

function matchesQuery(project: Project, query: string) {
  const normalized = query.trim().toLowerCase();
  if (!normalized) return true;
  return [
    project.projectId,
    project.projectName,
    project.managerName,
    project.managerContact,
    project.baseUrl,
    project.testPurpose,
  ].some((value) => value?.toLowerCase().includes(normalized));
}

export function ProjectsPage() {
  const navigate = useNavigate();
  const gridRef = useRef<AgGridReact<Project>>(null);
  const [projects, setProjects] = useState<Project[]>([]);
  const [loading, setLoading] = useState(true);
  const [query, setQuery] = useState('');
  const [filter, setFilter] = useState<ProjectFilter>('ALL');
  const [dialogOpen, setDialogOpen] = useState(false);
  const [editProject, setEditProject] = useState<Project | null>(null);
  const [envProject, setEnvProject] = useState<Project | null>(null);
  const [testLoadingId, setTestLoadingId] = useState<string | null>(null);

  const loadProjects = useCallback(async (options?: { silent?: boolean }) => {
    if (!options?.silent) setLoading(true);
    try {
      setProjects(await api.getProjects());
    } catch {
      setProjects([]);
    } finally {
      if (!options?.silent) setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadProjects();
  }, [loadProjects]);

  useEffect(() => {
    const hasActiveExecution = projects.some((project) =>
      project.latestExecutionStatus === 'PENDING'
      || project.latestExecutionStatus === 'RUNNING'
      || project.latestExecutionStatus === 'CANCEL_REQUESTED'
      || project.runnerActivity === 'TESTING'
    );
    if (!hasActiveExecution) return;
    const timer = setInterval(() => loadProjects({ silent: true }), 1500);
    return () => clearInterval(timer);
  }, [projects, loadProjects]);

  const stats = useMemo(() => ({
    total: projects.length,
    ready: projects.filter(isRunnerReady).length,
    testing: projects.filter((project) => project.runnerActivity === 'TESTING').length,
    failed: projects.filter((project) =>
      project.latestExecutionStatus === 'FAILED' || project.latestExecutionStatus === 'ERROR'
    ).length,
    ephemeral: projects.filter((project) => project.runnerLifecycle === 'EPHEMERAL').length,
  }), [projects]);

  const filteredProjects = useMemo(() => projects.filter((project) => {
    if (!matchesQuery(project, query)) return false;
    if (filter === 'RUNNING') return isRunnerReady(project);
    if (filter === 'TESTING') return project.runnerActivity === 'TESTING';
    if (filter === 'ERROR') return project.dockerStatus === 'ERROR'
      || project.latestExecutionStatus === 'FAILED'
      || project.latestExecutionStatus === 'ERROR';
    if (filter === 'EPHEMERAL') return project.runnerLifecycle === 'EPHEMERAL';
    return true;
  }), [projects, query, filter]);

  const handleCreate = async (data: ProjectFormData) => {
    await api.createProject(data);
    setDialogOpen(false);
    loadProjects();
  };

  const handleUpdate = async (data: ProjectFormData) => {
    if (!editProject) return;
    await api.updateProject(editProject.projectId, data);
    setEditProject(null);
    loadProjects();
  };

  const handleEnvSaved = (updated: Project) => {
    replaceProject(updated);
    setEnvProject(updated);
  };

  const handleDelete = async (projectId: string) => {
    const ok = confirm([
      `프로젝트 "${projectId}"를 삭제하시겠습니까?`,
      '',
      '프로젝트 설정, 테스트 파일, 실행 이력과 리포트가 함께 삭제됩니다.',
    ].join('\n'));
    if (!ok) return;
    await api.deleteProject(projectId);
    loadProjects();
  };

  const replaceProject = (updated: Project) => {
    setProjects((prev) => prev.map((project) =>
      project.projectId === updated.projectId ? updated : project
    ));
  };

  const handleRunProject = async (project: Project) => {
    if (isRequiredEnvMissing(project.loginEnvRequired, project.envVariables)) {
      alert('테스트 계정 환경변수가 필요합니다. 환경변수를 먼저 입력하세요.');
      setEnvProject(project);
      return;
    }

    setTestLoadingId(project.projectId);
    try {
      if (!(await confirmPlaywrightVersion(project, '테스트 실행'))) {
        return;
      }
      let runnableProject = project;
      if (!isRunnerReady(runnableProject)) {
        if (runnableProject.dockerEnabled && runnableProject.runnerLifecycle === 'PERSISTENT') {
          runnableProject = await api.startDocker(runnableProject.projectId);
          replaceProject(runnableProject);
        } else {
          alert('Docker Runner를 사용할 수 없습니다. 프로젝트 설정을 확인하세요.');
          return;
        }
      }

      const execution = await api.runTests(runnableProject.projectId);
      setProjects((prev) => prev.map((item) =>
        item.projectId === runnableProject.projectId
          ? {
              ...item,
              runnerActivity: 'TESTING',
              latestExecutionId: execution.id,
              latestExecutionStatus: execution.status,
              latestExecutionAt: execution.startedAt ?? execution.createdAt,
              latestExecutionDurationMs: execution.durationMs,
            }
          : item
      ));
    } catch (err) {
      alert(err instanceof Error ? err.message : '테스트 실행에 실패했습니다.');
    } finally {
      setTestLoadingId(null);
    }
  };

  const columnDefs = useMemo<ColDef<Project>[]>(() => [
    { field: 'displayOrder', headerName: '순서', width: 76, sort: 'asc', pinned: 'left' },
    {
      field: 'projectName',
      headerName: '프로젝트',
      minWidth: 210,
      width: 230,
      pinned: 'left',
      cellRenderer: (p: ICellRendererParams<Project>) => {
        if (!p.data) return null;
        return (
          <button
            type="button"
            className="flex min-w-0 flex-col py-1 text-left"
            onClick={() => navigate(projectTabPath(p.data!.projectId, 'dashboard'))}
          >
            <span className="truncate font-semibold text-foreground hover:text-primary">{p.data.projectName}</span>
            <span className="truncate font-mono text-xs text-muted-foreground">{p.data.projectId}</span>
          </button>
        );
      },
    },
    {
      field: 'serverType',
      headerName: '서버',
      width: 74,
      cellRenderer: (p: ICellRendererParams<Project>) =>
        p.data ? <ServerTypeBadge serverType={p.data.serverType} /> : null,
    },
    {
      headerName: '러너',
      width: 132,
      cellRenderer: (p: ICellRendererParams<Project>) =>
        p.data ? (
          <div className="flex flex-wrap items-center gap-1">
            <RunnerStatusBadge project={p.data} />
            <LifecycleBadge project={p.data} />
          </div>
        ) : null,
    },
    {
      headerName: '테스트',
      width: 184,
      sortable: false,
      filter: false,
      cellRenderer: (p: ICellRendererParams<Project>) => {
        if (!p.data) return null;
        const project = p.data;
        const testing = testLoadingId === project.projectId;
        const envState = envButtonState(project);
        const missingRequiredEnv = envState === 'required-missing';
        return (
          <div className="flex items-center gap-1">
            <button
              type="button"
              className={cn(
                'inline-flex h-7 w-7 items-center justify-center rounded-md border disabled:cursor-not-allowed disabled:opacity-50',
                envState === 'required-missing' && 'border-destructive/30 bg-destructive/10 text-destructive hover:bg-destructive/20',
                envState === 'required-configured' && 'border-success/30 bg-success/10 text-success hover:bg-success/20',
                envState === 'optional-configured' && 'border-primary/30 bg-primary/10 text-primary hover:bg-primary/20',
                envState === 'optional-empty' && 'border-border text-muted-foreground hover:border-primary/30 hover:bg-primary/10 hover:text-primary'
              )}
              onClick={(e) => {
                e.stopPropagation();
                setEnvProject(project);
              }}
              title={envButtonTitle(project)}
            >
              <SlidersHorizontal className="h-3.5 w-3.5" />
            </button>
            <button
              type="button"
              className="inline-flex h-7 w-7 items-center justify-center rounded-md border border-border text-muted-foreground hover:border-primary/30 hover:bg-primary/10 hover:text-primary disabled:cursor-not-allowed disabled:opacity-50"
              onClick={(e) => {
                e.stopPropagation();
                handleRunProject(project);
              }}
              disabled={testing || missingRequiredEnv}
              title={missingRequiredEnv ? '필수 환경변수 입력 후 실행할 수 있습니다' : '전체 테스트 실행'}
            >
              {testing ? <Loader2 className="h-3.5 w-3.5 animate-spin" /> : <Play className="h-3.5 w-3.5" />}
            </button>
            <button
              type="button"
              className="inline-flex h-7 w-7 items-center justify-center rounded-md border border-border text-muted-foreground hover:border-primary/30 hover:bg-primary/10 hover:text-primary"
              onClick={(e) => {
                e.stopPropagation();
                navigate(projectTabPath(project.projectId, 'results'));
              }}
              title="결과"
            >
              <BarChart3 className="h-3.5 w-3.5" />
            </button>
            <button
              type="button"
              className="inline-flex h-7 w-7 items-center justify-center rounded-md border border-border text-muted-foreground hover:border-primary/30 hover:bg-primary/10 hover:text-primary"
              onClick={(e) => {
                e.stopPropagation();
                navigate(projectTabPath(project.projectId, 'runs'));
              }}
              title="실행 이력"
            >
              <History className="h-3.5 w-3.5" />
            </button>
            <button
              type="button"
              className="inline-flex h-7 w-7 items-center justify-center rounded-md border border-border text-muted-foreground hover:border-primary/30 hover:bg-primary/10 hover:text-primary"
              onClick={(e) => {
                e.stopPropagation();
                navigate(projectTabPath(project.projectId, 'source'));
              }}
              title="소스"
            >
              <FileCode className="h-3.5 w-3.5" />
            </button>
          </div>
        );
      },
    },
    {
      headerName: '최근 실행',
      width: 190,
      cellRenderer: (p: ICellRendererParams<Project>) =>
        p.data ? (
          <div className="flex min-w-0 flex-col gap-0.5 py-1">
            <div className="flex items-center gap-2">
              <LatestExecutionBadge status={p.data.latestExecutionStatus} />
              <span className="truncate text-xs text-muted-foreground">{formatDuration(p.data.latestExecutionDurationMs)}</span>
            </div>
            <span className="truncate text-[11px] text-muted-foreground">{formatExecutionAt(p.data.latestExecutionAt)}</span>
          </div>
        ) : null,
    },
    {
      headerName: '실행 환경',
      width: 132,
      cellRenderer: (p: ICellRendererParams<Project>) =>
        p.data ? (
          <div className="flex min-w-0 flex-col gap-0.5 py-1 text-xs">
            <span className="truncate font-medium text-foreground">Node {p.data.nodeVersion}</span>
            <span className="truncate text-[11px] text-muted-foreground">PW {p.data.playwrightVersion} · {p.data.packageManager}</span>
          </div>
        ) : null,
    },
    {
      field: 'baseUrl',
      headerName: 'Base URL',
      minWidth: 160,
      width: 180,
      valueFormatter: (p) => p.value ?? '-',
    },
    {
      field: 'managerName',
      headerName: '관리자',
      width: 112,
      valueFormatter: (p) => p.value ?? '-',
    },
    {
      headerName: '',
      width: 92,
      sortable: false,
      filter: false,
      cellRenderer: (p: ICellRendererParams<Project>) =>
        p.data ? (
          <div className="flex items-center gap-1">
            <button
              type="button"
              className="inline-flex h-7 w-7 items-center justify-center rounded-md text-muted-foreground hover:bg-primary/10 hover:text-primary"
              onClick={(e) => {
                e.stopPropagation();
                setEditProject(p.data!);
              }}
              title="수정"
            >
              <Pencil className="h-4 w-4" />
            </button>
            <button
              type="button"
              className="inline-flex h-7 w-7 items-center justify-center rounded-md text-destructive hover:bg-destructive/10"
              onClick={(e) => {
                e.stopPropagation();
                handleDelete(p.data!.projectId);
              }}
              title="삭제"
            >
              <Trash2 className="h-4 w-4" />
            </button>
          </div>
        ) : null,
    },
  ], [navigate, testLoadingId]);

  const filterButtons: Array<{ id: ProjectFilter; label: string; count: number }> = [
    { id: 'ALL', label: '전체', count: stats.total },
    { id: 'RUNNING', label: '실행 가능', count: stats.ready },
    { id: 'TESTING', label: '테스트 중', count: stats.testing },
    { id: 'ERROR', label: '오류/실패', count: stats.failed },
    { id: 'EPHEMERAL', label: '일회용', count: stats.ephemeral },
  ];

  return (
    <div className="p-6 space-y-4">
      <div className="flex flex-col gap-3 lg:flex-row lg:items-end lg:justify-between">
        <div>
          <h2 className="text-2xl font-bold text-foreground">프로젝트</h2>
          <p className="mt-1 text-sm text-muted-foreground">테스트 프로젝트와 Runner 상태를 관리합니다.</p>
        </div>
        <div className="flex flex-wrap gap-2">
          <Button variant="outline" size="sm" onClick={() => loadProjects()} disabled={loading}>
            <RefreshCw className={cn('h-4 w-4', loading && 'animate-spin')} />
            새로고침
          </Button>
          <Button size="sm" onClick={() => setDialogOpen(true)}>
            <Plus className="h-4 w-4" />
            프로젝트 등록
          </Button>
        </div>
      </div>

      <div className="rounded-lg border border-border bg-card">
        <div className="grid grid-cols-2 divide-x divide-y divide-border md:grid-cols-5 md:divide-y-0">
          {[
            ['전체', stats.total],
            ['실행 가능', stats.ready],
            ['테스트 중', stats.testing],
            ['오류/실패', stats.failed],
            ['일회용', stats.ephemeral],
          ].map(([label, value]) => (
            <div key={label} className="px-4 py-3">
              <div className="text-xs text-muted-foreground">{label}</div>
              <div className="mt-1 text-xl font-semibold text-foreground">{value}</div>
            </div>
          ))}
        </div>
        <div className="flex flex-col gap-3 border-t border-border px-4 py-3 lg:flex-row lg:items-center lg:justify-between">
          <div className="relative max-w-xl flex-1">
            <Search className="pointer-events-none absolute left-3 top-2.5 h-4 w-4 text-muted-foreground" />
            <Input
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              placeholder="프로젝트명, ID, 관리자, URL 검색"
              className="pl-9"
            />
          </div>
          <div className="flex flex-wrap gap-1 rounded-lg bg-muted p-1">
            {filterButtons.map((item) => (
              <button
                key={item.id}
                type="button"
                onClick={() => setFilter(item.id)}
                className={cn(
                  'h-8 rounded-md px-3 text-xs font-medium transition-colors',
                  filter === item.id ? 'bg-primary text-primary-foreground' : 'text-muted-foreground hover:bg-card'
                )}
              >
                {item.label} {item.count}
              </button>
            ))}
          </div>
        </div>
      </div>

      {!loading && filteredProjects.length === 0 ? (
        <div className="flex min-h-[420px] items-center justify-center rounded-lg border border-dashed border-border bg-card">
          <div className="max-w-sm text-center">
            <FolderKanban className="mx-auto h-10 w-10 text-muted-foreground" />
            <h3 className="mt-4 text-lg font-semibold text-foreground">표시할 프로젝트가 없습니다</h3>
            <p className="mt-2 text-sm text-muted-foreground">검색어와 필터를 확인하거나 새 프로젝트를 등록하세요.</p>
            <Button className="mt-4" onClick={() => setDialogOpen(true)}>
              <Plus className="h-4 w-4" />
              프로젝트 등록
            </Button>
          </div>
        </div>
      ) : (
        <div className="ag-theme-playops overflow-hidden rounded-lg border border-border" style={{ height: 600 }}>
          <AgGridReact
            ref={gridRef}
            rowData={filteredProjects}
            columnDefs={columnDefs}
            defaultColDef={{ sortable: true, filter: false, resizable: true }}
            rowHeight={48}
            headerHeight={40}
            animateRows
            theme="legacy"
            rowSelection={{ mode: 'singleRow' }}
            onRowDoubleClicked={(e) => e.data && navigate(projectTabPath(e.data.projectId, 'dashboard'))}
            overlayNoRowsTemplate="표시할 프로젝트가 없습니다"
          />
        </div>
      )}

      <ProjectFormDialog
        open={dialogOpen}
        onOpenChange={setDialogOpen}
        onSubmit={handleCreate}
        title="프로젝트 등록"
      />

      {editProject && (
        <ProjectFormDialog
          open={!!editProject}
          onOpenChange={(open) => !open && setEditProject(null)}
          onSubmit={handleUpdate}
          title="프로젝트 수정"
          initial={editProject}
        />
      )}

      {envProject && (
        <EnvVariablesDialog
          open={!!envProject}
          onOpenChange={(open) => !open && setEnvProject(null)}
          project={envProject}
          onSaved={handleEnvSaved}
        />
      )}
    </div>
  );
}
