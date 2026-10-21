import { expect, test, type Page } from '@playwright/test'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

// Rastreia: specs/001-respostas-com-fontes/spec.md (REQ-01, REQ-03, REQ-04, REQ-05, REQ-07)
const EXAMPLES = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../../docs/exemplos')

// Espera o documento aparecer na lista já indexado; durante o envio só aparece "Indexando <arquivo>…"
async function uploadAndWait(page: Page, filename: string) {
  await page.locator('input[type=file]').setInputFiles(path.join(EXAMPLES, filename))
  await expect(page.getByRole('listitem').filter({ hasText: filename })).toContainText('trechos')
}

async function signUp(page: Page) {
  await page.goto('/')
  await page.getByRole('button', { name: 'Não tem conta? Criar uma' }).click()
  await page.getByPlaceholder('E-mail').fill(`e2e-${Date.now()}-${Math.random().toString(36).slice(2)}@docmind.dev`)
  await page.getByPlaceholder(/Senha/).fill('senha-e2e-123')
  await page.getByRole('button', { name: 'Criar conta' }).click()
  await expect(page.getByRole('heading', { name: 'Documentos' })).toBeVisible()
}

test('responde com citação clicável que abre o trecho de origem', async ({ page }) => {
  await signUp(page)
  await uploadAndWait(page, 'manual-vpn.md')

  await page.getByPlaceholder('Pergunte algo…').fill('O que significa o erro -12 na VPN?')
  await page.getByRole('button', { name: 'Enviar' }).last().click()

  await expect(page.getByText('Troque a senha pelo portal e tente de novo')).toBeVisible()
  const citation = page.getByRole('button', { name: '1', exact: true })
  await expect(citation).toBeVisible()
  await citation.click()
  // a fonte [1] se abre e mostra o trecho original
  const source = page.locator('details[open]')
  await expect(source).toContainText('[1]')
  await expect(source).toContainText('manual-vpn.md')
})

test('sem documentos, avisa que não encontrou em vez de inventar', async ({ page }) => {
  await signUp(page)
  await page.getByPlaceholder('Pergunte algo…').fill('Qual é a política de férias?')
  await page.getByRole('button', { name: 'Enviar' }).last().click()
  await expect(page.getByText(/Não encontrei nada sobre isso/)).toBeVisible()
})

test('um usuário não vê os documentos de outro', async ({ browser }) => {
  const ana = await browser.newPage()
  await signUp(ana)
  await uploadAndWait(ana, 'politica-reembolso.md')

  const bruno = await browser.newPage()
  await signUp(bruno)
  await expect(bruno.getByText(/Envie PDFs, Markdown ou TXT/)).toBeVisible()
  await bruno.getByPlaceholder('Pergunte algo…').fill('Qual o limite de reembolso de alimentação?')
  await bruno.getByRole('button', { name: 'Enviar' }).last().click()
  await expect(bruno.getByText(/Não encontrei nada sobre isso/)).toBeVisible()
})
