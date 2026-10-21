// Servidor que imita a API da OpenAI (embeddings + chat em streaming) para os testes E2E.
// Determinístico e sem custo: o E2E testa a integração do sistema, não a qualidade do modelo.
import { createServer } from 'node:http'

const DIMS = Number(process.env.EMBEDDING_DIMENSIONS ?? 768)
export const ANSWER = 'Troque a senha pelo portal e tente de novo depois de 5 minutos [1].'

function hash(word) {
  let h = 0
  for (const c of word) h = (h * 31 + c.charCodeAt(0)) | 0
  return Math.abs(h)
}

function embed(text) {
  const v = new Array(DIMS).fill(0)
  const words = text.toLowerCase().normalize('NFD').replace(/\p{M}/gu, '').split(/[^a-z0-9]+/)
  for (const w of words) if (w.length > 3) v[hash(w.slice(0, 6)) % DIMS] += 1
  const norm = Math.hypot(...v) || 1
  return v.map((x) => x / norm)
}

function readBody(req) {
  return new Promise((resolve) => {
    const chunks = []
    req.on('data', (c) => chunks.push(c))
    req.on('end', () => resolve(JSON.parse(Buffer.concat(chunks).toString() || '{}')))
  })
}

createServer(async (req, res) => {
  if (req.method === 'GET' && req.url === '/health') return res.end('ok')
  const body = await readBody(req)

  if (req.url.endsWith('/embeddings')) {
    const inputs = Array.isArray(body.input) ? body.input : [body.input]
    res.setHeader('Content-Type', 'application/json')
    return res.end(JSON.stringify({
      object: 'list',
      model: body.model,
      data: inputs.map((text, index) => ({ object: 'embedding', index, embedding: embed(text) })),
      usage: { prompt_tokens: 1, total_tokens: 1 },
    }))
  }

  res.setHeader('Content-Type', 'text/event-stream')
  const chunk = (delta, finish = null) =>
    `data: ${JSON.stringify({ id: 'e2e', object: 'chat.completion.chunk', created: 0, model: body.model, choices: [{ index: 0, delta, finish_reason: finish }] })}\n\n`
  for (const word of ANSWER.match(/\S+\s*/g)) res.write(chunk({ content: word }))
  res.end(chunk({}, 'stop') + 'data: [DONE]\n\n')
}).listen(9999, '127.0.0.1')
