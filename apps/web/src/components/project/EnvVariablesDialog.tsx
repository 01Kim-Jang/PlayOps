import { useEffect, useState } from 'react';
import { ClipboardPaste, Loader2, Plus, Trash2 } from 'lucide-react';
import type { Project } from '@/types';
import { api } from '@/api/client';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Label } from '@/components/ui/Label';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/Dialog';
import {
  EnvEntry,
  isSecretEnvKey,
  parseEnvVariablesText,
  parseEnvVariablesJson,
  serializeEnvVariables,
  SUGGESTED_ENV_KEYS,
} from '@/lib/envVariables';

interface EnvVariablesDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  project: Project;
  onSaved: (project: Project) => void;
}

function emptyRow(): EnvEntry {
  return { key: '', value: '' };
}

export function EnvVariablesDialog({
  open,
  onOpenChange,
  project,
  onSaved,
}: EnvVariablesDialogProps) {
  const [entries, setEntries] = useState<EnvEntry[]>([emptyRow()]);
  const [baseUrl, setBaseUrl] = useState('');
  const [bulkText, setBulkText] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    if (!open) return;
    const parsed = parseEnvVariablesJson(project.envVariables);
    setEntries(parsed.length > 0 ? parsed : [emptyRow()]);
    setBaseUrl(project.baseUrl ?? '');
    setBulkText('');
    setError('');
  }, [open, project]);

  const updateEntry = (index: number, patch: Partial<EnvEntry>) => {
    setEntries((prev) => prev.map((row, i) => (i === index ? { ...row, ...patch } : row)));
  };

  const addRow = (key = '') => {
    setEntries((prev) => [...prev, { key, value: '' }]);
  };

  const addSuggested = (key: string) => {
    if (entries.some((e) => e.key === key)) return;
    addRow(key);
  };

  const removeRow = (index: number) => {
    setEntries((prev) => (prev.length <= 1 ? [emptyRow()] : prev.filter((_, i) => i !== index)));
  };

  const applyBulkText = () => {
    const parsed = parseEnvVariablesText(bulkText);
    if (parsed.length === 0) {
      setError('붙여넣은 내용에서 환경변수를 찾지 못했습니다. JSON 객체 또는 KEY=VALUE 형식으로 입력하세요.');
      return;
    }
    setEntries(parsed);
    setBulkText('');
    setError('');
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    const envJson = serializeEnvVariables(entries);
    try {
      JSON.parse(envJson);
    } catch {
      setError('환경변수 형식이 올바르지 않습니다.');
      return;
    }
    setLoading(true);
    try {
      const updated = await api.updateProject(project.projectId, {
        envVariables: envJson,
        baseUrl: baseUrl.trim() || '',
      });
      onSaved(updated);
      onOpenChange(false);
    } catch (err) {
      setError(err instanceof Error ? err.message : '저장에 실패했습니다.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-xl">
        <DialogHeader>
          <DialogTitle>환경변수</DialogTitle>
          <DialogDescription>
            테스트 실행 시 Playwright 컨테이너에 전달되며, 저장 후 다음 실행부터 반영됩니다. Runner 재시작은
            필요하지 않습니다. 로그인 계정 변수명은 프로젝트 테스트 코드에서 사용하는 이름에 맞춰 등록하세요.
          </DialogDescription>
        </DialogHeader>

        <form onSubmit={handleSubmit} className="space-y-4">
          {project.loginEnvRequired && (
            <div className="rounded-lg border border-warning/30 bg-warning/10 px-3 py-2 text-sm text-warning">
              이 프로젝트는 로그인 계정 환경변수를 사용할 수 있습니다. 변수명은 프로젝트마다 다를 수 있으며,
              값이 없어도 저장은 가능합니다.
            </div>
          )}

          <div className="space-y-2">
            <Label>Base URL</Label>
            <Input
              value={baseUrl}
              onChange={(e) => setBaseUrl(e.target.value)}
              placeholder="https://example.com"
            />
            <p className="text-xs text-muted-foreground">
              실행 시 <span className="font-mono">BASE_URL</span>로 전달됩니다 (아래 JSON의 BASE_URL보다 우선).
            </p>
          </div>

          <div className="space-y-2">
            <div className="flex items-center justify-between">
              <Label>변수</Label>
              <Button type="button" size="sm" variant="outline" onClick={() => addRow()}>
                <Plus className="h-3 w-3 mr-1" />
                행 추가
              </Button>
            </div>
            <div className="flex flex-wrap gap-1">
              {SUGGESTED_ENV_KEYS.filter((k) => k !== 'BASE_URL').map((key) => (
                <button
                  key={key}
                  type="button"
                  className="text-xs px-2 py-0.5 rounded border border-border bg-muted hover:bg-accent font-mono"
                  onClick={() => addSuggested(key)}
                >
                  + {key}
                </button>
              ))}
            </div>
            <div className="rounded-lg border border-border bg-muted p-3 space-y-2">
              <div className="flex items-center justify-between gap-2">
                <Label>.env / JSON 붙여넣기</Label>
                <Button
                  type="button"
                  size="sm"
                  variant="outline"
                  onClick={applyBulkText}
                  disabled={!bulkText.trim()}
                >
                  <ClipboardPaste className="h-3 w-3 mr-1" />
                  적용
                </Button>
              </div>
              <textarea
                className="flex min-h-[96px] w-full rounded-md border border-border bg-card px-3 py-2 font-mono text-xs focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary"
                value={bulkText}
                onChange={(e) => setBulkText(e.target.value)}
                placeholder={'KEY=value\n# comment\n또는 {"KEY":"value"}'}
                spellCheck={false}
              />
            </div>
            <div className="max-h-[280px] overflow-y-auto space-y-2 rounded-lg border border-border p-3">
              {entries.map((row, index) => (
                <div key={index} className="flex gap-2 items-start">
                  <Input
                    className="font-mono text-xs flex-1"
                    placeholder="KEY"
                    value={row.key}
                    onChange={(e) => updateEntry(index, { key: e.target.value })}
                  />
                  <Input
                    className="font-mono text-xs flex-[1.5]"
                    placeholder="value"
                    type={isSecretEnvKey(row.key) ? 'password' : 'text'}
                    value={row.value}
                    onChange={(e) => updateEntry(index, { value: e.target.value })}
                    autoComplete="off"
                  />
                  <Button
                    type="button"
                    size="sm"
                    variant="ghost"
                    className="shrink-0 px-2"
                    onClick={() => removeRow(index)}
                    aria-label="삭제"
                  >
                    <Trash2 className="h-4 w-4 text-muted-foreground" />
                  </Button>
                </div>
              ))}
            </div>
          </div>

          {error && <p className="text-sm text-destructive">{error}</p>}

          <div className="flex justify-end gap-2">
            <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
              취소
            </Button>
            <Button type="submit" disabled={loading}>
              {loading ? (
                <>
                  <Loader2 className="h-4 w-4 animate-spin mr-1" />
                  저장 중...
                </>
              ) : (
                '저장'
              )}
            </Button>
          </div>
        </form>
      </DialogContent>
    </Dialog>
  );
}
