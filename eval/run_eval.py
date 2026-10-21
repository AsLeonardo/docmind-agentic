"""Avalia a recuperação do DocMind (sem LLM) com perguntas rotuladas.

Para cada pergunta, verifica em que posição o documento esperado aparece no /api/search.
Métricas: hit@1, hit@3, hit@5 e MRR (média do inverso da posição do primeiro acerto).

Uso:
    python eval/run_eval.py --url http://localhost:8080 --upload docs/exemplos
O script cria um usuário de avaliação, envia os documentos e roda as perguntas. Sem dependências externas.
"""
import argparse
import json
import mimetypes
import urllib.parse
import urllib.request
import uuid
from pathlib import Path

HERE = Path(__file__).parent


def call(url, method="GET", body=None, token=None, content_type="application/json"):
    headers = {"Content-Type": content_type} if body is not None else {}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    req = urllib.request.Request(url, data=body, headers=headers, method=method)
    with urllib.request.urlopen(req, timeout=120) as res:
        return json.load(res)


def upload(api, token, path):
    boundary = uuid.uuid4().hex
    mime = mimetypes.guess_type(path.name)[0] or "text/plain"
    body = (
        f"--{boundary}\r\nContent-Disposition: form-data; name=\"file\"; filename=\"{path.name}\"\r\n"
        f"Content-Type: {mime}\r\n\r\n"
    ).encode() + path.read_bytes() + f"\r\n--{boundary}--\r\n".encode()
    return call(f"{api}/api/documents", "POST", body, token, f"multipart/form-data; boundary={boundary}")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--url", default="http://localhost:8080")
    parser.add_argument("--upload", type=Path, help="pasta com documentos para enviar antes de avaliar")
    parser.add_argument("--k", type=int, default=5)
    args = parser.parse_args()
    api = args.url.rstrip("/")

    creds = json.dumps({"email": f"eval-{uuid.uuid4().hex[:8]}@docmind.dev", "password": "avaliacao-123"}).encode()
    token = call(f"{api}/api/auth/register", "POST", creds)["token"]

    if args.upload:
        for path in sorted(args.upload.glob("*")):
            doc = upload(api, token, path)
            print(f"enviado {doc['filename']}: {doc['chunkCount']} trechos")

    questions = [json.loads(l) for l in (HERE / "questions.jsonl").read_text(encoding="utf-8").splitlines() if l.strip()]
    hits = {1: 0, 3: 0, 5: 0}
    reciprocal_ranks = []
    misses = []
    for q in questions:
        query = urllib.parse.urlencode({"q": q["question"], "k": args.k})
        results = call(f"{api}/api/search?{query}", token=token)
        files = [r["filename"] for r in results]
        rank = files.index(q["expected"]) + 1 if q["expected"] in files else None
        for k in hits:
            hits[k] += rank is not None and rank <= k
        reciprocal_ranks.append(1 / rank if rank else 0)
        if rank != 1:
            misses.append(f"  posição {rank or '-'}: {q['question']!r} (veio {files[0] if files else 'nada'})")

    n = len(questions)
    print(f"\nperguntas: {n}")
    for k, h in hits.items():
        print(f"hit@{k}: {h / n:.0%}")
    print(f"MRR:   {sum(reciprocal_ranks) / n:.2f}")
    if misses:
        print("Fora do 1º lugar:")
        print("\n".join(misses))


if __name__ == "__main__":
    main()
