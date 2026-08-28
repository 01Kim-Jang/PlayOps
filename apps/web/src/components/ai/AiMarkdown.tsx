import Markdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import type { Components } from 'react-markdown';
import { cn } from '@/lib/utils';

/**
 * AI 답변(Markdown) 렌더러.
 * 채팅 말풍선(text-xs) 안에 들어가는 것을 전제로 크기를 맞췄다.
 * 프로젝트에 typography 플러그인이 없어 클래스는 직접 지정한다.
 */
const components: Components = {
  h1: ({ children }) => <h1 className="mt-3 mb-1.5 text-sm font-bold first:mt-0">{children}</h1>,
  h2: ({ children }) => <h2 className="mt-3 mb-1.5 text-[13px] font-bold first:mt-0">{children}</h2>,
  h3: ({ children }) => <h3 className="mt-2.5 mb-1 text-xs font-bold first:mt-0">{children}</h3>,
  h4: ({ children }) => <h4 className="mt-2.5 mb-1 text-xs font-semibold first:mt-0">{children}</h4>,
  h5: ({ children }) => <h5 className="mt-2 mb-1 text-xs font-semibold first:mt-0">{children}</h5>,
  h6: ({ children }) => <h6 className="mt-2 mb-1 text-xs font-semibold first:mt-0">{children}</h6>,
  p: ({ children }) => <p className="my-1.5 leading-relaxed first:mt-0 last:mb-0">{children}</p>,
  ul: ({ children }) => <ul className="my-1.5 list-disc space-y-0.5 pl-4 first:mt-0 last:mb-0">{children}</ul>,
  ol: ({ children }) => <ol className="my-1.5 list-decimal space-y-0.5 pl-4 first:mt-0 last:mb-0">{children}</ol>,
  li: ({ children }) => <li className="leading-relaxed">{children}</li>,
  strong: ({ children }) => <strong className="font-bold">{children}</strong>,
  em: ({ children }) => <em className="italic">{children}</em>,
  blockquote: ({ children }) => (
    <blockquote className="my-1.5 border-l-2 border-border pl-2 text-muted-foreground first:mt-0 last:mb-0">
      {children}
    </blockquote>
  ),
  hr: () => <hr className="my-2 border-border" />,
  a: ({ href, children }) => (
    <a
      href={href}
      target="_blank"
      rel="noreferrer noopener"
      className="font-medium underline underline-offset-2 hover:opacity-80"
    >
      {children}
    </a>
  ),
  // 인라인 코드 기본 스타일. 코드블록(pre) 안에서는 pre 쪽에서 초기화한다.
  code: ({ children, className }) => (
    <code className={cn('rounded bg-muted px-1 font-mono text-[0.95em]', className)}>{children}</code>
  ),
  pre: ({ children }) => (
    <pre className="my-1.5 overflow-x-auto rounded-lg bg-muted p-2 font-mono text-[11px] leading-relaxed first:mt-0 last:mb-0 [&>code]:block [&>code]:bg-transparent [&>code]:p-0 [&>code]:text-[11px]">
      {children}
    </pre>
  ),
  table: ({ children }) => (
    <div className="my-1.5 overflow-x-auto first:mt-0 last:mb-0">
      <table className="w-full border-collapse border border-border text-[11px]">{children}</table>
    </div>
  ),
  thead: ({ children }) => <thead className="bg-muted">{children}</thead>,
  th: ({ children }) => (
    <th className="border border-border px-2 py-1 text-left font-semibold">{children}</th>
  ),
  td: ({ children }) => <td className="border border-border px-2 py-1 align-top">{children}</td>,
};

export function AiMarkdown({ content, className }: { content: string; className?: string }) {
  return (
    <div className={cn('break-words', className)}>
      <Markdown remarkPlugins={[remarkGfm]} components={components}>
        {content}
      </Markdown>
    </div>
  );
}
