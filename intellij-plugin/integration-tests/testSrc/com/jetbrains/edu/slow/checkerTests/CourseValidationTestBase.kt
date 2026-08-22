package com.jetbrains.edu.slow.checkerTests

import com.intellij.ide.starter.junit5.hyphenateWithClass
import com.intellij.ide.starter.models.IdeInfo
import com.intellij.ide.starter.models.TestCase
import com.intellij.ide.starter.plugins.PluginConfigurator
import com.intellij.ide.starter.project.NoProject
import com.intellij.ide.starter.runner.IDECommandLine
import com.intellij.ide.starter.runner.Starter
import com.intellij.tools.ide.performanceTesting.commands.CommandChain
import com.jetbrains.edu.coursecreator.validation.ValidationCase
import com.jetbrains.edu.coursecreator.validation.ValidationResultNode
import com.jetbrains.edu.coursecreator.validation.ValidationSuite
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DynamicNode
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestInfo
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.absolutePathString
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.test.assertNotNull

/**
 * Base class for tests validating a course with the `validateCourse` command in a real IDE process.
 *
 * Test data location is derived from [testDataPrefix] and the name of the test method itself:
 * a test method `correct plain text course` in a class with `"plain"` prefix
 * expects the following layout:
 * ```
 * testData/plain/correct plain text course/course/...  - course to validate
 * testData/plain/correct plain text course/expected.json - expected validation results
 * ```
 * so a single test class may contain any number of test cases without configuring paths.
 */
abstract class CourseValidationTestBase(private val testDataPrefix: String, private val ideInfo: IdeInfo) {

  private lateinit var testInfo: TestInfo

  protected open fun commandChain(): CommandChain = CommandChain()

  protected open fun installPlugin(configurator: PluginConfigurator, pluginPath: Path) {
    configurator.installPluginFromPath(pluginPath)
  }

  @BeforeEach
  fun setUpTestInfo(testInfo: TestInfo) {
    this.testInfo = testInfo
  }

  protected fun doTest(): List<DynamicNode> {
    val pluginPath: Path = resolvePluginPath()
    val courseLocal = resolveCourse()
    val workspaceDir = Files.createTempDirectory("validateCourse-workspace")

    val context = Starter.newContext(
      testInfo.hyphenateWithClass(),
      TestCase(ideInfo, NoProject)
    ).apply {
      installPlugin(PluginConfigurator(this), pluginPath)
      applyVMOptionsPatch {
        addSystemProperty("idea.is.internal", true)
        addSystemProperty("java.awt.headless", true)
        addSystemProperty("edu.disable.user.agreement", true)
      }
    }

    val reportFile = context.paths.testHome.resolve("checkTestReport/validation-report.json")

    // with --local option, the course opens twice, see CourseSource.kt line 6
    context.runIDE(
      commandLine = { _ ->
        IDECommandLine.Args(
          "validateCourse",
          workspaceDir.absolutePathString(),
          "--local", courseLocal.absolutePathString(),
          "--tests", "true",
          "--links", "false",
          "--output-format", "json",
          "--output", reportFile.absolutePathString(),
        )
      },
      commands = commandChain(),
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

  // We need to flatten json tree and consider every leaf separately because of DynamicTest. Simple way can
  // be used but the output will be just true or false
  // assertEquals(validationResult, json.decodeFromString<ValidationSuite>(validationResultJson))
  //
  // suite → { "name": ..., "children": [...] } — a group.
  // The top suite is named root_node. case → { "name": ..., "result": {...} } — a leaf (an actual test).
  // result is success, ignored, or failed
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

  private fun testCaseDataDir(): Path {
    val testMethodName = testInfo.testMethod.orElseThrow {
      IllegalStateException("`doTest` is supposed to be called from a test method")
    }.name
    return Paths.get(TEST_DATA_ROOT, testDataPrefix, testMethodName)
  }

  private fun resolveCourse(): Path {
    val resolved = testCaseDataDir().resolve(COURSE_DIR_NAME)
    check(resolved.exists()) { "Course missing at $resolved" }
    return resolved
  }

  private fun resolveExpectedReport(): Path {
    val resolved = testCaseDataDir().resolve(EXPECTED_REPORT_NAME)
    check(resolved.exists()) { "Expected report is missing at $resolved" }
    return resolved
  }

  private companion object {

    private const val TEST_DATA_ROOT = "testData"
    private const val COURSE_DIR_NAME = "course"
    private const val EXPECTED_REPORT_NAME = "expected.json"

    fun resolvePluginPath(): Path {
      val property = System.getProperty("path.to.build.plugin")
      checkNotNull(property) { "System property 'path.to.build.plugin' is not set" }
      val resolved = Paths.get(property)
      check(resolved.exists()) { "Plugin missing at $resolved" }
      return resolved
    }
  }
}
