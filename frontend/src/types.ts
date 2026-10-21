export interface DocumentInfo {
  id: number
  filename: string
  contentType: string
  sizeBytes: number
  chunkCount: number
  createdAt: string
}

export interface Source {
  n: number
  documentId: number
  filename: string
  page: number | null
  chunkIndex: number
  score: number
  text: string
}

export interface ChatMessage {
  role: 'user' | 'assistant'
  content: string
  sources?: Source[]
  error?: string
  streaming?: boolean
}
