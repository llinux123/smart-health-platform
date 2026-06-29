---
trigger: model_decision
description: 编辑 Java 文件或构建配置后，需要执行格式化、代码检查或编译验证时
---

# Java Hooks

> This file extends [common/hooks.md](../common/hooks.md) with Java-specific content.

## Post-Edit Verification Checklist

After editing Java source files or build configuration, perform the following checks in order:

### 1. Code Formatting

- **google-java-format**: Auto-format `.java` files after edit
- **checkstyle**: Run style checks to enforce project conventions

```bash
# Format single file
google-java-format -i path/to/File.java

# Run checkstyle (Maven)
./mvnw checkstyle:check
```

### 2. Compilation Verification

Always verify compilation after structural changes:

```bash
# Maven
./mvnw compile -q

# Gradle
./gradlew compileJava -q
```

### 3. Scope by File Type

| File Pattern | Action |
|---|---|
| `**/*.java` | Format + checkstyle + compile |
| `pom.xml` | Compile to verify dependency changes |
| `build.gradle` / `build.gradle.kts` | Compile to verify build config changes |
| `application.yml` | Verify property names, restart to validate |
