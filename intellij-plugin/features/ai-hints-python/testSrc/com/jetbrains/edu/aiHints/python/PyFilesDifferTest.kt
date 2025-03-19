package com.jetbrains.edu.aiHints.python

import com.intellij.psi.PsiFileFactory
import com.jetbrains.edu.aiHints.core.EduAIHintsProcessor
import com.jetbrains.edu.aiHints.python.PyHintsTestUtils.PY_LESSON
import com.jetbrains.edu.aiHints.python.PyHintsTestUtils.PY_TASK
import com.jetbrains.edu.aiHints.python.PyHintsTestUtils.PY_TASK_FILE
import com.jetbrains.edu.aiHints.python.PyHintsTestUtils.getPsiFile
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.python.PythonLanguage
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@Suppress("JUnit4RunWithInspection")
@RunWith(Parameterized::class)
@Ignore /** [com.jetbrains.edu.aiHints.core.EduAIHintsProcessor] is not registered for Python yet */
class PyFilesDifferTest(
  private val testCase: PyFilesDifferTestCase
) : EduTestCase() {
  override fun createCourse() {
    courseWithFiles(language = PythonLanguage.INSTANCE) {
      lesson(PY_LESSON) {
        eduTask(PY_TASK) {
          pythonTaskFile(PY_TASK_FILE, testCase.old)
        }
      }
    }
  }

  @Test
  fun `test finding changed functions between two psi files`() {
    val oldPsiFile = getPsiFile(project, PY_LESSON, PY_TASK, PY_TASK_FILE)
    val newPsiFile = PsiFileFactory.getInstance(project).createFileFromText(PY_TASK_FILE, PythonLanguage.INSTANCE, testCase.new)
    val actualResult = EduAIHintsProcessor.forCourse(getCourse())
      ?.getFilesDiffer()
      ?.findChangedMethods(oldPsiFile, newPsiFile, testCase.considerParameters)
    assertEquals(testCase.expectedChangedFunctions, actualResult)
  }

  data class PyFilesDifferTestCase(
    val old: String,
    val new: String,
    val expectedChangedFunctions: List<String>,
    val considerParameters: Boolean = false
  )

  companion object {
    @JvmStatic
    @Parameterized.Parameters(name = "{0}")
    fun data(): Collection<Array<PyFilesDifferTestCase>> = listOf(
      arrayOf(
        PyFilesDifferTestCase(
          old = """
                def foo():
                    return 42
            """.trimIndent(),
          new = """
                def foo():
                    return 43
            """.trimIndent(),
          expectedChangedFunctions = listOf("foo")
        )
      ),

      arrayOf(
        PyFilesDifferTestCase(
          old = """
                def greet(name):
                    return f"Hello {name}"
            """.trimIndent(),
          new = """
                def greet(name, greeting="Hello"):
                    return f"{greeting} {name}"
            """.trimIndent(),
          expectedChangedFunctions = listOf("greet"),
          considerParameters = true
        )
      ),

      arrayOf(
        PyFilesDifferTestCase(
          old = """
                def add(a, b):
                    return a + b

                def multiply(x, y):
                    return x * y
            """.trimIndent(), new = """
                def add(a, b):
                    return a + b

                def multiply(x, y):
                    result = x * y
                    return result
            """.trimIndent(),
          expectedChangedFunctions = listOf("multiply")
        )
      ),

      arrayOf(
        PyFilesDifferTestCase(
          old = """
                @decorator
                def process(data):
                    return data.upper()
            """.trimIndent(),
          new = """
                @decorator
                def process(data):
                    return data.lower()
            """.trimIndent(),
          expectedChangedFunctions = listOf("process")
        )
      ),

      arrayOf(
        PyFilesDifferTestCase(
          old = """
                def unchanged(x):
                    return x * 2
            """.trimIndent(),
          new = """
                def unchanged(x):
                    return x * 2
            """.trimIndent(),
          expectedChangedFunctions = listOf()
        )
      ),

      arrayOf(
        PyFilesDifferTestCase(
          old = """
                def calc(x):
                    return x+1
            """.trimIndent(),
          new = """
                def calc(x):
                    return x + 1
            """.trimIndent(),
          expectedChangedFunctions = listOf("calc")
        )
      ),

      arrayOf(
        PyFilesDifferTestCase(
          old = """
            def foo():
              return 42

            def foo2():
              return 43
        """.trimIndent(),
          new = """
            def foo():
              return 42

            def foo2():
              return 42
        """.trimIndent(),
          expectedChangedFunctions = listOf("foo2"),
          considerParameters = true
        )
      ),

      arrayOf(
        PyFilesDifferTestCase(
          old = """
          class A:
            def foo(x):
              return x + 1
        """.trimIndent(),
          new = """
          class A:
            def foo(x):
              return x + 42
        """.trimIndent(),
          expectedChangedFunctions = listOf("foo")
        )
      ),

      arrayOf(
        PyFilesDifferTestCase(
          old = """
          def change_parameters(x, y):
            return x + y
        """.trimIndent(),
          new = """
          def change_parameters(x: int, y: int):
            return x + y
        """.trimIndent(),
          expectedChangedFunctions = listOf()
        )
      ),

      arrayOf(
        PyFilesDifferTestCase(
          old = """
          def change_parameters(x, y):
            return x + y
        """.trimIndent(),
          new = """
          def change_parameters(x: int, y: int):
            return x + y
        """.trimIndent(),
          expectedChangedFunctions = listOf("change_parameters"),
          considerParameters = true
        )
      ),

      arrayOf(
        PyFilesDifferTestCase(
          old = """
          def change_parameters(x, y):
            return x + y
        """.trimIndent(),
          new = """
          def change_parameters(y, x):
            return x + y
        """.trimIndent(),
          expectedChangedFunctions = listOf("change_parameters"),
          considerParameters = true
        )
      )
    )
  }
}