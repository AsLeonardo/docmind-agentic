import { useCallback, useEffect, useState } from 'react'
import { api, session, UnauthorizedError } from './api'
import { AuthScreen } from './components/AuthScreen'
import { Chat } from './components/Chat'
import { DocumentsPanel } from './components/DocumentsPanel'
import type { DocumentInfo } from './types'

export default function App() {
  const [token, setToken] = useState(session.get())
  const [documents, setDocuments] = useState<DocumentInfo[]>([])
  const [error, setError] = useState<string | null>(null)

  const logout = useCallback(() => {
    session.set(null)
    setToken(null)
    setDocuments([])
  }, [])

  const handleError = useCallback(
    (err: unknown) => {
      if (err instanceof UnauthorizedError) logout()
      else setError(err instanceof Error ? err.message : 'Algo deu errado')
    },
    [logout],
  )

  const loadDocuments = useCallback(() => {
    api.documents().then(setDocuments).catch(handleError)
  }, [handleError])

  useEffect(() => {
    if (token) loadDocuments()
  }, [token, loadDocuments])

  if (!token) {
    return (
      <AuthScreen
        onToken={(t) => {
          session.set(t)
          setToken(t)
        }}
      />
    )
  }

  return (
    <main className="mx-auto max-w-6xl space-y-4 px-4 py-6">
      <header className="flex items-center justify-between">
        <div>
          <h1 className="text-xl font-bold text-slate-900">DocMind</h1>
          <p className="text-sm text-slate-600">Respostas com base nos seus documentos, sempre com a fonte.</p>
        </div>
        <button onClick={logout} className="text-sm text-slate-600 hover:text-slate-900">
          Sair
        </button>
      </header>
      {error && (
        <p className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-800" onClick={() => setError(null)}>
          {error}
        </p>
      )}
      <div className="grid gap-4 md:grid-cols-[260px_1fr]">
        <DocumentsPanel documents={documents} onChange={loadDocuments} onError={handleError} />
        <Chat onUnauthorized={handleError} />
      </div>
    </main>
  )
}
