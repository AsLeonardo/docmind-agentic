import type { Source } from '../types'

/** Troca marcações [n] por botões que destacam a fonte correspondente. */
export function AnswerText({ text, sources, onCite }: { text: string; sources?: Source[]; onCite: (n: number) => void }) {
  const parts = text.split(/(\[\d+\])/g)
  return (
    <p className="whitespace-pre-wrap leading-relaxed">
      {parts.map((part, i) => {
        const match = /^\[(\d+)\]$/.exec(part)
        const n = match ? Number(match[1]) : null
        if (n !== null && sources?.some((s) => s.n === n)) {
          return (
            <button
              key={i}
              onClick={() => onCite(n)}
              className="mx-0.5 rounded bg-indigo-100 px-1 text-xs font-semibold text-indigo-800 hover:bg-indigo-200"
            >
              {n}
            </button>
          )
        }
        return <span key={i}>{part}</span>
      })}
    </p>
  )
}
