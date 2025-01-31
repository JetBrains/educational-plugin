package com.jetbrains.edu.aiHints.python

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.jetbrains.edu.aiHints.core.EduAIHintsProcessor
import com.jetbrains.edu.aiHints.python.PyHintsTestUtils.PY_LESSON
import com.jetbrains.edu.aiHints.python.PyHintsTestUtils.PY_TASK
import com.jetbrains.edu.aiHints.python.PyHintsTestUtils.PY_TASK_FILE
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.python.PythonLanguage
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@Suppress("Junit4RunWithInspection")
@RunWith(Parameterized::class)
class PyFunctionDiffReducerTest(
  private val functionName: String,
  private val currentCode: String,
  private val codeHint: String,
  private val expectedResult: String,
) : EduTestCase() {
  override fun createCourse() {
    courseWithFiles(language = PythonLanguage.INSTANCE) {
      lesson(PY_LESSON) {
        eduTask(PY_TASK) {
          pythonTaskFile(PY_TASK_FILE, currentCode)
        }
      }
    }
  }

  @Test
  fun `test reducing the difference between function and CodeHint`() { // todo: migrate to extracting psi file from edu task
    val current = PsiFileFactory.getInstance(project).createFileFromText("current.py", currentCode)
    val codeHint = PsiFileFactory.getInstance(project).createFileFromText("codeHint.py", codeHint)

    val functionFromCode = getFunctionPsiWithName(current, functionName)?.copy() ?: error("current null")
    val functionFromCodeHint = getFunctionPsiWithName(codeHint, functionName)?.copy() ?: error("codeHint null")
    val resultPsiElement =
      EduAIHintsProcessor.forCourse(getCourse())?.getFunctionDiffReducer()?.reduceDiffFunctions(functionFromCode, functionFromCodeHint)

    assertEquals(expectedResult, resultPsiElement?.text)
  }

  private fun getFunctionPsiWithName(codePsiFile: PsiFile, functionName: String): PsiElement? {
    return EduAIHintsProcessor.forCourse(getCourse())?.getFunctionSignatureManager()?.getFunctionBySignature(codePsiFile, functionName)
  }

  companion object {
    @JvmStatic
    @Parameterized.Parameters(name = "{0}")
    fun data(): Collection<Array<String>> = listOf( // todo: add return typing to tests
      arrayOf(
        "foo",
        """
           def foo():
             # TODO
        """.trimIndent(),
        """
           def foo():
             csv = pd.read_csv("file.csv")
             return csv['col_name']
        """.trimIndent(),
        """
           def foo():
             csv = pd.read_csv("file.csv")
        """.trimIndent()
      ),

      arrayOf(
        "sum",
        """
          def sum(a, b):
            c = a + b
        """.trimIndent(),
        """
          def sum(a, b):
            c = a + b
            return c
        """.trimIndent(),
        """
          def sum(a, b):
            c = a + b
            return c
        """.trimIndent()
      ),

      arrayOf(
        "sum_typed",
        """
          def sum_typed(a: int, b: int):
            c = a + b
        """.trimIndent(),
        """
          def sum_typed(a: int, b: int):
            c = a + b
            return c
        """.trimIndent(),
        """
          def sum_typed(a: int, b: int):
            c = a + b
            return c
        """.trimIndent()
      ),

      arrayOf(
        "create_series",
        """
          def create_series(dict_: dict):
            # TODO
        """.trimIndent(),
        """
          def create_series(dict_: dict):
            return pd.Series(dict_)
        """.trimIndent(),
        """
          def create_series(dict_: dict):
            return pd.Series(dict_)
        """.trimIndent()
      ),

      arrayOf(
        "correct_inconsistency",
        """
          def correct_inconsistency(df):
            # Correct inconsistencies in the 'Height' column by converting all values to float
        """.trimIndent(),
        """
          def correct_inconsistency(df):
            df_final = df.copy()
            df_final['Height'] = df_final['Height'].astype(float)
            return df_final
        """.trimIndent(),
        """
          def correct_inconsistency(df):
            df_final = df.copy()
        """.trimIndent()
      )
    )
  }
}