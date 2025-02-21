package com.jetbrains.edu.aiDebugging.kotlin.slice

import com.intellij.openapi.vfs.findPsiFile
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.ext.getVirtualFile
import com.jetbrains.edu.learning.findTask
import org.junit.Before
import org.junit.Test
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.aiDebugging.kotlin.slice.DependencyDirection.FORWARD
import com.jetbrains.edu.aiDebugging.kotlin.slice.DependencyDirection.BACKWARD

class CodeDependencyAnalyzerTest : EduTestCase() {

  private var psiFile: PsiFile? = null

  @Before
  fun initialise() {
    val course = project.course ?: error("Course was not found")
    val task = course.findTask("lesson1", "task1")
    val virtualFile = task.taskFiles.values.first().getVirtualFile(project) ?: error("Can't find virtual file for `${task.name}` task")
    psiFile = virtualFile.findPsiFile(project)
    checkNotNull(psiFile) { "psi File for the virtual file `$virtualFile` is null" }
  }

  @Test
  fun `test forward dependencies`() {
    val function = psiFile?.function("severalFunctionsTest") ?: error("psi function hasn't been found")
    val actual = CodeDependencyAnalyzer().processDependency(function, FORWARD).simplify()
    val expected = mapOf(
      // data dependencies for the first function:
      "var test = true" to setOf("if (test) {", "println(test)"),
      "fun sumTest(): Int {" to setOf("val c = sumTest()"),

      // control dependencies for the first function:
      "if (test) {" to setOf("println(test)", "val c = sumTest()", "val a = 1"),

      // data dependencies for the called function:
      "val n = readln().toInt()" to setOf("for (q in 0..n) {"),
      "q" to setOf("println(q)", "sum += q"),
      "var sum = 0" to setOf("return sum", "sum += q"),
      "sum += q" to setOf("return sum"),

      // control dependencies for the called function:
      "for (q in 0..n) {" to setOf("sum += q", "println(q)"),
    )
    assertEquals(expected, actual)
  }

  @Test
  fun `test backward dependencies`() {
    val function = psiFile?.function("severalFunctionsTest") ?: error("psi function hasn't been found")
    val actual = CodeDependencyAnalyzer().processDependency(function, BACKWARD).simplify()
    val expected = mapOf(
      // for the first function:
      "if (test) {" to setOf("var test = true"), // data dependencies
      "println(test)" to setOf("var test = true", "if (test) {"), // data and control dependencies
      "val c = sumTest()" to setOf("fun sumTest(): Int {", "if (test) {"), // data and control dependencies
      "val a = 1" to setOf("if (test) {"), // control dependencies

      // for the called function:
      "for (q in 0..n) {" to setOf("val n = readln().toInt()"), // data dependencies
      "println(q)" to setOf("q", "for (q in 0..n) {"), // data and control dependencies
      "sum += q" to setOf("q", "var sum = 0", "for (q in 0..n) {"), // data and control dependencies
      "return sum" to setOf("sum += q", "var sum = 0"), // data dependencies

    )
    assertEquals(expected, actual)
  }

  @Test
  fun `forward and backward dependencies are consistent`() {
    val function = psiFile?.function("severalFunctionsTest") ?: error("psi function hasn't been found")
    val analyzer = CodeDependencyAnalyzer()
    val forwardDependency = analyzer.processDependency(function, FORWARD).simplify()
    val backwardDependency = analyzer.processDependency(function, BACKWARD).simplify()
    assertDependenciesConsistency(forwardDependency, backwardDependency, BACKWARD)
    assertDependenciesConsistency(backwardDependency, forwardDependency, FORWARD)
  }

  private fun assertDependenciesConsistency(
    sourceDependency: Map<String, HashSet<String>>,
    targetDependency: Map<String, HashSet<String>>,
    dependencyDirection: DependencyDirection,
  ) {
    sourceDependency.forEach { (key, value) ->
      value.forEach { dependency ->
        assertTrue(
          "Expected '$dependency' to have '$key' in its ${if (dependencyDirection == FORWARD) "forward" else "backward"} dependencies, but it was not found.",
          targetDependency.getOrDefault(dependency, emptySet()).contains(key)
        )
      }
    }
  }

  private fun PsiElementToDependencies.simplify() = mapValues { (_, dependencies) ->
    dependencies.map { it.text.trimToFirstLine() }.toHashSet()
  }.mapKeys { it.key.text.trimToFirstLine() }

  private fun PsiFile.function(name: String) =
    PsiTreeUtil.findChildrenOfType(this, org.jetbrains.kotlin.psi.KtFunction::class.java).find { it.name == name }

  private fun String.trimToFirstLine() = lineSequence().firstOrNull()?.trim() ?: this

  override fun createCourse() {
    StudyTaskManager.Companion.getInstance(project).course = courseWithFiles(courseMode = CourseMode.STUDENT) {
      frameworkLesson("lesson1") {
        eduTask("task1") {
          taskFile(
            name = "Main.kt",
            text = """
    object Task {
      fun severalFunctionsTest() {
        var test = true
        if (test) {
          println(test)
          val c = sumTest()
        } else {
          val a = 1
        }
      }   

      fun sumTest(): Int {
        val n = readln().toInt()
        var sum = 0
        for (q in 0..n) {
            println(q)
            sum += q
        }
        return sum
      }
    }
    """.trimIndent()
          )
        }
      }
    }
  }
}