import { Ban, ChevronDown, ChevronRight, File, Folder, FolderOpen } from 'lucide-react';
import { useMemo, useState } from 'react';
import { cn } from '@/lib/utils';

interface TreeNode {
  name: string;
  path: string;
  children: TreeNode[];
  isFile: boolean;
  restricted: boolean;
}

const HIDDEN_TREE_SEGMENTS = new Set([
  '.git',
  '.hg',
  '.svn',
  '.cache',
  '.gradle',
  '.idea',
  '.next',
  '.playwright',
  '.turbo',
  '.vscode',
  'build',
  'coverage',
  'dist',
  'node_modules',
  'out',
  'playwright-report',
  'target',
  'test-results',
]);

function isHiddenTreePath(path: string) {
  return path
    .replaceAll('\\', '/')
    .split('/')
    .filter(Boolean)
    .some((segment) => HIDDEN_TREE_SEGMENTS.has(segment));
}

function buildTree(paths: string[], isRestrictedFile?: (path: string) => boolean): TreeNode[] {
  const root: TreeNode[] = [];

  for (const filePath of paths) {
    if (isHiddenTreePath(filePath)) continue;
    const parts = filePath.split('/');
    let current = root;

    for (let i = 0; i < parts.length; i++) {
      const part = parts[i];
      const isFile = i === parts.length - 1;
      const path = parts.slice(0, i + 1).join('/');
      let node = current.find((n) => n.name === part);

      if (!node) {
        node = {
          name: part,
          path,
          children: [],
          isFile,
          restricted: isFile ? Boolean(isRestrictedFile?.(path)) : false,
        };
        current.push(node);
      }
      current = node.children;
    }
  }

  const sortNodes = (nodes: TreeNode[]) => {
    nodes.sort((a, b) => {
      if (a.isFile !== b.isFile) return a.isFile ? 1 : -1;
      return a.name.localeCompare(b.name);
    });
    nodes.forEach((n) => sortNodes(n.children));
  };
  sortNodes(root);
  return root;
}

function TreeItem({
  node,
  depth,
  onFileClick,
  selectedPath,
}: {
  node: TreeNode;
  depth: number;
  onFileClick?: (path: string) => void;
  selectedPath?: string | null;
}) {
  const [open, setOpen] = useState(depth < 2);

  if (node.isFile) {
    const selected = selectedPath === node.path;
    const restricted = node.restricted;
    return (
      <button
        type="button"
        onClick={() => {
          if (!restricted) onFileClick?.(node.path);
        }}
        disabled={restricted}
        title={restricted ? '보기 금지 파일' : node.path}
        className={cn(
          'flex w-full items-center gap-1.5 py-1 text-sm rounded px-1 cursor-pointer text-left',
          selected && !restricted && 'bg-primary/15 text-primary',
          restricted && 'cursor-not-allowed text-muted-foreground opacity-80',
          !selected && !restricted && 'text-muted-foreground hover:bg-accent'
        )}
        style={{ paddingLeft: depth * 16 + 4 }}
      >
        {restricted ? (
          <Ban className="h-3.5 w-3.5 text-destructive shrink-0" />
        ) : (
          <File className="h-3.5 w-3.5 text-muted-foreground shrink-0" />
        )}
        <span className="truncate">{node.name}</span>
        {restricted && <span className="ml-auto text-[10px] text-muted-foreground shrink-0">보기 금지</span>}
      </button>
    );
  }

  return (
    <div>
      <button
        type="button"
        onClick={() => setOpen(!open)}
        className="flex w-full items-center gap-1.5 py-1 text-sm text-foreground hover:bg-accent rounded px-1 cursor-pointer"
        style={{ paddingLeft: depth * 16 + 4 }}
      >
        {open ? (
          <ChevronDown className="h-3.5 w-3.5 text-muted-foreground shrink-0" />
        ) : (
          <ChevronRight className="h-3.5 w-3.5 text-muted-foreground shrink-0" />
        )}
        {open ? (
          <FolderOpen className="h-3.5 w-3.5 text-amber-500 shrink-0" />
        ) : (
          <Folder className="h-3.5 w-3.5 text-amber-500 shrink-0" />
        )}
        <span className="font-medium truncate">{node.name}</span>
      </button>
      {open &&
        node.children.map((child) => (
          <TreeItem
            key={child.path}
            node={child}
            depth={depth + 1}
            onFileClick={onFileClick}
            selectedPath={selectedPath}
          />
        ))}
    </div>
  );
}

interface FileTreeProps {
  files: string[];
  className?: string;
  onFileClick?: (path: string) => void;
  selectedPath?: string | null;
  isRestrictedFile?: (path: string) => boolean;
}

export function FileTree({ files, className, onFileClick, selectedPath, isRestrictedFile }: FileTreeProps) {
  const visibleFiles = useMemo(() => files.filter((file) => !isHiddenTreePath(file)), [files]);
  const tree = useMemo(() => buildTree(visibleFiles, isRestrictedFile), [visibleFiles, isRestrictedFile]);

  if (visibleFiles.length === 0) {
    return (
      <div className={cn('text-sm text-muted-foreground py-8 text-center', className)}>
        업로드된 파일이 없습니다
      </div>
    );
  }

  return (
    <div className={cn('overflow-auto', className)}>
      {tree.map((node) => (
        <TreeItem
          key={node.path}
          node={node}
          depth={0}
          onFileClick={onFileClick}
          selectedPath={selectedPath}
        />
      ))}
    </div>
  );
}
