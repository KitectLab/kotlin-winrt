# AGENTS

## Collaboration Rules

1. Every completed change item must be committed immediately after it is finished.
2. Each change item must be committed on the current branch. Do not mix unrelated work across branches.
3. Keep every commit atomic. One commit should represent one self-contained change with a clear purpose.
4. Do not bundle unrelated edits into the same commit.
5. If a task contains multiple independent change items, split them into separate branches and separate commits.
6. This project must be built and tested in a Windows environment.
7. When running Gradle in the required Windows environment from WSL, use `./.agent_scripts/run_windows_gradle.sh <gradle-args...>` instead of invoking `gradlew.bat` directly.
