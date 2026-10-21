import { useState, type FormEvent } from 'react'
import { api } from '../api'

export function AuthScreen({ onToken }: { onToken: (token: string) => void }) {
  const [mode, setMode] = useState<'login' | 'register'>('login')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  async function submit(e: FormEvent) {
    e.preventDefault()
    setLoading(true)
    setError(null)
    try {
      const { token } = mode === 'login' ? await api.login(email, password) : await api.register(email, password)
      onToken(token)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Falha ao entrar')
    } finally {
      setLoading(false)
    }
  }

  const input = 'w-full rounded-lg border border-slate-300 px-3 py-2 text-sm focus:border-indigo-500 focus:outline-none'

  return (
    <main className="flex min-h-screen items-center justify-center px-4">
      <form onSubmit={submit} className="w-full max-w-sm space-y-4 rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
        <div>
          <h1 className="text-2xl font-bold text-slate-900">DocMind</h1>
          <p className="text-sm text-slate-600">Pergunte aos seus documentos. Cada resposta cita de onde veio.</p>
        </div>
        <input className={input} type="email" placeholder="E-mail" value={email} onChange={(e) => setEmail(e.target.value)} required />
        <input
          className={input}
          type="password"
          placeholder="Senha (mín. 8 caracteres)"
          minLength={8}
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          required
        />
        {error && <p className="text-sm text-red-700">{error}</p>}
        <button
          disabled={loading}
          className="w-full rounded-lg bg-indigo-600 py-2 text-sm font-semibold text-white hover:bg-indigo-700 disabled:opacity-50"
        >
          {mode === 'login' ? 'Entrar' : 'Criar conta'}
        </button>
        <button
          type="button"
          className="w-full text-sm text-indigo-700 hover:underline"
          onClick={() => setMode(mode === 'login' ? 'register' : 'login')}
        >
          {mode === 'login' ? 'Não tem conta? Criar uma' : 'Já tenho conta'}
        </button>
      </form>
    </main>
  )
}
