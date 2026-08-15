# harmoni-launcher

## MVP mode — ACTIVE until I say "MVP shipped"
Pre-MVP, shipping speed beats everything. Where this section disagrees with any rule below it, **this section wins**. Do not ask me to confirm any of it.

Suspended until MVP ships:
- The whole **Code quality bar** section. No SOLID/DRY/KISS/YAGNI passes, no splitting a file at 500 lines, no deduplication, no dead-code sweeps, no magic-number extraction, no nesting refactors. Ugly and shipped beats clean and pending.
- Tests, typecheck, and lint. Don't write them, don't run them, don't fix a red one, don't report on them. A failing gate is not a blocker.
- `/review`. Never run it, never offer it, never mention it.
- Merge authorization. `-m` is not required and not wanted. Finish a slice, land it, keep going. No pre-merge checklist, no waiting, no asking. **The branch and PR themselves are not suspended** (see below); only the gate in front of the merge is.
- Requirements rounds. Don't open with MCQs. Take the sensible reading, build it, then state the assumption in one line so I can veto it.
- Commit hygiene. Batch freely. Atomicity, "commit as you build", and amend-don't-stack are all off.
- Doc currency. README and SPEC drift is fine.
- The lettered handoff menu. One or two lines on what changed and what I can try instead.

Still on, because none of it is code quality:
- Conventional Commit subjects. The husky `commit-msg` hook runs commitlint and will reject anything else, and `--no-verify` stays out of bounds.
- Confirm-first on anything that destroys work: force-push, history rewrite, deleting unmerged work.
- No secrets committed.
- Everything in **Working with me** about how you talk to me: no em-dashes, no unmeasured claims, no "did you refresh", report what you observed rather than what you intended.

**Branching and merging are NOT suspended.** Every change still goes on a `<type>/<kebab-summary>` branch, still gets a PR, and still lands by squash-merge with the branch deleted. Never commit straight to `main`, even though `main` has no enforced protection (a private repo on this plan cannot enable it). MVP mode removes the review and the wait, not the workflow: I still want every slice as a revertable unit with a PR behind it.

**Exit:** when I say MVP has shipped, delete this section. Everything below applies again in full, and the first task after that is paying down whatever this let through.



## Environment
- GitHub user: `wpinrui`.
- Use `python`, never `python3`.
- Scratch files go in `temp/`, which is gitignored. Not the session scratchpad, which I cannot navigate, and nowhere else in the repo.
- Never use the memory tool. I work across machines and memory does not travel. Anything worth keeping goes in this file, which is in git.

## Branching & commits
- `main` is protected. Branch before any change.
- Branch names: `<type>/<kebab-summary>`, `<type>` ∈ {feat, fix, refactor, perf, docs, test, build, ci, chore}.
- One feature branch, one PR at a time. Do not open a new branch, even on a different working tree.
- Conventional Commits, single-line subject, no body. Small, atomic, one logical change.
- Fix an immediate mistake by amending, not by stacking an "oops" commit.
- Commit as you build: finish a logical slice, commit it, then start the next.
- `--force-with-lease` only, never `--force`. Never force-push `main`.
- PR body: `Closes #N` on its own line per issue. A comma list closes only the first.

## Merge & PR
- Open a PR as soon as the first commit is pushed. Never ask. Opening one says nothing about whether the work is ready to review or merge.
- Squash-merge only, deleting the merged branch: `gh pr merge --squash --delete-branch`.
- Run `/review` before every merge, unprompted. It is mandatory, not a per-PR decision.
- Pre-merge checks, all of them: review approved, tests green, typecheck clean, working tree clean, and no findings left open.
- Triggers are explicit flags, never the word "go". `-r` review now. `-m` merge, and this IS the merge authorisation. `-rm` review then merge in the same turn if it passes, no pause between.
- `-m`/`-rm` are still gated on the pre-merge checks. If a check fails, fix it and try again.
- A failing review under `-rm` is not a stop. Fix and re-review until it passes.
- After merge: confirm the branch is gone and pull `main`.

## Tasks & questions
- "Do X" is a task. A question is a question, "can we do X" included: that one asks whether X is feasible. Put `Task:` in front to make a question a task.
- A question asked once gets an immediate, minimal answer. Look only at what you can check in seconds; if the answer needs more than that, say what it would take and stop.
- Asked again, something is wrong: I am pushing back, something was done incorrectly, or we have miscommunicated. Spend the effort then.
- A one-line question gets a one-line answer: no preamble, no "you're right to push back".

## Working modes
- Before building a feature, gather requirements first: multiple-choice questions, lettered options, covering scope and behaviour. Never invent a spec. Bugfixes are exempt when the bug is clearly defined.
- Every option needs a real reason to pick it. Do not provide trap options.
- A trap I have not thought of is its own line, after the options, not a caveat hung on each one.
- Use plan mode when a change spans several files or the approach is uncertain, and show me the plan before you build. Skip it when the diff fits in one sentence.
- `-afk` I am not there and cannot answer. Make every call yourself and do whatever it takes to keep going.
- `-spec` requirements first, as many rounds as it takes. Recommend an option each time. Get my sign-off, then honour the spec.
- `-iter` thinnest slice I can try, then hand it over and loop on my feedback. Zero housekeeping mid-loop: no tests, typecheck or review. Commit only when I ask. Subagents inherit that. Batch it all when I explicitly end the loop.

## Working with me
- Restate non-trivial tasks in your own words before starting.
- Do not silently drop a requirement. Surface it and ask.
- Remind me of something I may have missed at most twice. If I do not respond, I have decided it is not relevant.
- Run the tests and typecheck that cover the diff, not every suite.
- A gate failure in a file the diff never touched is not yours. Name it with `file:line`, say it is pre-existing, carry on.
- Report where we are now, not how we got here. Skip what was broken and what you tried. If it can be said shorter, say it shorter.
- Never assert a cause or cite a number you have not measured. Mark measured versus assumed. Do not rationalise a failure with invented facts.
- If I say it is still broken after your fix, your fix was wrong. Re-read what I said. Never suggest I did not refresh, reload or pull.
- When I say something is broken, do not fix it. Tell me what you think is wrong, or say plainly that you have no idea, then stop. No "fixed" until I say so.
- Never suppress an error or skip a test. Fix root causes, not symptoms.
- Your workspace IS my working copy. Never tell me to pull, rebuild or sync to see your changes.
- Never offer to launch or run the project for me. Verify headlessly and state plainly what you could not verify that way.
- Building and installing to my device is fine. Running it is not: no `am start`, no `force-stop`, no `input tap`/`swipe`, no screenshots unless I ask. I drive the app, you don't.
- No em-dashes anywhere you write. End the sentence at the clause and cut the trailer, do not swap in a comma or hyphen.
- No middots either, anywhere: not in prose, not in UI copy, not as a separator in code or in output. Use a word, a comma or a list.
- Load-bearing info on a GitHub issue goes in the body (`gh issue edit`), never in comments. Fold corrections and dependency notes into the body.
- Keep `README.md` current in the same PR when a change is reader-facing. Not for internal refactors or test tweaks.
- List options with letters, not numbers.
- Confirm risky actions (force-push, history rewrite, deleting an unmerged or shared branch, data loss) before executing.
- End a completed unit of work with a short handoff and a lettered menu of only the currently actionable options, spelled out each time. Never offer a "done" opt-out.
