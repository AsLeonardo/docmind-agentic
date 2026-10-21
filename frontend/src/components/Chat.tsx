import { useEffect, useRef, useState, type FormEvent } from 'react'
import { streamChat } from '../api'
import type { ChatMessage } from '../types'
import { AnswerText } from './AnswerText'
import { SourceList } from './SourceList'

const SUGGESTIONS = [
  'O que fazer quando a VPN dá erro de autenticação?',
  'Quantos dias tenho para pedir reembolso de despesas?',
  'Qual é o prazo para responder um chamado P1?',
]

export function Chat({ onUnauthorized }: { onUnauthorized: (err: unknown) => void }) {
  const [messages, setMessages] = useState<ChatMessage[]>([])
  const [input, setInput] = useState('')
  const [highlight, setHighlight] = useState<{ msg: number; n: number } | null>(null)
  const abort = useRef<AbortController | null>(null)
  const bottom = useRef<HTMLDivElement>(null)
  const busy = messages[messages.length - 1]?.streaming ?? false

  useEffect(() => {
    bottom.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages])

  function patchLast(patch: (m: ChatMessage) => ChatMessage) {
    setMessages((prev) => [...prev.slice(0, -1), patch(prev[prev.length - 1])])
  }

  async function ask(question: string) {
    if (!question.trim() || busy) return
    const history = messages.filter((m) => !m.error).map(({ role, content }) => ({ role, content }))
    setMessages((prev) => [
      ...prev,
      { role: 'user', content: question },
      { role: 'assistant', content: '', streaming: true },
    ])
    setInput('')
    abort.current = new AbortController()
    try {
      await streamChat(
        question,
        history,
        {
          onSources: (sources) => patchLast((m) => ({ ...m, sources })),
          onToken: (text) => patchLast((m) => ({ ...m, content: m.content + text })),
          onError: (error) => patchLast((m) => ({ ...m, error })),
        },
        abort.current.signal,
      )
    } catch (err) {
      if (err instanceof DOMException && err.name === 'AbortError') return
      onUnauthorized(err)
      patchLast((m) => ({ ...m, error: err instanceof Error ? err.message : 'Falha na conexão' }))
    } finally {
      patchLast((m) => ({ ...m, streaming: false }))
    }
  }

  function submit(e: FormEvent) {
    e.preventDefault()
    ask(input)
  }

  return (
    <section className="flex min-h-[70vh] flex-col rounded-2xl border border-slate-200 bg-white shadow-sm">
      <div className="flex-1 space-y-5 overflow-y-auto p-5">
        {messages.length === 0 && (
          <div className="space-y-3 py-10 text-center">
            <p className="text-slate-600">Faça uma pergunta sobre os documentos enviados.</p>
            <div className="flex flex-wrap justify-center gap-2">
              {SUGGESTIONS.map((s) => (
                <button
                  key={s}
                  onClick={() => ask(s)}
                  className="rounded-full border border-slate-300 px-3 py-1 text-xs text-slate-700 hover:bg-slate-100"
                >
                  {s}
                </button>
              ))}
            </div>
          </div>
        )}
        {messages.map((m, i) =>
          m.role === 'user' ? (
            <div key={i} className="flex justify-end">
              <p className="max-w-[80%] rounded-2xl rounded-br-sm bg-indigo-600 px-4 py-2 text-sm text-white">{m.content}</p>
            </div>
          ) : (
            <div key={i} className="max-w-[90%] text-sm text-slate-800">
              {m.content ? (
                <AnswerText text={m.content} sources={m.sources} onCite={(n) => setHighlight({ msg: i, n })} />
              ) : (
                m.streaming && <p className="animate-pulse text-slate-400">Buscando nos documentos…</p>
              )}
              {m.error && <p className="mt-1 text-red-700">{m.error}</p>}
              {!m.streaming && m.sources && (
                <SourceList sources={m.sources} highlighted={highlight?.msg === i ? highlight.n : null} />
              )}
            </div>
          ),
        )}
        <div ref={bottom} />
      </div>
      <form onSubmit={submit} className="flex gap-2 border-t border-slate-200 p-3">
        <input
          className="flex-1 rounded-lg border border-slate-300 px-3 py-2 text-sm focus:border-indigo-500 focus:outline-none"
          placeholder="Pergunte algo…"
          value={input}
          maxLength={2000}
          onChange={(e) => setInput(e.target.value)}
        />
        {busy ? (
          <button type="button" onClick={() => abort.current?.abort()} className="rounded-lg border border-slate-300 px-4 text-sm">
            Parar
          </button>
        ) : (
          <button className="rounded-lg bg-indigo-600 px-4 text-sm font-semibold text-white hover:bg-indigo-700">Enviar</button>
        )}
      </form>
    </section>
  )
}
