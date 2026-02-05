# .class Ignore Fix Report

## Root cause

Compiled artifacts under `target/` were already tracked in Git. Ignore rules in `.gitignore` were present, but tracked files are not ignored until removed from the index.

## Evidence

- `git ls-files | findstr /i "\.class$"` returned tracked files such as:
  - `target/classes/unrn/rating/api/RatingController.class`
- After untracking, `git check-ignore -v target/classes/unrn/rating/api/RatingController.class` reports:
  - `.gitignore:2:/target/ target/classes/unrn/rating/api/RatingController.class`

## Changes made

- No .gitignore changes were required; `target/`, `*.class`, `*.jar`, `*.war`, `*.ear`, `.classpath`, `.project`, `.settings/`, `.idea/`, `*.iml`, `.vscode/`, `logs/`, `*.log` are already present.
- Removed tracked build artifacts from the Git index.

## Commands executed

```
# Verify tracked artifacts
 git ls-files | findstr /i "\.class$"
 git ls-files | findstr /i "target/"

# Verify ignore rule
 git check-ignore -v target/classes/unrn/rating/api/RatingController.class

# Untrack artifacts (keep on disk)
 git rm -r --cached "target"
```

## Verification

```
 git ls-files | findstr /i "\.class$"   # no output
 git ls-files | findstr /i "target/"    # no output
 git check-ignore -v target/classes/unrn/rating/api/RatingController.class
 git status -s
```

## Final expected state

- No `.class` or `target/` artifacts are tracked in this repo.
- `target/` contents are ignored and no longer show in Source Control.
