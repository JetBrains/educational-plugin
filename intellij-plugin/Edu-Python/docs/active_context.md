# Active Context

## Current State
The Edu-Python module is actively maintained and provides comprehensive Python support for the JetBrains Educational plugin. The module consists of several key components:

### Core Components
1. Course Configuration
   - PyNewConfigurator: Handles Python course configuration
   - PyNewCourseBuilder: Manages Python course creation and structure

2. Task Management
   - PyNewTaskCheckerProvider: Orchestrates task checking
     - PyCodeExecutor: Handles Python code execution
     - PyEnvironmentChecker: Verifies Python environment
     - PyNewEduTaskChecker: Implements task checking logic

3. Project Setup
   - Python SDK Management
   - Virtual Environment Support
   - Project Structure Templates

### Module Structure
```
Edu-Python/
├── Idea/           # IDEA-specific implementation
├── PyCharm/        # PyCharm-specific implementation
├── src/            # Core source code
├── testSrc/        # Test source code
├── resources/      # Resource files
└── docs/           # Module documentation
```

## Active Development Areas
1. Task Checking System
   - Code execution
   - Environment verification
   - Test result processing

2. Course Creation Tools
   - Template management
   - Project structure generation
   - Task file handling

3. IDE Integration
   - PyCharm support
   - IDEA support
   - SDK management

## Current Focus
The module currently focuses on:
1. Maintaining Python course support
2. Enhancing task checking capabilities
3. Improving IDE integration
4. Supporting Python educational features

## Integration Points
1. Educational Core Plugin
   - Task management
   - Course structure
   - Educational features

2. Python Plugin
   - SDK handling
   - Code execution
   - Environment management

3. IDE Platforms
   - PyCharm integration
   - IDEA integration