# codebase_whisperer/reasoning/iterative.py
from __future__ import annotations
from typing import List, Dict, Optional, Callable
from codebase_whisperer.llm.ollama import OllamaClient

DEFAULT_CHECKLIST = [
  "Answer directly addresses the user’s question.",
  "Uses only provided context; flags missing info instead of inventing.",
  "Includes concrete steps, examples, or code when relevant.",
  "No contradictions; terminology is consistent.",
]

def _chat(client: OllamaClient, model: str, messages: List[Dict], stream: bool=False,
          on_chunk: Optional[Callable[[str], None]] = None, **opts) -> str:
    return client.chat(model, messages, stream=stream, on_chunk=on_chunk, options=opts or None)

def iterative_answer(
    *,
    client: OllamaClient,
    model: str,
    user_question: str,
    context_blocks: List[str],
    max_tokens: int = 600,
    temperature_draft: float = 0.7,
    temperature_refine: float = 0.3,
    beams: int = 1,                # >1 enables self-consistency
    checklist: List[str] = DEFAULT_CHECKLIST,
    stream_final: bool = True,
    on_final_chunk: Optional[Callable[[str], None]] = None,
    system_preamble: str = "You are a careful, rigorous coding assistant.",
) -> str:
    """
    Returns final answer (also streams it if stream_final=True).
    """
    ctx = "\n\n".join(context_blocks) if context_blocks else "(no extra context)"
    # 1) PLAN
    plan_msgs = [
      {"role": "system", "content": system_preamble},
      {"role": "user", "content":
        f"""You will plan a short solution before answering.
Question:
{user_question}

Available context (use cautiously, do not invent beyond it):
{ctx}

Produce a short plan with numbered steps. Keep it under 6 bullet points.
Do NOT answer the question yet."""
      }
    ]
    plan = _chat(client, model, plan_msgs, stream=False, num_predict=256, temperature=0.2)

    # 2) DRAFT (optionally multiple beams)
    drafts: List[str] = []
    for i in range(beams):
        draft_msgs = [
          {"role": "system", "content": system_preamble},
          {"role": "user", "content":
            f"""Follow this plan, then produce a concise draft answer.
Plan:
{plan}

Question:
{user_question}

Context:
{ctx}

Constraints:
- If info is missing from context, say so explicitly.
- Prefer concrete steps/snippets.
- Max {max_tokens} tokens in final answer.
Now produce the DRAFT ANSWER."""
          }
        ]
        d = _chat(client, model, draft_msgs, stream=False, num_predict=max_tokens, temperature=temperature_draft)
        drafts.append(d.strip())

    chosen = drafts[0]
    if beams > 1:
        # 3) SELF-CONSISTENCY “VOTE” (pick best draft via short rubric)
        vote_msgs = [
          {"role": "system", "content": system_preamble},
          {"role": "user", "content":
            f"""We have {len(drafts)} candidate drafts for the same question.

Question:
{user_question}

Context:
{ctx}

Scoring rubric (0-10):
- Directness & relevance
- Faithfulness to context (no speculation)
- Clarity & actionable steps
- Technical correctness

Return exactly one line: BEST_INDEX starting from 1."""
          },
          {"role": "assistant", "content": "\n\n".join([f"Draft {i+1}:\n{d}" for i, d in enumerate(drafts)])}
        ]
        pick = _chat(client, model, vote_msgs, stream=False, num_predict=16, temperature=0.0)
        try:
            idx = int([c for c in pick if c.isdigit()][0]) - 1
            if 0 <= idx < len(drafts):
                chosen = drafts[idx]
        except Exception:
            pass

    # 4) CRITIQUE
    checklist_text = "\n".join(f"- {item}" for item in checklist)
    critique_msgs = [
      {"role": "system", "content": system_preamble},
      {"role": "user", "content":
        f"""Critique the DRAFT against this checklist.
Checklist:
{checklist_text}

Question:
{user_question}

Context:
{ctx}

Draft:
{chosen}

Return a bullet list of concrete fixes. If none, say 'Looks good.'"""
      }
    ]
    critique = _chat(client, model, critique_msgs, stream=False, num_predict=256, temperature=0.2)

    # 5) REVISE (stream to user)
    revise_msgs = [
      {"role": "system", "content": system_preamble},
      {"role": "user", "content":
        f"""Revise the DRAFT into the FINAL ANSWER, applying the critique.
Only output the final answer (no notes).

Question:
{user_question}

Context:
{ctx}

Draft:
{chosen}

Critique:
{critique}"""
      }
    ]
    final = _chat(
        client, model, revise_msgs,
        stream=stream_final, on_chunk=on_final_chunk,
        num_predict=max_tokens, temperature=temperature_refine
    )
    return final.strip()