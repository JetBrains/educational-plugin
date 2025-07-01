# Technical Context

## Technology Stack

### 1. Core Technologies
- Kotlin: Primary development language
- Java: Support for JVM platform
- Python: Target language support
- Gradle: Build system and dependency management

### 2. Platform Integration
- IntelliJ Platform SDK: Core IDE platform
- Python Plugin: Python language support
- TOML Plugin: Configuration file support

### 3. Testing Framework
- JUnit: Unit testing framework
- IntelliJ Platform Test Framework: IDE integration testing

## Dependencies

### 1. Internal Dependencies
```
edu-python
├── educational-core    # Core educational plugin functionality
├── Edu-Python:Idea    # IDEA-specific implementation
└── Edu-Python:PyCharm # PyCharm-specific implementation
```

### 2. External Dependencies
- IntelliJ Platform
  - Base IDE functionality
  - UI components
  - Project model
  - Editor services

- Python Plugin
  - Python language support
  - SDK management
  - Virtual environment handling
  - Code execution

- TOML Plugin
  - Configuration file support
  - Used in testing

### 3. Test Dependencies
- JUnit Test Framework
- IntelliJ Test Framework
- Mock Frameworks
- Test Utilities

## Build System

### 1. Gradle Configuration
- Uses Gradle Kotlin DSL
- Implements plugin module conventions
- Manages IDE plugin dependencies
- Configures test environment

### 2. Module Structure
```
Edu-Python/
├── src/          # Main source code
├── testSrc/      # Test source code
├── resources/    # Resource files
└── build/        # Build outputs
```

### 3. Build Features
- IDE Plugin Packaging
- Resource Management
- Test Execution
- Dependency Resolution

## Development Environment

### 1. Required Tools
- JDK 11 or later
- Gradle 7.x or later
- IntelliJ IDEA
- Python Plugin
- TOML Plugin

### 2. IDE Configuration
- Plugin Development Environment
- Python SDK Integration
- Test Framework Support

### 3. Development Support
- Hot Reload Capability
- Debug Configuration
- Test Running
- Resource Management

## Integration Points

### 1. IDE Integration
- IntelliJ Platform API
- Editor Services
- Project Model
- UI Components

### 2. Language Integration
- Python Language Support
- SDK Management
- Virtual Environments
- Code Execution

### 3. Plugin Integration
- Educational Core Plugin
- Python Plugin
- TOML Plugin
- Test Framework