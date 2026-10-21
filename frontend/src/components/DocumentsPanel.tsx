import { useRef, useState } from 'react'
import { api } from '../api'
import type { DocumentInfo } from '../types'

interface Props {
  documents: DocumentInfo[]
  onChange: () => void
  onError: (err: unknown) => void
}

export function DocumentsPanel({ documents, onChange, onError }: Props) {
  const fileInput = useRef<HTMLInputElement>(null)
  const [uploading, setUploading] = useState<string | null>(null)

  async function upload(files: FileList | null) {
    if (!files) return
    for (const file of Array.from(files)) {
      setUploading(file.name)
      try {
        await api.upload(file)
      } catch (err) {
        onError(err)
      }
    }
    setUploading(null)
    if (fileInput.current) fileInput.current.value = ''
    onChange()
  }

  async function remove(doc: DocumentInfo) {
    if (!confirm(`Remover ${doc.filename}?`)) return
    try {
      await api.remove(doc.id)
      onChange()
    } catch (err) {
      onError(err)
    }
  }

  return (
    <aside className="space-y-3">
      <div className="flex items-center justify-between">
        <h2 className="font-semibold text-slate-900">Documentos</h2>
        <button
          className="rounded-lg bg-indigo-600 px-3 py-1.5 text-xs font-semibold text-white hover:bg-indigo-700 disabled:opacity-50"
          disabled={uploading !== null}
          onClick={() => fileInput.current?.click()}
        >
          Enviar
        </button>
        <input
          ref={fileInput}
          type="file"
          accept=".pdf,.md,.txt"
          multiple
          hidden
          onChange={(e) => upload(e.target.files)}
        />
      </div>
      {uploading && <p className="text-xs text-slate-500">Indexando {uploading}…</p>}
      {documents.length === 0 && !uploading && (
        <p className="text-sm text-slate-500">Envie PDFs, Markdown ou TXT. Os exemplos em docs/exemplos ajudam a testar.</p>
      )}
      <ul className="space-y-2">
        {documents.map((d) => (
          <li key={d.id} className="group flex items-start justify-between gap-2 rounded-lg border border-slate-200 bg-white p-2.5">
            <div className="min-w-0">
              <p className="truncate text-sm font-medium text-slate-800" title={d.filename}>
                {d.filename}
              </p>
              <p className="text-xs text-slate-500">
                {d.chunkCount} trechos · {(d.sizeBytes / 1024).toFixed(0)} KB
              </p>
            </div>
            <button
              className="text-xs text-slate-400 hover:text-red-600"
              onClick={() => remove(d)}
              aria-label={`Remover ${d.filename}`}
            >
              ✕
            </button>
          </li>
        ))}
      </ul>
    </aside>
  )
}
