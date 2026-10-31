# AI-assisted development workflow

How Claude Code is used on this project, what is enforced automatically, what still needs a human check,
and an honest account of what went right and wrong in Waves 1 and 2.

## 1. Principle

**The AI executes, deterministic code checks, the developer decides.**
An assistant's output — code, a report, a "tests pass" claim — is treated as a proposal until it is verified
by something that does not depend on the assistant: the build, a test seen failing, a diff read by a human.

## 2. Project setup

| Piece | Location | What it does | Nature |
|-------|----------|--------------|--------|
| Project instructions | `CLAUDE.md` | Commands, architecture, project rules (migrations, `Clock`, destructive commands, verification) | **Guidance** — loaded in every session, usually followed, not guaranteed |
| `/new-migration` skill | `.claude/skills/new-migration/SKILL.md` | Creates the next Flyway `V<n+1>` migration; sees the existing files through dynamic context injection | **Guidance** — runs only when invoked (`disable-model-invocation: true`) |
| Migration guard hook | `.claude/hooks/protect-migrations.sh` (`PreToolUse`, `Edit\|Write`) | Blocks edits to an existing `db/migration/V*.sql`, allows new ones; fails closed if it cannot parse its input | **Enforced** — exit code 2 stops the tool call |
| Destructive-command rules | `.claude/settings.json` → `permissions.ask` | Asks for confirmation before `git restore`, `git checkout --`, `git reset --hard`, `git push --force/-f`, `git clean`, `rm -rf`, `docker compose down`, `Remove-Item` | **Enforced** — for both the `Bash` and `PowerShell` tools |
| Formatter | Spotless + Palantir Java Format in `pom.xml` | `./mvnw verify` fails on unformatted code; `./mvnw spotless:apply` fixes it | **Enforced** by the build |

Rule of thumb: a rule that must **never** be broken needs enforcement (hook, permission rule, build check),
not only a line in `CLAUDE.md`. The simplest mechanism that works wins: declarative permission rules for
command prefixes, a hook only where logic is needed ("does this migration already exist?").

## 3. Delegation workflow

**Prompt template** (used for every delegated task):

1. **Goal** — one sentence.
2. **Why** — the problem it solves, so the assistant makes sensible choices in the details.
3. **Where** — the files allowed to change.
4. **Constraints** — what must not happen (no production changes, no new plugins, no commit) and the design
   decisions already taken, so the assistant does not take them on its own.
5. **Acceptance criteria** — commands to run and expected output, a mutation check ("break it, show it fails"),
   and a final report including **what was not verified**.

**Verification checklist** (after every delegated task):

- [ ] Read the diff (`git diff`; `git diff -w` for formatting changes).
- [ ] Re-run the checks independently instead of trusting the report.
- [ ] Check the **procedure** (rules followed, right files touched) **and the result** (does it do what was asked?).
- [ ] Compare the result with the **actual request sent**, not with the request you think was sent.
- [ ] Confirm the mutation check really fails for the intended reason (an assertion, not an unrelated error).

The more obscure the domain (Maven plugin configuration, for example), the more prescriptive the prompt
should be: give the documented solution and leave the assistant execution and verification only.

## 4. Delegation log (Wave 2)

| Task | Prompt essentials | Outcome | Independent verification |
|------|-------------------|---------|--------------------------|
| Parameterized test: late-orders query vs `OrderStatus.isOpen()` | Native SQL for states with no domain method, no setters on `Order`, mutation must fail on the assertion, not on SQL | Correct test in `OrderRepositoryTest`, accurate report with a useful "not verified" list | Re-ran it (9/9 green); removed `CANCELLED` from the query in a **copy** of the project: fails for `CANCELLED` as expected |
| Mockito as explicit Java agent (Java 25 warning) | The documented Mockito §0.3 solution, the empty `<argLine/>` pitfall, no other plugins, warning shown before and after | Exactly the documented configuration, commented with its source | Old `pom.xml`: 2 warning lines; new one: 0, all tests green |
| Spotless + Palantir formatting | Pinned versions, two phases with a pause, check must fail before formatting, `git diff -w` for review | Config and formatting in two separate commits | `spotless:check` failed before, passed after; `git diff -w` showed only imports and line breaks; 50/50 tests green |
| `/new-migration` skill trial run | Add an optional `notes` column to `orders` | Correct `V2` file name, `V1` untouched, asked before touching `Order.java`, ran `verify` and not `spring-boot:run` | Trial migration deleted afterwards (not committed) |

## 5. What went wrong, and what caught it

| Event | Who | Caught by | Fix |
|-------|-----|-----------|-----|
| A delegated session ran a mutation check by editing a production file in place, then restored it with `git checkout --` without asking | Delegated session | Reviewing the session transcript | Rule extended in `CLAUDE.md`; `ask` permission rules |
| `ask` rules written only for the `Bash` tool; the session used the `PowerShell` tool and ran `git restore` without a prompt | Tutor | **A real test** of the rule | Mirrored every rule as `PowerShell(...)` |
| Test counts reported as success without checking failures (a red build was committed and pushed once) | Tutor | The next full build | Always read `Failures` and `Errors`, not the number of tests run |
| Claim that `@ParameterizedTest(name = "{0}")` shows state names in Surefire output | Tutor | The delegated session's report | Corrected |
| The skill's `VARCHAR(5000)` was flagged as a bug; the request actually said 5000 | Tutor (review) | Checking the real prompt in the transcript | Added the "compare with the actual request" check |
| An existing `SKILL.md` draft was overwritten without being read; `docker compose down -v` was run without asking | Tutor | Self-reported | `ask` rules |
| Three tests that could not fail (`save()` never null, `assertThat` without assertions, counting queries on an empty list) | Developer + tutor | Mutation checks | Mutation check made a project rule |
| Two unused imports left by refactorings, missed by every review | Developer | Spotless `removeUnusedImports` | Formatter enforced by the build |
| The first migration hook failed **open** (exit 0 on unparseable input) and `jq` on Windows appends `\r` | Tutor | Simulated hook inputs before enabling it | Fail closed; strip `\r` |
| The hook blocked writing this document: a session started before `jq` was installed had no `jq` on its PATH, so the hook failed closed on every `Write` | Environment | The hook itself | With the developer's explicit approval, the file was written through a shell command, which the hook does not cover (see §7) |

Most mistakes in Wave 2 were made by the assistant acting as tutor — the strongest argument in this document
for verifying every assistant, including the one explaining how to verify.

## 6. Assisted vs manual

**Observed facts**

- Wave 1: the developer wrote the production code; at the developer's request the assistant wrote most of the
  test classes, the README and the pull request description, all reviewed and verified before merging.
- Bugs in hand-written code were caught by tests within seconds (an inverted guard condition in `Order`,
  a missing `@Id`, a query attribute typo validated at startup).
- Delegation worked best when the prompt fixed the design decisions in advance (Tasks 2–4); without them the
  assistant had to choose on its own, and the riskiest choices (editing production code, `git checkout --`)
  happened exactly there.
- Verification was never free: each delegated task needed an independent re-run, and two of them needed a
  mutation check on a copy of the project.

## 7. Known limits

- `CLAUDE.md` and skills are guidance: an assistant can misread or work around them.
- Permission rules match the command as written for a given tool (`Bash` and `PowerShell` need separate rules)
  and are not a security boundary around the program.
- The migration hook covers `Edit` and `Write`, not shell commands (for example `sed -i` on a migration).
- A fail-closed hook depends on its environment: without `jq` on the session's PATH it blocks every file edit.
- Hooks and rules reduce the chance of mistakes; they do not replace review, tests and a human decision.