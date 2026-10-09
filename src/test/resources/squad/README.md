# SQuAD v1.1 evaluation snapshot

A vendored, deterministic slice of the **Stanford Question Answering Dataset
(SQuAD) v1.1** used by DocIntel's real-inputs benchmark
(`com.angelo.docintel.eval.SquadBenchmarkTest`). It turns a public
question-answering-over-documents gold set into a retrieval + grounded-answer
evaluation for DocIntel's own TF-IDF pipeline.

## Source

| | |
|---|---|
| Dataset | SQuAD v1.1 (`rajpurkar/squad` on the Hugging Face Hub) |
| Config / split | `plain_text` / `validation` (10 570 QA pairs over ~2 000 paragraphs) |
| Slice vendored here | the **first 1 200 rows** of the validation split |
| License | CC BY-SA 4.0 (SQuAD is released under Creative Commons Attribution-ShareAlike 4.0) |
| Upstream paper | Rajpurkar et al., *SQuAD: 100,000+ Questions for Machine Comprehension of Text*, EMNLP 2016 |

SQuAD rows carry `id`, `title`, `context` (a Wikipedia paragraph), `question`
and `answers.text` (one or more gold answer spans copied verbatim from the
context). The first 1 200 validation rows span four articles — `Super_Bowl_50`,
`Warsaw`, `Normans` and `Nikola_Tesla` — which gives both cross-topic retrieval
(telling a football paragraph from a Tesla paragraph) and the harder
within-topic retrieval (telling two Super-Bowl-50 paragraphs apart). That mix is
what makes the benchmark a real test rather than a topic classifier.

## Files and schema

Both files are UTF-8 JSON Lines (one JSON object per line).

`corpus.jsonl` — the document corpus (each distinct SQuAD paragraph is one
DocIntel document):

```json
{"id": "d000", "title": "Super_Bowl_50", "text": "Super Bowl 50 was an American football game ..."}
```

`questions.jsonl` — the gold question set:

```json
{"id": "56be4db0acb8001400a502ec", "question": "Which NFL team represented the AFC at Super Bowl 50?", "docId": "d000", "answers": ["Denver Broncos"]}
```

`docId` is the id of the corpus document the question's answer was written
against — SQuAD's ground truth for which paragraph should be retrieved.
`answers` is the de-duplicated list of gold answer strings for that question.

## Shape

| Metric | Value |
|---|---|
| Documents (distinct paragraphs) | 152 |
| Questions | 1 200 |
| Articles | 4 (`Super_Bowl_50`, `Warsaw`, `Normans`, `Nikola_Tesla`) |
| Mean questions per document | 7.9 |
| Random recall@1 (chance, 1 gold doc of 152) | ≈ 0.7 % |

SHA-256 (as vendored):

```
corpus.jsonl     ed010987a89bace148c63c98db7e6a2cb3fe0227a2fec705626eeff4021516ae
questions.jsonl  68e9c53f03b9cafa9ee05567ca85afaa442af794748603f8cbadded5e38dffde
```

## Regeneration

The slice is reproducible from the Hugging Face datasets-server rows API. Each
page returns at most 100 rows, so twelve pages cover the first 1 200 rows:

```bash
mkdir -p sq_pages
for off in $(seq 0 100 1100); do
  curl -sS "https://datasets-server.huggingface.co/rows?dataset=rajpurkar%2Fsquad&config=plain_text&split=validation&offset=${off}&length=100" \
    > sq_pages/p_${off}.json
done
```

```python
# python3 -I regenerate.py   (reads sq_pages/, writes corpus.jsonl + questions.jsonl)
import json, os
rows = []
for off in range(0, 1200, 100):
    rows += [r["row"] for r in json.load(open(f"sq_pages/p_{off}.json"))["rows"]]
docid, docs, questions = {}, [], []
for r in rows:
    ctx = r["context"]
    if ctx not in docid:
        did = "d%03d" % len(docs)
        docid[ctx] = did
        docs.append({"id": did, "title": r["title"], "text": ctx})
    seen, ans = set(), []
    for a in r["answers"]["text"]:
        if a not in seen:
            seen.add(a); ans.append(a)
    questions.append({"id": r["id"], "question": r["question"],
                      "docId": docid[ctx], "answers": ans})
with open("corpus.jsonl", "w") as f:
    for d in docs: f.write(json.dumps(d, ensure_ascii=False) + "\n")
with open("questions.jsonl", "w") as f:
    for q in questions: f.write(json.dumps(q, ensure_ascii=False) + "\n")
```

Documents are numbered `d000, d001, ...` in first-appearance order within the
slice, so regeneration is deterministic and byte-stable.
