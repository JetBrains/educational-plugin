package com.jetbrains.edu.csharp

import com.jetbrains.edu.learning.TaskBuilder
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils

val solutionFile = GeneratorUtils.getInternalTemplateText(CSharpCourseBuilder.SOLUTION_FILE_TEMPLATE, mapOf())
val csprojWithTests = GeneratorUtils.getInternalTemplateText(
  CSharpCourseBuilder.PROJECT_FILE_WITH_TESTS_TEMPLATE,
  mapOf(CSharpCourseBuilder.VERSION_VARIABLE to DEFAULT_DOT_NET)
)
val csprojWithoutTests = GeneratorUtils.getInternalTemplateText(
  CSharpCourseBuilder.PROJECT_FILE_NO_TESTS_TEMPLATE,
  mapOf(CSharpCourseBuilder.VERSION_VARIABLE to DEFAULT_DOT_NET)
)

fun TaskBuilder.csTaskTestFiles(csprojName: String) {
  taskFile("$csprojName.csproj", csprojWithTests)
  taskFile("Task.cs", taskFileContents(csprojName))
  taskFile("Test.cs", testFileContents(csprojName))
}

fun TaskBuilder.csTaskFiles(csprojName: String) {
  taskFile("$csprojName.csproj", csprojWithoutTests)
  taskFile("Program.cs", mainFileContents(csprojName))
}

fun taskFileContents(namespace: String): String = GeneratorUtils.getInternalTemplateText(
  CSharpConfigurator.TASK_CS, mapOf(CSharpCourseBuilder.NAMESPACE_VARIABLE to namespace)
)

fun mainFileContents(namespace: String): String = GeneratorUtils.getInternalTemplateText(
  CSharpConfigurator.MAIN_CS, mapOf(CSharpCourseBuilder.NAMESPACE_VARIABLE to namespace)
)

fun testFileContents(namespace: String): String = GeneratorUtils.getInternalTemplateText(
  CSharpConfigurator.TEST_CS,
  mapOf(
    CSharpCourseBuilder.NAMESPACE_VARIABLE to namespace,
    CSharpCourseBuilder.TEST_NAME_VARIABLE to namespace.filter { it != '.' } + "Test")
)