package com.jetbrains.edu.csharp

import com.jetbrains.edu.learning.TaskBuilder
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils

val solutionFile = GeneratorUtils.getInternalTemplateText(CSharpCourseBuilder.SOLUTION_FILE_TEMPLATE, mapOf())
val csprojWithTests = GeneratorUtils.getInternalTemplateText(
  CSharpCourseBuilder.PROJECT_FILE_TEMPLATE,
  mapOf(CSharpCourseBuilder.VERSION_VARIABLE to DEFAULT_DOT_NET)
)

fun TaskBuilder.csTaskTestFiles(csprojName: String) {
  taskFile("$csprojName.csproj", csprojWithTests)
  dir("src") {
    taskFile("Task.cs", GeneratorUtils.getInternalTemplateText(CSharpConfigurator.TASK_CS))
  }
  dir("test") {
    taskFile("Test.cs", GeneratorUtils.getInternalTemplateText(CSharpConfigurator.TEST_CS))
  }
}

fun TaskBuilder.csTaskFiles(csprojName: String) {
  taskFile("$csprojName.csproj", csprojWithTests)
  dir("src") {
    taskFile("Task.cs", GeneratorUtils.getInternalTemplateText(CSharpConfigurator.TASK_CS))
  }
}
