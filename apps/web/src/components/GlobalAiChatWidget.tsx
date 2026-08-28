import { useState } from 'react';
import { Bot, Loader2, MessageCircle, Send, Sparkles, X } from 'lucide-react';
import { api } from '@/api/client';
import { cn } from '@/lib/utils';

interface ChatMessage {
  role: 'user' | 'assistant';
  content: string;
}

export function GlobalAiChatWidget() {
  const [open, setOpen] = useState(false);
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [question, setQuestion] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSend = async (e: React.FormEvent) => {
    e.preventDefault();
    const q = question.trim();
    if (!q || loading) return;

    setQuestion('');
    setMessages((prev) => [...prev, { role: 'user', content: q }]);
    setLoading(true);
    try {
      const res = await api.ai.chat({ question: q, userLevel: 'JUNIOR' });
      setMessages((prev) => [...prev, { role: 'assistant', content: res.answer }]);
    } catch (err) {
      setMessages((prev) => [
        ...prev,
        { role: 'assistant', content: `답변 생성 중 오류: ${err instanceof Error ? err.message : String(err)}` },
      ]);
    } finally {
      setLoading(false);
    }
  };

  return (
    <>
      {open && (
        <div className="fixed bottom-24 right-6 z-50 flex h-[520px] w-96 flex-col overflow-hidden rounded-2xl border border-border bg-card shadow-2xl">
          <div className="flex items-center justify-between gap-2 border-b border-sidebar-active bg-gradient-to-r from-sidebar via-ai-accent/25 to-sidebar px-4 py-3 text-white">
            <div className="flex items-center gap-2">
              <span className="flex h-7 w-7 items-center justify-center rounded-lg bg-ai-accent/20 text-ai-accent ring-1 ring-ai-accent/30">
                <Sparkles className="h-4 w-4" />
              </span>
              <div>
                <p className="text-sm font-bold leading-tight">PlayOps AI 도우미</p>
                <p className="text-[11px] leading-tight text-sidebar-foreground">Playwright/E2E 관련 질문에 답해드려요</p>
              </div>
            </div>
            <button
              type="button"
              onClick={() => setOpen(false)}
              className="rounded-md p-1 text-sidebar-foreground hover:bg-white/10 hover:text-white"
              aria-label="닫기"
            >
              <X className="h-4 w-4" />
            </button>
          </div>

          <div className="flex-1 space-y-2.5 overflow-y-auto bg-background p-3">
            {messages.length === 0 ? (
              <div className="flex h-full flex-col items-center justify-center gap-2 text-center text-muted-foreground">
                <Bot className="h-8 w-8" />
                <p className="text-xs">
                  Playwright 문법, 셀렉터, 에러 원인 등 무엇이든 물어보세요.
                  <br />
                  특정 프로젝트/실행에 대한 질문은 해당 프로젝트의 "AI 분석" 탭을 이용하면 더 정확합니다.
                </p>
              </div>
            ) : (
              messages.map((msg, idx) => (
                <div key={idx} className={cn('flex gap-2 text-xs', msg.role === 'user' ? 'justify-end' : 'justify-start')}>
                  <div
                    className={cn(
                      'max-w-[85%] rounded-xl px-3 py-2 whitespace-pre-wrap leading-relaxed',
                      msg.role === 'user'
                        ? 'rounded-br-none bg-ai-accent font-medium text-ai-accent-foreground'
                        : 'rounded-bl-none border border-border bg-card text-foreground'
                    )}
                  >
                    {msg.content}
                  </div>
                </div>
              ))
            )}
            {loading && (
              <div className="flex items-center gap-2 text-[11px] text-muted-foreground">
                <Loader2 className="h-3 w-3 animate-spin text-ai-accent" />
                AI 답변 작성 중...
              </div>
            )}
          </div>

          <form onSubmit={handleSend} className="flex gap-2 border-t border-border bg-card p-2.5">
            <input
              type="text"
              value={question}
              onChange={(e) => setQuestion(e.target.value)}
              placeholder="무엇이든 물어보세요"
              className="flex-1 rounded-lg border border-border bg-background px-3 py-2 text-xs text-foreground placeholder:text-muted-foreground focus:border-ai-accent focus:outline-none"
            />
            <button
              type="submit"
              disabled={loading || !question.trim()}
              className="inline-flex h-8 w-8 shrink-0 items-center justify-center rounded-lg bg-ai-accent text-ai-accent-foreground hover:opacity-90 disabled:opacity-50"
            >
              {loading ? <Loader2 className="h-3.5 w-3.5 animate-spin" /> : <Send className="h-3.5 w-3.5" />}
            </button>
          </form>
        </div>
      )}

      <button
        type="button"
        onClick={() => setOpen((prev) => !prev)}
        className="fixed bottom-6 right-6 z-50 flex h-14 w-14 items-center justify-center rounded-full bg-ai-accent text-ai-accent-foreground shadow-lg shadow-ai-accent/30 transition-transform hover:scale-105 hover:opacity-90"
        title="PlayOps AI 도우미"
        aria-label="PlayOps AI 도우미 열기"
      >
        {open ? <X className="h-6 w-6" /> : <MessageCircle className="h-6 w-6" />}
      </button>
    </>
  );
}
