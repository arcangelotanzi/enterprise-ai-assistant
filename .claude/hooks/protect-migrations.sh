#!/usr/bin/env bash
# PreToolUse hook for Edit|Write.
# Versioned Flyway migrations (db/migration/V*.sql) are immutable once they exist:
# creating a new one is allowed, editing an existing one is blocked.
# Exit 2 = block the tool call; stderr is shown to Claude as the reason.

# Fail closed: without jq we cannot inspect the call, so we block instead of letting it through.
if ! command -v jq >/dev/null 2>&1; then
  echo "protect-migrations hook: jq is not installed or not on PATH, blocking file edits until it is." >&2
  exit 2
fi

input=$(cat)
# tr -d '\r': jq on Windows ends its output with CRLF
if ! file=$(jq -r '.tool_input.file_path // empty' <<<"$input"); then
  echo "protect-migrations hook: could not parse the hook input, blocking to be safe." >&2
  exit 2
fi
file=$(printf '%s' "$file" | tr -d '\r')
[ -z "$file" ] && exit 0

# Windows paths arrive with backslashes (C:\dev\...); convert to a POSIX path for matching
path=$(cygpath -u "$file" 2>/dev/null || printf '%s' "$file")

case "$path" in
  */src/main/resources/db/migration/V*.sql)
    if [ -f "$path" ]; then
      echo "Blocked: $(basename "$path") already exists. Versioned Flyway migrations are immutable." \
           "Create the next V<n+1> migration instead (see /new-migration)." >&2
      exit 2
    fi
    ;;
esac

exit 0
