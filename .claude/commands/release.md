# Release Skill

Create a new release for postgres-object.

## Arguments

- `$ARGUMENTS` - Optional: version number (e.g., "0.22.10") and/or `--stable` flag

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
   - **Default: pre-release** - always add `--prerelease` flag unless `--stable` is specified

## Release Types

- **Pre-release (default)**: All releases are pre-releases by default for testing before making official
- **Stable release**: Add `--stable` to arguments to create a full/official release

## Example Usage

```
/release 0.23.0              (pre-release)
/release 0.23.0 --stable     (stable release)
/release                     (auto-bump patch, pre-release)
/release --stable            (auto-bump patch, stable release)
```
