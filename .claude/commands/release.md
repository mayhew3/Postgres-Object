# Release Skill

Create a new release for postgres-object.

## Arguments

- `$ARGUMENTS` - Optional: version number (e.g., "0.22.10") and/or flags like "--prerelease"

## Version Format

- Versions follow semver: `MAJOR.MINOR.PATCH` (e.g., `0.22.9`)
- **No "v" prefix** - use `0.22.9` not `v0.22.9`
- Version is stored in `build.gradle` in the `version` property

## Workflow

1. **Determine version**: If not provided in arguments, read current version from `build.gradle` and bump the patch number
2. **Update build.gradle**: Change the `version = 'X.Y.Z'` line
3. **Run tests**: Execute `./test-local.bat` (Windows) or `./test-local.sh` (Linux/Mac) to verify everything works
4. **Commit**: Stage and commit with message format: `Bump version to X.Y.Z and <brief description of changes>`
5. **Push**: Push to origin
6. **Create GitHub release**: Use `gh release create` with:
   - Tag: version number (no "v" prefix)
   - Title: version number
   - Notes: summary of changes since last release
   - Add `--prerelease` flag if specified in arguments or if this is a pre-release

## Pre-release

Add `--prerelease` to arguments to mark the release as a pre-release. This is useful for:
- Testing releases before making them official
- Beta versions
- Release candidates

## Example Usage

```
/release 0.23.0
/release 0.23.0 --prerelease
/release --prerelease  (auto-bump patch version)
```
