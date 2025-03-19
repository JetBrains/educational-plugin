package com.jetbrains.edu.aiHints.python

import com.jetbrains.edu.aiHints.core.EduAIHintsProcessor
import com.jetbrains.edu.aiHints.core.context.FunctionSignature
import com.jetbrains.edu.aiHints.core.context.FunctionsToStrings
import com.jetbrains.edu.aiHints.python.PyHintsTestUtils.PY_LESSON
import com.jetbrains.edu.aiHints.python.PyHintsTestUtils.PY_TASK
import com.jetbrains.edu.aiHints.python.PyHintsTestUtils.PY_TASK_FILE
import com.jetbrains.edu.aiHints.python.PyHintsTestUtils.getPsiFile
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.python.PythonLanguage
import org.junit.Ignore
import org.junit.Test

@Ignore /** [com.jetbrains.edu.aiHints.core.EduAIHintsProcessor] is not registered for Python yet */
class PyStringExtractorTest : EduTestCase() {
  override fun createCourse() {
    courseWithFiles(language = PythonLanguage.INSTANCE) {
      lesson(PY_LESSON) {
        eduTask(PY_TASK) {
          @Suppress("PyInterpreter")
          pythonTaskFile(PY_TASK_FILE, text = """
              a = "referenced"
              # noinspection PyUnresolvedReferences
              def foo():
                  print("double")
                  print('single')
                  print(f"formatted {1 + 3}")
                  print(a)
                
              def single_fun():
                return 'single_fun'
                
              def double_fun():
                return "double_fun"
                
              def formatted_fun():
                return f"formatted {a}"
            """.trimIndent()
          )
        }
      }
    }
  }

  @Test
  fun `test getting map of function signatures to strings`() {
    val psiFile = getPsiFile(project, PY_LESSON, PY_TASK, PY_TASK_FILE)
    val expectedResult = mapOf(
      FunctionSignature(
        "foo",
        listOf(),
        null,
        null,
        null
      ) to listOf("referenced", "double", "single", "formatted {1 + 3}"),
      FunctionSignature(
        "single_fun",
        listOf(),
        null,
        null,
        null
      ) to listOf("single_fun"),
      FunctionSignature(
        "double_fun",
        listOf(),
        null,
        null,
        null
      ) to listOf("double_fun"),
      FunctionSignature(
        "formatted_fun",
        listOf(),
        null,
        null,
        null
      ) to listOf("referenced", "formatted {a}")
    ).let(::FunctionsToStrings)

    assertEquals(expectedResult, EduAIHintsProcessor.forCourse(getCourse())?.getStringsExtractor()?.getFunctionsToStringsMap(psiFile))
  }
}