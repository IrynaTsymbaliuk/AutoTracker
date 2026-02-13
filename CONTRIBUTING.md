# Contributing to AutoTracker

First off, thank you for considering contributing to AutoTracker! It's people like you that make AutoTracker such a great tool for the Android community.

## Code of Conduct

This project and everyone participating in it is governed by respect, professionalism, and inclusivity. By participating, you are expected to uphold these values.

## How Can I Contribute?

### Reporting Bugs

Before creating bug reports, please check the existing issues to avoid duplicates. When you create a bug report, include as many details as possible:

**Use the bug report template** which includes:
- A clear and descriptive title
- Exact steps to reproduce the problem
- Expected behavior
- Actual behavior
- Device information (Android version, device model)
- AutoTracker version
- Code snippets or logs if applicable
- Screenshots if relevant

### Suggesting Enhancements

Enhancement suggestions are tracked as GitHub issues. When creating an enhancement suggestion, please include:

- A clear and descriptive title
- A detailed description of the proposed functionality
- Explain why this enhancement would be useful
- List any alternative solutions or features you've considered

### Pull Requests

1. **Fork the repository** and create your branch from `main`
2. **Follow the coding style** of the project
3. **Write clear commit messages**
4. **Include tests** for new functionality
5. **Update documentation** as needed
6. **Ensure tests pass** before submitting

#### Pull Request Process

1. Update the README.md with details of changes if applicable
2. Follow the pull request template
3. The PR will be merged once you have the sign-off of at least one maintainer

## Development Setup

### Prerequisites

- JDK 11 or higher
- Android Studio Iguana or later
- Android SDK with API 26+
- Git

### Setting Up Your Development Environment

1. **Fork and clone the repository:**
   ```bash
   git clone git@github.com:YOUR_USERNAME/AutoTracker.git
   cd AutoTracker
   ```

2. **Open in Android Studio:**
   - Open Android Studio
   - Select "Open an Existing Project"
   - Navigate to the cloned directory

3. **Build the project:**
   ```bash
   ./gradlew build
   ```

4. **Run tests:**
   ```bash
   ./gradlew test
   ```

## Project Structure

```
AutoTracker/
├── core/                          # Core module with base interfaces
│   └── src/main/java/com/auto/tracker/core/
│       ├── DataCollector.kt       # Main interface
│       ├── StepData.kt           # Data models
│       └── PermissionState.kt    # Permission states
├── collector-steps/               # Steps collector implementation
│   └── src/main/java/com/auto/tracker/collector/steps/
│       ├── StepsCollector.kt     # Main collector
│       ├── StepsSyncManager.kt   # Sync logic
│       ├── StepsSyncWorker.kt    # Background worker
│       └── db/                   # Database layer
└── [future collector modules]
```

## Coding Standards

### Kotlin Style Guide

- Follow the [official Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html)
- Use meaningful variable and function names
- Keep functions small and focused
- Add KDoc comments for public APIs

### Example:

```kotlin
/**
 * Collects step data from Health Connect API.
 *
 * @param context Application context
 * @return Flow of step data for today
 */
fun collectSteps(context: Context): Flow<List<StepsData>>
```

### Code Organization

- One class per file
- Group related functionality
- Use package structure that reflects feature organization
- Keep internal implementation details private or internal

### Naming Conventions

- Classes: PascalCase (`StepsCollector`)
- Functions: camelCase (`checkPermissions`)
- Constants: UPPER_SNAKE_CASE (`PERMISSIONS`)
- Variables: camelCase (`stepsData`)

## Testing

### Writing Tests

- Write unit tests for all business logic
- Write integration tests for data layer
- Use meaningful test names that describe what is being tested

```kotlin
@Test
fun `checkPermissions returns Granted when all permissions are granted`() {
    // Arrange
    val collector = StepsCollector.getInstance(context)

    // Act
    val result = collector.checkPermissions()

    // Assert
    assertEquals(PermissionState.Granted, result)
}
```

### Running Tests

```bash
# Run all tests
./gradlew test

# Run tests for specific module
./gradlew :core:test
./gradlew :collector-steps:test

# Run with coverage
./gradlew jacocoTestReport
```

## Adding New Data Collectors

To add a new data collector (e.g., sleep data, heart rate):

1. **Create a new module:**
   ```
   collector-[data-type]/
   ```

2. **Implement the DataCollector interface:**
   ```kotlin
   class SleepCollector : DataCollector<SleepData> {
       override suspend fun checkPermissions(): PermissionState
       override suspend fun sync(): Result<Unit>
       override fun observe(): Flow<List<SleepData>>
   }
   ```

3. **Define data models in core module** (if generic) or in your collector module
4. **Add database layer** using Room
5. **Implement sync manager** for data synchronization
6. **Add background worker** if needed
7. **Write tests**
8. **Update documentation**

## Documentation

- Add KDoc comments to all public APIs
- Update README.md for new features
- Include code examples in documentation
- Keep documentation up-to-date with code changes

## Commit Messages

Follow the conventional commits specification:

- `feat:` - New feature
- `fix:` - Bug fix
- `docs:` - Documentation changes
- `refactor:` - Code refactoring
- `test:` - Adding or updating tests
- `chore:` - Maintenance tasks

Examples:
```
feat: add sleep data collection support
fix: resolve sync token persistence issue
docs: update installation instructions
refactor: simplify permission checking logic
test: add unit tests for StepsSyncManager
chore: update dependencies to latest versions
```

## Versioning

We use [Semantic Versioning](https://semver.org/):

- MAJOR version for incompatible API changes
- MINOR version for backwards-compatible functionality
- PATCH version for backwards-compatible bug fixes

## Publishing Changes

Only maintainers can publish new versions. If you're a contributor:

1. Submit your PR
2. Wait for review and approval
3. Maintainers will handle version bumping and publishing

## Communication

- **GitHub Issues**: For bug reports and feature requests
- **GitHub Discussions**: For questions and general discussions
- **Pull Request Comments**: For code review and implementation discussions

## Recognition

Contributors will be recognized in:
- GitHub contributors page
- Release notes for significant contributions
- Special acknowledgments for major features

## Questions?

Don't hesitate to ask questions! You can:
- [Start a discussion](https://github.com/IrynaTsymbaliuk/AutoTracker/discussions) on GitHub
- Comment on relevant issues or pull requests
- Reach out to maintainers via discussions or issues

## License

By contributing, you agree that your contributions will be licensed under the Apache License 2.0.

---

Thank you for contributing to AutoTracker! 🎉
