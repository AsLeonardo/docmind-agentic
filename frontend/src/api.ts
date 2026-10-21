import type { DocumentInfo, Source } from './types'

const TOKEN_KEY = 'docmind.token'

export const session = {
  get(): string | null {
    try {
      return localStorage.getItem(TOKEN_KEY)
    } catch {
      return null
    }
  },
  set(token: string | null) {
    try {
      if (token) localStorage.setItem(TOKEN_KEY, token)
      else localStorage.removeItem(TOKEN_KEY)
    } catch {
      /* armazenamento indisponível: a sessão dura até recarregar a página */
    }
  },
}

export class UnauthorizedError extends Error {}

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const token = session.get()
  const headers = new Headers(init.headers)
  if (token) headers.set('Authorization', `Bearer ${token}`)
  if (init.body && !(init.body instanceof FormData)) headers.set('Content-Type', 'application/json')

  const res = await fetch(path, { ...init, headers })
  if (res.status === 401 && token) throw new UnauthorizedError('Sessão expirada')
  if (!res.ok) {
    const problem = await res.json().catch(() => null)
    throw new Error(problem?.detail ?? `Erro ${res.status}`)
  }
  return (res.status === 204 ? undefined : await res.json()) as T
}

export const api = {
  register: (email: string, password: string) =>
    request<{ token: string }>('/api/auth/register', { method: 'POST', body: JSON.stringify({ email, password }) }),
  login: (email: string, password: string) =>
    request<{ token: string }>('/api/auth/login', { method: 'POST', body: JSON.stringify({ email, password }) }),
  documents: () => request<DocumentInfo[]>('/api/documents'),
  upload: (file: File) => {
    const form = new FormData()
    form.append('file', file)
    return request<DocumentInfo>('/api/documents', { method: 'POST', body: form })
  },
  remove: (id: number) => request<void>(`/api/documents/${id}`, { method: 'DELETE' }),
}

export interface StreamHandlers {
  onSources: (sources: Source[]) => void
  onToken: (text: string) => void
  onError: (message: string) => void
}

/**
 * POST /api/chat devolve Server-Sent Events. EventSource só faz GET, então lemos o corpo
 * com fetch + ReadableStream e separamos os eventos manualmente.
 */
export async function streamChat(
  question: string,
  history: { role: string; content: string }[],
  handlers: StreamHandlers,
  signal?: AbortSignal,
) {
  const res = await fetch('/api/chat', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Accept: 'text/event-stream',
      Authorization: `Bearer ${session.get()}`,
    },
    body: JSON.stringify({ question, history }),
    signal,
  })
  if (res.status === 401) throw new UnauthorizedError('Sessão expirada')
  if (!res.ok || !res.body) throw new Error(`Erro ${res.status}`)

  const reader = res.body.pipeThrough(new TextDecoderStream()).getReader()
  let buffer = ''
  for (;;) {
    const { value, done } = await reader.read()
    if (done) break
    buffer += value
    let boundary
    while ((boundary = buffer.indexOf('\n\n')) >= 0) {
      dispatch(buffer.slice(0, boundary), handlers)
      buffer = buffer.slice(boundary + 2)
    }
  }
}

function dispatch(raw: string, handlers: StreamHandlers) {
  let event = 'message'
  const data: string[] = []
  for (const line of raw.split('\n')) {
    if (line.startsWith('event:')) event = line.slice(6).trim()
    else if (line.startsWith('data:')) data.push(line.slice(5))
  }
  if (data.length === 0) return
  const payload = JSON.parse(data.join('\n'))
  if (event === 'sources') handlers.onSources(payload)
  else if (event === 'token') handlers.onToken(payload.text)
  else if (event === 'error') handlers.onError(payload.message)
}
