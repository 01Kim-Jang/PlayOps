import Editor from '@monaco-editor/react';
import { Loader2 } from 'lucide-react';
import { useMemo } from 'react';

function languageFromPath(path: string | null): string {
  if (!path) return 'plaintext';
  if (path.endsWith('.ts') || path.endsWith('.tsx')) return 'typescript';
  if (path.endsWith('.js') || path.endsWith('.jsx')) return 'javascript';
  if (path.endsWith('.json')) return 'json';
  if (path.endsWith('.yml') || path.endsWith('.yaml')) return 'yaml';
  if (path.endsWith('.md')) return 'markdown';
  if (path.endsWith('.html')) return 'html';
  if (path.endsWith('.css')) return 'css';
  return 'plaintext';
}

interface MonacoEditorProps {
  path: string | null;
  value: string;
  onChange: (value: string) => void;
  readOnly?: boolean;
  loading?: boolean;
}

export function MonacoEditor({ path, value, onChange, readOnly, loading }: MonacoEditorProps) {
  const language = useMemo(() => languageFromPath(path), [path]);

  if (loading) {
    return (
      <div className="flex-1 flex items-center justify-center min-h-[420px] bg-[#1e1e1e]">
        <Loader2 className="h-6 w-6 animate-spin text-slate-400" />
      </div>
    );
  }

  if (!path) {
    return (
      <div className="flex-1 flex items-center justify-center min-h-[420px] bg-muted text-muted-foreground text-sm">
        왼쪽 트리에서 파일을 선택하세요
      </div>
    );
  }

  return (
    <Editor
      height="100%"
      language={language}
      value={value}
      onChange={(v) => onChange(v ?? '')}
      theme="vs-dark"
      options={{
        readOnly: readOnly ?? false,
        minimap: { enabled: false },
        fontSize: 13,
        lineNumbers: 'on',
        scrollBeyondLastLine: false,
        automaticLayout: true,
        tabSize: 2,
        wordWrap: 'on',
      }}
    />
  );
}
