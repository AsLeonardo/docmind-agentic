import type { Source } from '../types'

export function SourceList({ sources, highlighted }: { sources: Source[]; highlighted: number | null }) {
  if (sources.length === 0) return null
  return (
    <div className="mt-3 space-y-1.5">
      <p className="text-xs font-semibold uppercase tracking-wide text-slate-500">Fontes</p>
      {sources.map((s) => (
        <details
          key={s.n}
          open={highlighted === s.n}
          className={`rounded-lg border bg-white px-3 py-2 text-sm ${highlighted === s.n ? 'border-indigo-400 ring-2 ring-indigo-100' : 'border-slate-200'}`}
        >
          <summary className="cursor-pointer text-slate-700">
            <span className="mr-2 font-semibold text-indigo-700">[{s.n}]</span>
            {s.filename}
            {s.page != null && <span className="text-slate-500">, p. {s.page}</span>}
            <span className="ml-2 text-xs text-slate-400">similaridade {s.score.toFixed(2)}</span>
          </summary>
          <p className="mt-2 whitespace-pre-wrap text-xs leading-relaxed text-slate-600">{s.text}</p>
        </details>
      ))}
    </div>
  )
}
