package com.jetbrains.edu.slow.checkerTests

import com.intellij.ide.starter.community.model.BuildType
import com.intellij.ide.starter.ide.IdeProductProvider
import com.intellij.ide.starter.junit5.hyphenateWithClass
import com.intellij.ide.starter.models.TestCase
import com.intellij.ide.starter.plugins.PluginConfigurator
import com.intellij.ide.starter.project.NoProject
import com.intellij.ide.starter.runner.IDECommandLine
import com.intellij.ide.starter.runner.Starter
import com.jetbrains.edu.coursecreator.validation.ValidationCase
import com.jetbrains.edu.coursecreator.validation.ValidationResultNode
import com.jetbrains.edu.coursecreator.validation.ValidationSuite
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DynamicNode
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.TestInfo
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.absolutePathString
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.test.assertNotNull

private const val PLAIN_COURSE_LOCAL_PATH =
  "testData/PlainTextCourseTest"

private const val EXPECTED_REPORT_RELATIVE =
  "testData/expectedJsons/plainTextTest.json"

// Temporarily placed in 261 source set because of some incompatibilities between 261 and 262 in starter framework
// It will be fixed separately
class JBAcademyCheckCourseTest {

  @TestFactory
  fun checkCourse(testInfo: TestInfo): List<DynamicNode> {
    val pluginPath: Path = resolvePluginPath()
    val courseLocal = resolveCourseLocal()
    val workspaceDir = Files.createTempDirectory("validateCourse-workspace")

    val context = Starter.newContext(
      testInfo.hyphenateWithClass(),
      TestCase(
        IdeProductProvider.IU.copy(version = "2026.1.1", buildType = BuildType.RELEASE.type), NoProject)
    ).apply {
      PluginConfigurator(this).installPluginFromPath(pluginPath)
      applyVMOptionsPatch {
        addSystemProperty("idea.is.internal", true)
        addSystemProperty("java.awt.headless", true)
      }
    }

    val reportFile: Path = context.paths.testHome.resolve("checkTestReport/validation-report.json")

    // with --local option, the course opens twice, see CourseSource.kt line 6
    context.runIDE(
      commandLine = { _ ->
        IDECommandLine.Args(
          "validateCourse",
          workspaceDir.absolutePathString(),
          "--local", courseLocal,
          "--tests", "true",
          "--links", "false",
          "--output-format", "json",
          "--output", reportFile.absolutePathString(),
        )
      },

      expectedExitCode = 0,
    )

    val actual = Json.decodeFromString<ValidationSuite>(reportFile.readText())
    val expected = Json.decodeFromString<ValidationSuite>(resolveExpectedReport().readText())

    val expectedCases = flattenCases(expected).toMap()
    val actualCases = flattenCases(actual).toMap()

    // Merge display paths from both reports so extra OR missing nodes are both detected.
    val allPaths = (expectedCases.keys + actualCases.keys).sorted()

    return allPaths.map { displayPath ->
      DynamicTest.dynamicTest(displayPath) {
        val expectedCase = expectedCases[displayPath]
        val actualCase = actualCases[displayPath]
        assertNotNull(expectedCase, "Unexpected case present only in actual report at: $displayPath")
        assertNotNull(actualCase, "Expected case missing from actual report at: $displayPath")
        assertEquals(expectedCase.result, actualCase.result, "Case result mismatch at: $displayPath")
      }
    }
  }

  /**
   * We need to flatten json tree and consider every leaf separately because of DynamicTest.
   * Simple way can be used but the output will be just true or false
   * ``` assertEquals(validationResult, json.decodeFromString<ValidationSuite>(validationResultJson))```
   *
   *   suite → { "name": ..., "children": [...] } — a group. The top suite is named root_node.
   *   case → { "name": ..., "result": {...} } — a leaf (an actual test). result is success, ignored, or failed
   */
  private fun flattenCases(node: ValidationResultNode, path: String = ""): List<Pair<String, ValidationCase>> {
    val name = when (node) {
      is ValidationSuite -> node.name
      is ValidationCase -> node.name
    }
    val displayPath = when {
      name == ValidationResultNode.ROOT_NODE_NAME -> path
      path.isEmpty() -> name
      else -> "$path / $name"
    }
    return when (node) {
      is ValidationCase -> listOf(displayPath to node)
      is ValidationSuite -> node.children.flatMap { flattenCases(it, displayPath) }
    }
  }

  private companion object {

    fun resolvePluginPath(): Path {
      val property = System.getProperty("path.to.build.plugin")
      checkNotNull(property) { "System property 'path.to.build.plugin' is not set" }
      val resolved = Paths.get(property)
      check(resolved.exists()) { "Plugin missing at $resolved" }
      return resolved
    }

    fun resolveCourseLocal(): String {
      val resolved = Paths.get(PLAIN_COURSE_LOCAL_PATH)
      check(resolved.exists()) { "Course missing at $resolved" }
      return resolved.absolutePathString()
    }

    fun resolveExpectedReport(): Path {
      val resolved = Paths.get(EXPECTED_REPORT_RELATIVE)
      check(resolved.exists()) { "Expected report is missing at $resolved" }
      return resolved
    }
  }
}
