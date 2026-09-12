# Prompt: Kickoff (first Claude Code session)

Paste this as the very first message in Claude Code, from the repo root, after creating the empty
git repo and copying `CLAUDE.md`, `PROJECT_MANIFEST.md` and `.claude/` into it.

---

You are starting the EasyTrain project from scratch. Read, in this order: `CLAUDE.md`,
`.claude/context.md`, `.claude/standards.md`, `.claude/roadmap.md`, `.claude/SKILLS.md`.

Then:

1. Summarize back to me in ≤ 15 lines: the two roles, the single-writer rule, the sync contract's
   write path, and the Phase 0 deliverables — so I can confirm you've understood the spec.
2. List every question you have about `context.md` that would change how you build Phase 0 or 1.
   Do not guess on: module names, the Supabase key handling, or anything on the destructive list.
3. Wait for my answers.
4. Then implement Phase 0 (ET-001) exactly as its Prompt in `roadmap.md` says, on branch
   `feature/ET-001-bootstrap`, starting with `/resolve-versions` for the full dependency list.

Rules that apply from the first line of code: no new technologies without asking, no versions
from memory, no unsolicited docs, keep `PROJECT_MANIFEST.md` current, never run anything on the
destructive list.
