# Heroku Backup Implementation Design Document

## Overview

This document outlines the design and implementation plan for enabling database backups to run from within Heroku environments, rather than only from local PC environments.

**Status**: Design Phase
**Created**: 2025-11-14
**Goal**: Enable Java applications using postgres-object to perform database backups while running on Heroku

## Background

### Current State

The postgres-object library currently supports database backups through several executor classes:
- `DataBackupLocalExecutor` - Backups from local PostgreSQL instances
- `DataBackupRemoteExecutor` - Backups of remote Heroku databases (run from local PC)
- `DataBackupRemoteSchemaExecutor` - Schema-specific remote backups (run from local PC)

**Key limitation**: All backup executors assume execution from a Windows PC environment, not from within Heroku itself.

### The Problem

When attempting to run backups from a Heroku dyno (Java process running on Heroku), several issues prevent it from working:

1. **PGPASSFILE requirement**: Code requires PGPASSFILE environment variable pointing to a password file, but:
   - Heroku provides credentials via `DATABASE_URL` (connection string with embedded password)
   - Creating password files on ephemeral Heroku filesystem is unnecessary and problematic
   - Remote backups already pass full DATABASE_URL to pg_dump (which includes password)

2. **Windows-only assumptions**:
   - Hardcoded backslash path separators (`\\`) instead of cross-platform `File.separator`
   - Hardcoded `.exe` extensions on executables (pg_dump.exe, pg_restore.exe, aws.exe)
   - Heroku uses Linux, so executables have no extension

3. **Local PC assumptions**:
   - Assumes persistent filesystem for backup storage
   - Heroku dynos have ephemeral filesystems that reset on restart
   - Backups must go to persistent storage (AWS S3)

4. **GenericDataBackupExecutor limitation**:
   - Currently hardcoded to only support LocalDatabaseEnvironment
   - Cannot route to remote executors

### Discovery

We confirmed that on Heroku:
- `pg_dump` executable is available at `/usr/bin/pg_dump`
- Setting `POSTGRES17_PROGRAM_DIR=/usr/bin` should work
- DATABASE_URL already contains all connection credentials

## Architecture Decisions

### Phased Implementation Approach

We will implement this in two phases:

**Phase 1: Temp File Approach (Initial Implementation)**
- Backup to temp file in `/tmp` directory on Heroku
- Upload completed backup to AWS S3
- Clean up temp file after successful upload
- **Rationale**: Easier to debug, simpler error handling, safer for initial rollout

**Phase 2: Streaming Approach (Future Optimization)**
- Stream pg_dump output directly to AWS S3 via pipe
- No temp file needed
- More efficient for large databases
- **Rationale**: Better for very large databases, but adds complexity

**Why this approach?**
1. Database backups are ~55MB with regular archiving (well under Heroku dyno limits)
2. Temp file approach is proven and debuggable
3. Can validate implementation before optimizing
4. Streaming can be added later as optional enhancement
5. Incremental risk reduction

### OS-Agnostic Design

All backup/restore executors will be refactored to support both Windows and Linux:

1. **Path separators**: Use `File.separator` instead of hardcoded `\\`
2. **Executable detection**: Use OS detection to determine executable names
3. **Conditional PGPASSFILE**: Only require for local environments

### Environment-Based Routing

The backup system will intelligently route based on environment type:

```
DatabaseEnvironment
├── LocalDatabaseEnvironment → DataBackupLocalExecutor
└── RemoteDatabaseEnvironment → DataBackupRemoteExecutor (or new HerokuExecutor)
    └── HerokuDatabaseEnvironment
```

## Current State Analysis

### File Structure

**Base Classes**:
- `DataBackupExecutor.java` - Abstract base for all backup operations
- `DataRestoreExecutor.java` - Abstract base for all restore operations

**Implementations**:
- `DataBackupLocalExecutor.java` - Local PostgreSQL backups
- `DataBackupRemoteExecutor.java` - Remote Heroku backups (from PC)
- `DataBackupRemoteSchemaExecutor.java` - Schema-specific remote backups (from PC)
- `DataRestoreLocalExecutor.java` - Restore to local PostgreSQL
- `DataRestoreRemoteExecutor.java` - Restore to Heroku (via S3)
- `DataRestoreRemoteSchemaExecutor.java` - Schema-specific Heroku restore

**Generic Executors**:
- `GenericDataBackupExecutor.java` - Routes to appropriate backup executor
- `GenericDataRestoreExecutor.java` - Routes to appropriate restore executor

### Issues Identified

#### 1. PGPASSFILE Requirement (DataBackupExecutor.java:46-51)

```java
String postgres_pgpass = EnvironmentChecker.getOrThrow("PGPASSFILE");

logger.info("Backing up from environment '" + backupEnvironment.getEnvironmentName() + "'");

File pgpass_file = new File(postgres_pgpass);
assert pgpass_file.exists() && pgpass_file.isFile();
```

**Issue**: Required for ALL environments, even though remote backups don't use it.

**Impact**: Blocks Heroku execution even though DATABASE_URL contains credentials.

**Solution**: Make conditional based on `backupEnvironment.isLocal()`

#### 2. Windows Path Separators

**Locations**:
- DataBackupExecutor.java:59, 65, 72, 82
- DataRestoreExecutor.java (similar locations)
- DataArchiver.java:197, 203, 211, 217

**Example**:
```java
File app_backup_dir = new File(backup_dir_location + "\\" + folderName);
File env_backup_dir = new File(app_backup_dir.getPath() + "\\" + backupEnvironment.getEnvironmentName());
```

**Solution**: Replace all with `File.separator`

#### 3. Hardcoded Executable Extensions

**Locations**:
- DataBackupLocalExecutor.java:23 - `postgres_program_dir + "\\pg_dump.exe"`
- DataBackupRemoteExecutor.java:25 - `postgres_program_dir + "\\pg_dump.exe"`
- DataRestoreLocalExecutor.java:36 - `postgres_program_dir + "\\pg_restore.exe"`
- DataRestoreRemoteExecutor.java:59, 89, 119 - `heroku.cmd`, `aws.exe`
- DataRestoreRemoteSchemaExecutor.java:42 - `postgres_program_dir + "\\pg_restore.exe"`

**Solution**: Add OS-detection utility methods:
```java
protected String getPgDumpExecutable() {
    return isWindows() ? "pg_dump.exe" : "pg_dump";
}

protected String getPgRestoreExecutable() {
    return isWindows() ? "pg_restore.exe" : "pg_restore";
}

protected String getAwsExecutable() {
    return isWindows() ? "aws.exe" : "aws";
}

protected String getHerokuExecutable() {
    return isWindows() ? "heroku.cmd" : "heroku";
}

private boolean isWindows() {
    return System.getProperty("os.name").toLowerCase().contains("win");
}
```

#### 4. GenericDataBackupExecutor Limitation (lines 30-35)

```java
public void runUpdate() throws MissingEnvException, InterruptedException, IOException, SQLException {
    LocalDatabaseEnvironment localDatabaseEnvironment = (LocalDatabaseEnvironment) databaseEnvironment;

    DataBackupExecutor executor = new DataBackupLocalExecutor(localDatabaseEnvironment, "PostgresObject");
    executor.runUpdate();
}
```

**Issue**: Hardcoded to only support LocalDatabaseEnvironment

**Solution**: Add environment type detection:
```java
public void runUpdate() throws MissingEnvException, InterruptedException, IOException, SQLException {
    DataBackupExecutor executor;

    if (databaseEnvironment.isLocal()) {
        executor = new DataBackupLocalExecutor(
            (LocalDatabaseEnvironment) databaseEnvironment, "PostgresObject");
    } else {
        executor = new DataBackupRemoteExecutor(
            (RemoteDatabaseEnvironment) databaseEnvironment, "PostgresObject");
    }

    executor.runUpdate();
}
```

#### 5. Hardcoded S3 Bucket (DataRestoreRemoteExecutor.java:111)

```java
String bucketName = "s3://mediamogulbackups";
```

**Issue**: Application-specific bucket name hardcoded

**Solution**: Make configurable via environment variable with this as default

## Phase 1 Implementation Plan

### 1. Make DataBackupExecutor OS-Agnostic

**File**: `src/main/java/com/mayhew3/postgresobject/db/DataBackupExecutor.java`

**Changes**:

1. Add OS detection utility methods (lines ~93-110):
```java
protected String getPgDumpExecutable() {
    return isWindows() ? "pg_dump.exe" : "pg_dump";
}

protected String getPgRestoreExecutable() {
    return isWindows() ? "pg_restore.exe" : "pg_restore";
}

protected boolean isWindows() {
    return System.getProperty("os.name").toLowerCase().contains("win");
}
```

2. Make PGPASSFILE conditional (lines 46-51):
```java
String postgres_pgpass = null;
if (backupEnvironment.isLocal()) {
    postgres_pgpass = EnvironmentChecker.getOrThrow("PGPASSFILE");
    File pgpass_file = new File(postgres_pgpass);
    assert pgpass_file.exists() && pgpass_file.isFile();
}
```

3. Replace path separators (lines 59, 65, 72, 82):
```java
File app_backup_dir = new File(backup_dir_location + File.separator + folderName);
File env_backup_dir = new File(app_backup_dir.getPath() + File.separator + backupEnvironment.getEnvironmentName());
File schema_backup_dir = backupEnvironment.getSchemaName() == null ? env_backup_dir :
    new File(env_backup_dir.getPath() + File.separator + backupEnvironment.getSchemaName());
String fullBackupPath = schema_backup_dir.getPath() + File.separator + formattedDate + ".dump";
```

### 2. Update DataBackupLocalExecutor

**File**: `src/main/java/com/mayhew3/postgresobject/db/DataBackupLocalExecutor.java`

**Change** (line 23):
```java
List<String> args = Lists.newArrayList(
    postgres_program_dir + File.separator + getPgDumpExecutable(),
    "--host=localhost",
    // ... rest of args
```

### 3. Update DataBackupRemoteExecutor

**File**: `src/main/java/com/mayhew3/postgresobject/db/DataBackupRemoteExecutor.java`

**Change** (line 25):
```java
ProcessBuilder processBuilder = new ProcessBuilder(
    postgres_program_dir + File.separator + getPgDumpExecutable(),
    "--format=custom",
    "--verbose",
    "--no-privileges",
    "--no-owner",
    "--file=" + fullBackupPath,
    "\"" + databaseUrl + "\"");
```

### 4. Update DataRestoreExecutor

**File**: `src/main/java/com/mayhew3/postgresobject/db/DataRestoreExecutor.java`

**Changes**:

1. Add OS detection methods (similar to DataBackupExecutor)

2. Make PGPASSFILE conditional (around line 61):
```java
String postgres_pgpass = null;
if (restoreEnvironment.isLocal()) {
    postgres_pgpass = EnvironmentChecker.getOrThrow("PGPASSFILE");
    File pgpass_file = new File(postgres_pgpass);
    assert pgpass_file.exists() && pgpass_file.isFile();
}
```

3. Replace path separators throughout

### 5. Update All Restore Executors

Apply similar OS-agnostic changes to:
- `DataRestoreLocalExecutor.java` - Use `getPgRestoreExecutable()`
- `DataRestoreRemoteExecutor.java` - Use `getPgRestoreExecutable()`, `getAwsExecutable()`, `getHerokuExecutable()`
- `DataRestoreRemoteSchemaExecutor.java` - Use `getPgRestoreExecutable()`

### 6. Update GenericDataBackupExecutor

**File**: `src/main/java/com/mayhew3/postgresobject/db/GenericDataBackupExecutor.java`

**Change** (lines 30-35):
```java
public void runUpdate() throws MissingEnvException, InterruptedException, IOException, SQLException {
    DataBackupExecutor executor;

    if (databaseEnvironment.isLocal()) {
        executor = new DataBackupLocalExecutor(
            (LocalDatabaseEnvironment) databaseEnvironment, "PostgresObject");
    } else {
        executor = new DataBackupRemoteExecutor(
            (RemoteDatabaseEnvironment) databaseEnvironment, "PostgresObject");
    }

    executor.runUpdate();
}
```

### 7. Handle Heroku Ephemeral Filesystem

**Consideration**: Heroku dynos have ephemeral filesystems that reset on restart.

**For Phase 1**, we'll use `/tmp` for temporary backup storage, then:
1. Application code uploads to AWS S3 after backup completes
2. Application code cleans up `/tmp` file after successful upload

**Required environment variables on Heroku**:
- `DB_BACKUP_DIR=/tmp` - Temporary storage location
- `AWS_ACCESS_KEY_ID` - For S3 upload
- `AWS_SECRET_ACCESS_KEY` - For S3 upload
- `S3_BACKUP_BUCKET` - Destination bucket (or make configurable)

**Note**: The backup executor creates the file, but the calling application is responsible for:
1. Uploading the backup file to S3
2. Cleaning up the temp file

Alternatively, we could extend `DataBackupRemoteExecutor` to handle S3 upload automatically.

## Environment Variables

### Required on Heroku

| Variable | Value | Purpose |
|----------|-------|---------|
| `DATABASE_URL` | (Provided by Heroku) | PostgreSQL connection string with credentials |
| `POSTGRES17_PROGRAM_DIR` | `/usr/bin` | Location of pg_dump executable |
| `DB_BACKUP_DIR` | `/tmp` | Temporary backup storage |

### Required for S3 Upload (if auto-upload implemented)

| Variable | Value | Purpose |
|----------|-------|---------|
| `AWS_ACCESS_KEY_ID` | (Your AWS key) | AWS authentication |
| `AWS_SECRET_ACCESS_KEY` | (Your AWS secret) | AWS authentication |
| `S3_BACKUP_BUCKET` | `your-backup-bucket` | S3 bucket for backups |

### Optional

| Variable | Value | Purpose |
|----------|-------|---------|
| `AWS_PROGRAM_DIR` | `/usr/local/bin` | Location of AWS CLI (if not in PATH) |

### NOT Required on Heroku

| Variable | Reason |
|----------|--------|
| `PGPASSFILE` | Credentials in DATABASE_URL |

## Testing Strategy

### 1. Local Windows Testing (Regression)
- Ensure all existing Windows-based backups still work
- Verify PGPASSFILE still used correctly for local backups
- Test both local and remote backup executors

### 2. Local Linux Testing (CI)
- Run tests in CI environment (Linux)
- Verify executable detection works (no .exe extension)
- Verify path separators work correctly

### 3. Heroku Staging Testing
- Deploy to Heroku staging environment
- Set required environment variables
- Run backup from within Heroku dyno
- Verify backup file created in `/tmp`
- Verify backup can be restored successfully

### 4. Integration Testing
- Test full backup → S3 upload → cleanup workflow
- Test error handling (S3 upload failures, etc.)
- Test with different PostgreSQL versions

## Phase 2 Future Enhancements

### Streaming to S3

**Goal**: Eliminate temp file by streaming pg_dump output directly to S3.

**Approach**:
```bash
pg_dump [args] --format=custom | aws s3 cp - s3://bucket/backup.dump
```

**In Java**:
```java
// Start pg_dump process
ProcessBuilder pgDumpBuilder = new ProcessBuilder(pgDumpCmd);
Process pgDump = pgDumpBuilder.start();

// Start aws s3 cp process
ProcessBuilder s3UploadBuilder = new ProcessBuilder(awsS3Cmd);
Process s3Upload = s3UploadBuilder.start();

// Pipe pg_dump stdout to aws stdin
InputStream backupStream = pgDump.getInputStream();
OutputStream s3InputStream = s3Upload.getOutputStream();

// Copy stream
backupStream.transferTo(s3InputStream);
```

**Configuration**:
- Environment variable: `BACKUP_STRATEGY=stream|file` (default: `file`)
- Or automatic based on backup size threshold

**Benefits**:
- No disk space needed
- Faster for very large databases
- No temp file cleanup needed

**Trade-offs**:
- Slightly more complex error handling
- Can't verify backup before uploading
- If S3 fails mid-stream, must re-run entire pg_dump

### Configurable S3 Bucket

Make S3 bucket name configurable instead of hardcoded:

```java
String bucketName = EnvironmentChecker.getOptional("S3_BACKUP_BUCKET")
    .orElse("s3://mediamogulbackups");
```

### Automatic Backup Upload

Extend `DataBackupRemoteExecutor` to automatically upload to S3 after backup:

```java
@Override
void executeBackup(String fullBackupPath) throws IOException, InterruptedException, MissingEnvException, SQLException {
    // Execute pg_dump
    String databaseUrl = remoteDatabaseEnvironment.getDatabaseUrl();
    // ... pg_dump execution ...

    // If running on Heroku and backup succeeded, upload to S3
    if (isHerokuEnvironment()) {
        uploadToS3(fullBackupPath);
        cleanupTempFile(fullBackupPath);
    }
}
```

## Implementation Checklist

### Phase 1: Make It Work

- [ ] Add OS detection utility methods to DataBackupExecutor
- [ ] Make PGPASSFILE conditional in DataBackupExecutor
- [ ] Replace path separators in DataBackupExecutor
- [ ] Update DataBackupLocalExecutor with OS-agnostic executable paths
- [ ] Update DataBackupRemoteExecutor with OS-agnostic executable paths
- [ ] Update DataRestoreExecutor (PGPASSFILE, path separators, OS detection)
- [ ] Update DataRestoreLocalExecutor with OS-agnostic paths
- [ ] Update DataRestoreRemoteExecutor with OS-agnostic paths
- [ ] Update DataRestoreRemoteSchemaExecutor with OS-agnostic paths
- [ ] Update GenericDataBackupExecutor to route based on environment type
- [ ] Write unit tests for OS detection
- [ ] Test on local Windows environment (regression)
- [ ] Test on CI Linux environment
- [ ] Document Heroku environment variables needed
- [ ] Test on Heroku staging environment
- [ ] Document S3 upload process for applications

### Phase 2: Optimize (Future)

- [ ] Implement streaming backup option
- [ ] Add BACKUP_STRATEGY configuration
- [ ] Make S3 bucket configurable
- [ ] Consider automatic S3 upload in executor
- [ ] Performance comparison: temp file vs streaming
- [ ] Update documentation with streaming approach

## Open Questions

1. **S3 Upload Responsibility**: Should the backup executor automatically upload to S3, or is that the application's responsibility?
   - **Option A**: Executor handles it (simpler for apps, more complex executor)
   - **Option B**: Application handles it (more flexible, apps must implement)

2. **S3 Bucket Configuration**: Should S3 bucket name be:
   - **Option A**: Environment variable (most flexible)
   - **Option B**: Constructor parameter (explicit)
   - **Option C**: Keep hardcoded with env var override (backward compatible)

3. **Error Handling**: If S3 upload fails, should we:
   - **Option A**: Leave file in /tmp for retry (might fill disk)
   - **Option B**: Delete file and fail (requires full pg_dump retry)
   - **Option C**: Retry S3 upload N times before giving up

4. **Restore Support**: Do we need restore FROM Heroku, or just backups?
   - If yes, restore has similar challenges (temp file in /tmp, download from S3)

## References

- Current implementation: `src/main/java/com/mayhew3/postgresobject/db/`
- PostgreSQL pg_dump documentation: https://www.postgresql.org/docs/current/app-pgdump.html
- Heroku PostgreSQL: https://devcenter.heroku.com/articles/heroku-postgresql
- AWS S3 CLI: https://docs.aws.amazon.com/cli/latest/reference/s3/

## Changelog

- 2025-11-14: Initial design document created
