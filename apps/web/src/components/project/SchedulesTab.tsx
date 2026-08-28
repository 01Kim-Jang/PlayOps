import { useCallback, useEffect, useState } from 'react';
import { CalendarClock, Loader2, Plus, Trash2 } from 'lucide-react';
import { api } from '@/api/client';
import type { ExecutionSchedule, ExecutionScheduleRequest } from '@/types';
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
import { cn } from '@/lib/utils';

interface SchedulesTabProps {
  projectId: string;
}

const DAY_OPTIONS: { code: string; label: string }[] = [
  { code: 'MON', label: '월' },
  { code: 'TUE', label: '화' },
  { code: 'WED', label: '수' },
  { code: 'THU', label: '목' },
  { code: 'FRI', label: '금' },
  { code: 'SAT', label: '토' },
  { code: 'SUN', label: '일' },
];

function daysLabel(daysOfWeek: string) {
  if (!daysOfWeek || daysOfWeek === '*') return '매일';
  const codes = daysOfWeek.split(',');
  return DAY_OPTIONS.filter((d) => codes.includes(d.code)).map((d) => d.label).join(', ');
}

function timeLabel(hour: number, minute: number) {
  return `${String(hour).padStart(2, '0')}:${String(minute).padStart(2, '0')}`;
}

interface FormState {
  name: string;
  specPath: string;
  time: string;
  days: string[];
  enabled: boolean;
}

const emptyForm: FormState = { name: '', specPath: '', time: '09:00', days: [], enabled: true };

function ScheduleFormDialog({
  open,
  initial,
  onOpenChange,
  onSubmit,
}: {
  open: boolean;
  initial: ExecutionSchedule | null;
  onOpenChange: (open: boolean) => void;
  onSubmit: (data: ExecutionScheduleRequest) => Promise<void>;
}) {
  const [form, setForm] = useState<FormState>(emptyForm);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    if (!open) return;
    if (initial) {
      setForm({
        name: initial.name ?? '',
        specPath: initial.specPath ?? '',
        time: timeLabel(initial.hour, initial.minute),
        days: initial.daysOfWeek === '*' ? [] : initial.daysOfWeek.split(','),
        enabled: initial.enabled,
      });
    } else {
      setForm(emptyForm);
    }
    setError('');
  }, [open, initial]);

  const toggleDay = (code: string) => {
    setForm((prev) => ({
      ...prev,
      days: prev.days.includes(code) ? prev.days.filter((d) => d !== code) : [...prev.days, code],
    }));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    const [hourStr, minuteStr] = form.time.split(':');
    const hour = Number(hourStr);
    const minute = Number(minuteStr);
    if (Number.isNaN(hour) || Number.isNaN(minute)) {
      setError('시각을 입력하세요.');
      return;
    }
    setSaving(true);
    try {
      await onSubmit({
        name: form.name.trim() || undefined,
        specPath: form.specPath.trim() || undefined,
        hour,
        minute,
        daysOfWeek: form.days.length === 0 ? '*' : form.days.join(','),
        enabled: form.enabled,
      });
    } catch (err) {
      setError(err instanceof Error ? err.message : '저장 실패');
    } finally {
      setSaving(false);
    }
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{initial ? '예약 수정' : '예약 추가'}</DialogTitle>
          <DialogDescription>지정한 요일/시각에 자동으로 테스트를 실행합니다. (서버 로컬 시각 기준)</DialogDescription>
        </DialogHeader>
        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="space-y-2">
            <Label>이름 (선택)</Label>
            <Input
              value={form.name}
              onChange={(e) => setForm((prev) => ({ ...prev, name: e.target.value }))}
              placeholder="예: 매일 야간 회귀 테스트"
            />
          </div>
          <div className="space-y-2">
            <Label>대상 spec 경로 (비우면 전체 실행)</Label>
            <Input
              value={form.specPath}
              onChange={(e) => setForm((prev) => ({ ...prev, specPath: e.target.value }))}
              placeholder="tests/login.spec.ts"
              className="font-mono text-xs"
            />
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-2">
              <Label>시각</Label>
              <Input
                type="time"
                value={form.time}
                onChange={(e) => setForm((prev) => ({ ...prev, time: e.target.value }))}
                required
              />
            </div>
            <div className="space-y-2">
              <Label>활성화</Label>
              <label className="flex items-center gap-2 h-9">
                <input
                  type="checkbox"
                  checked={form.enabled}
                  onChange={(e) => setForm((prev) => ({ ...prev, enabled: e.target.checked }))}
                />
                <span className="text-sm text-muted-foreground">즉시 활성화</span>
              </label>
            </div>
          </div>
          <div className="space-y-2">
            <Label>요일 (선택 안 하면 매일)</Label>
            <div className="flex flex-wrap gap-2">
              {DAY_OPTIONS.map((d) => (
                <button
                  key={d.code}
                  type="button"
                  onClick={() => toggleDay(d.code)}
                  className={cn(
                    'h-8 w-10 rounded-md border text-xs font-semibold transition-colors',
                    form.days.includes(d.code)
                      ? 'border-primary bg-primary/10 text-primary'
                      : 'border-border bg-card text-muted-foreground hover:border-primary/40'
                  )}
                >
                  {d.label}
                </button>
              ))}
            </div>
          </div>
          {error && <p className="text-sm text-destructive">{error}</p>}
          <div className="flex justify-end gap-2">
            <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
              취소
            </Button>
            <Button type="submit" disabled={saving}>
              {saving ? '저장 중...' : '저장'}
            </Button>
          </div>
        </form>
      </DialogContent>
    </Dialog>
  );
}

export function SchedulesTab({ projectId }: SchedulesTabProps) {
  const [schedules, setSchedules] = useState<ExecutionSchedule[]>([]);
  const [loading, setLoading] = useState(true);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [editing, setEditing] = useState<ExecutionSchedule | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      setSchedules(await api.schedules.listByProject(projectId));
    } catch {
      setSchedules([]);
    } finally {
      setLoading(false);
    }
  }, [projectId]);

  useEffect(() => {
    load();
  }, [load]);

  const handleSubmit = async (data: ExecutionScheduleRequest) => {
    if (editing) {
      await api.schedules.update(projectId, editing.id, data);
    } else {
      await api.schedules.create(projectId, data);
    }
    setDialogOpen(false);
    setEditing(null);
    load();
  };

  const handleToggleEnabled = async (schedule: ExecutionSchedule) => {
    await api.schedules.update(projectId, schedule.id, {
      name: schedule.name,
      specPath: schedule.specPath,
      hour: schedule.hour,
      minute: schedule.minute,
      daysOfWeek: schedule.daysOfWeek,
      enabled: !schedule.enabled,
    });
    load();
  };

  const handleDelete = async (schedule: ExecutionSchedule) => {
    if (!confirm(`'${schedule.name || timeLabel(schedule.hour, schedule.minute)}' 예약을 삭제하시겠습니까?`)) return;
    await api.schedules.remove(projectId, schedule.id);
    load();
  };

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <div>
          <h3 className="font-medium text-foreground">예약 실행</h3>
          <p className="text-xs text-muted-foreground mt-0.5">
            지정한 요일/시각에 서버가 자동으로 테스트를 실행합니다. 실패/오류 시 Slack 알림 설정과 동일하게 알립니다.
          </p>
        </div>
        <Button
          size="sm"
          onClick={() => {
            setEditing(null);
            setDialogOpen(true);
          }}
        >
          <Plus className="h-4 w-4" />
          예약 추가
        </Button>
      </div>

      {loading ? (
        <div className="flex items-center justify-center py-10">
          <Loader2 className="h-6 w-6 animate-spin text-muted-foreground" />
        </div>
      ) : schedules.length === 0 ? (
        <div className="rounded-xl border border-dashed border-border p-10 text-center text-sm text-muted-foreground">
          등록된 예약이 없습니다.
        </div>
      ) : (
        <div className="space-y-2">
          {schedules.map((schedule) => (
            <div
              key={schedule.id}
              className="flex items-center justify-between gap-4 rounded-lg border border-border bg-card p-3"
            >
              <div className="flex items-center gap-3 min-w-0">
                <CalendarClock className={cn('h-4 w-4 flex-shrink-0', schedule.enabled ? 'text-primary' : 'text-muted-foreground')} />
                <div className="min-w-0">
                  <div className="flex items-center gap-2">
                    <span className="text-sm font-semibold text-foreground truncate">
                      {schedule.name || '(이름 없음)'}
                    </span>
                    <span className="text-xs text-muted-foreground whitespace-nowrap">
                      {daysLabel(schedule.daysOfWeek)} {timeLabel(schedule.hour, schedule.minute)}
                    </span>
                  </div>
                  <p className="text-xs text-muted-foreground font-mono truncate">
                    {schedule.specPath || '전체 실행'}
                    {schedule.lastTriggeredAt && (
                      <span className="ml-2 text-muted-foreground">
                        · 마지막 실행: {new Date(schedule.lastTriggeredAt).toLocaleString('ko-KR')}
                      </span>
                    )}
                  </p>
                </div>
              </div>
              <div className="flex items-center gap-1.5 flex-shrink-0">
                <button
                  type="button"
                  onClick={() => handleToggleEnabled(schedule)}
                  className={cn(
                    'text-xs font-medium px-2 py-1 rounded-full',
                    schedule.enabled ? 'bg-success/10 text-success' : 'bg-muted text-muted-foreground'
                  )}
                >
                  {schedule.enabled ? '활성' : '비활성'}
                </button>
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => {
                    setEditing(schedule);
                    setDialogOpen(true);
                  }}
                >
                  수정
                </Button>
                <button
                  type="button"
                  className="text-destructive hover:text-destructive/80 p-1.5"
                  onClick={() => handleDelete(schedule)}
                >
                  <Trash2 className="h-4 w-4" />
                </button>
              </div>
            </div>
          ))}
        </div>
      )}

      <ScheduleFormDialog
        open={dialogOpen}
        initial={editing}
        onOpenChange={(open) => {
          setDialogOpen(open);
          if (!open) setEditing(null);
        }}
        onSubmit={handleSubmit}
      />
    </div>
  );
}
