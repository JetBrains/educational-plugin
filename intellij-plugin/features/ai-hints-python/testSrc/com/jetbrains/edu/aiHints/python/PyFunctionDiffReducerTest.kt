package com.jetbrains.edu.aiHints.python

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.jetbrains.edu.aiHints.core.EduAIHintsProcessor
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
  fun `test reducing the difference between function and CodeHint`() {
    val current = getPsiFile(project, PY_LESSON, PY_TASK, PY_TASK_FILE)
    val codeHint = PsiFileFactory.getInstance(project).createFileFromText("codeHint.py", PythonLanguage.INSTANCE, codeHint)

    val functionFromCode = getFunctionPsiWithName(current, functionName) ?: error("Current PSI File is null")
    val functionFromCodeHint = getFunctionPsiWithName(codeHint, functionName) ?: error("PSI File for CodeHint is null")
    val resultPsiElement =
      EduAIHintsProcessor.forCourse(getCourse())?.getFunctionDiffReducer()?.reduceDiffFunctions(functionFromCode, functionFromCodeHint)

    assertEquals(expectedResult, resultPsiElement?.text)
  }

  // TODO: Tests for the case when there is a new function only

  private fun getFunctionPsiWithName(codePsiFile: PsiFile, functionName: String): PsiElement? {
    return EduAIHintsProcessor.forCourse(getCourse())?.getFunctionSignatureManager()?.getFunctionBySignature(codePsiFile, functionName)
  }

  companion object {
    @JvmStatic
    @Parameterized.Parameters(name = "{0}")
    fun data(): Collection<Array<String>> = listOf(
      // Simple function
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

      // Add a return statement
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

      // Add a return statement
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

      // Add a return statement
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

      // Add one statement from a solution
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
      ),

      // Add a missing while statement
      arrayOf(
        "while_part",
        """
          def while_part():
              a = 52
              b = 12
              return a + b
        """.trimIndent(),
        """
          def while_part():
              a = 52
              b = 12
              while b < a:
                print(a)
                b = b + 10
              return a + b
        """.trimIndent(),
        """
          def while_part():
              a = 52
              b = 12
              while b < a:
                  pass
        """.trimIndent()
      ),

      // Empty spaces in the parameter list
      arrayOf(
        "parameter_list_spaces",
        """
          def parameter_list_spaces(a , b):
              return a + b
        """.trimIndent(),
        """
          def parameter_list_spaces(    a     , b     ):
              return a + b + 42
        """.trimIndent(),
        """
          def parameter_list_spaces(a , b):
              return a + b + 42
        """.trimIndent()
      ),

      // Complete parameter list
      arrayOf(
        "param_change",
        """
          def param_change(a, b):
              return a + b
        """.trimIndent(),
        """
          def param_change(a, b, c=0):
              return a + b + c
        """.trimIndent(),
        """
          def param_change(a, b, c=0):
              return a + b
        """.trimIndent()
      ),

      // Complex parameters
      arrayOf(
        "complex_params",
        """
          def complex_params(a, b=10, *args):
              return sum([a, b] + list(args))
        """.trimIndent(),
        """
          def complex_params(a, b=10, *args, **kwargs):
              return sum([a, b] + list(args)) + sum(kwargs.values())
        """.trimIndent(),
        """
          def complex_params(a, b=10, *args, **kwargs):
              return sum([a, b] + list(args))
        """.trimIndent()
      ),

      // Change parameter types
      arrayOf(
        "param_type_change",
        """
          def param_type_change(a: int, b: str):
              return str(a) + b
        """.trimIndent(),
        """
          def param_type_change(a: float, b: str):
              return str(a) + b
        """.trimIndent(),
        """
          def param_type_change(a: float, b: str):
              return str(a) + b
        """.trimIndent()
      ),

      // Complete the return type
      arrayOf(
        "return_type_change",
        """
          def return_type_change(a: int, b: int) -> int:
              return a + b
        """.trimIndent(),
        """
          def return_type_change(a: int, b: int) -> float:
              return a + b
        """.trimIndent(),
        """
          def return_type_change(a: int, b: int) -> float:
              return a + b
        """.trimIndent()
      ),

      // Empty spaces in the return type
      arrayOf(
        "foo",
        """
          def foo() -> int   :
              # TODO
        """.trimIndent(),
        """
          def foo()     ->     int:
              csv = pd.read_csv("file.csv")
              return csv['col_name']
        """.trimIndent(),
        """
          def foo() -> int   :
              csv = pd.read_csv("file.csv")
        """.trimIndent()
      ),

      // Removing the return type annotation
      arrayOf(
        "remove_return_type",
        """
          def remove_return_type(a: int, b: int) -> int:
              return a + b
        """.trimIndent(),
        """
          def remove_return_type(a: int, b: int):
              return a + b
        """.trimIndent(),
        """
          def remove_return_type(a: int, b: int):
              return a + b
        """.trimIndent()
      ),

      // Add `for` loop with `pass`
      arrayOf(
        "for_loop",
        """
          def for_loop(items):
              result = 0
              return result
        """.trimIndent(),
        """
          def for_loop(items):
              result = 0
              for item in items:
                  result += item
              return result
        """.trimIndent(),
        """
          def for_loop(items):
              result = 0
              for item in items:
                  pass
        """.trimIndent()
      ),

      // Add `if` with `pass`
      arrayOf(
        "if_statement",
        """
          def if_statement(value):
              result = 0
              return result
        """.trimIndent(),
        """
          def if_statement(value):
              result = 0
              if value > 0:
                  result = value
              return result
        """.trimIndent(),
        """
          def if_statement(value):
              result = 0
              if value > 0:
                  pass
        """.trimIndent()
      ),

      // Add missing `if` statement
      arrayOf(
        "replace_statement_type",
        """
          def replace_statement_type(value):
              result = 0
              result += value
              return result
        """.trimIndent(),
        """
          def replace_statement_type(value):
              result = 0
              if value > 0:
                  result = value
              return result
        """.trimIndent(),
        """
          def replace_statement_type(value):
              result = 0
              if value > 0:
                  pass
              return result
        """.trimIndent()
      ),

      // Nested
      arrayOf(
        "nested_structures",
        """
          def nested_structures(items):
              result = 0
              return result
        """.trimIndent(),
        """
          def nested_structures(items):
              result = 0
              for item in items:
                  if item > 0:
                      while item > 0:
                          result += 1
                          item -= 1
              return result
        """.trimIndent(),
        """
          def nested_structures(items):
              result = 0
              for item in items:
                  pass
        """.trimIndent()
      ),

      // Function with `pass` body
      arrayOf(
        "empty_function",
        """
          def empty_function():
              pass
        """.trimIndent(),
        """
          def empty_function():
              result = 42
              return result
        """.trimIndent(),
        """
          def empty_function():
              result = 42
        """.trimIndent()
      ),

      // Function with comments
      arrayOf(
        "comments_only",
        """
          def comments_only():
              # This function does nothing
              # It just has comments
        """.trimIndent(),
        """
          def comments_only():
              # This function does nothing
              # It just has comments
              result = "Hello, World!"
              return result
        """.trimIndent(),
        """
          def comments_only():
              result = "Hello, World!"
        """.trimIndent() // When extracting [PyFunction] from the given file, it's returned without [PsiComment]s.
      ),

      // Function with spaces comments
      arrayOf(
        "spaces",
        """
          def spaces():
              result    =   "Hello, World!"
        """.trimIndent(),
        """
          def spaces():
              result = "Hello, World!"
              return result
        """.trimIndent(),
        """
          def spaces():
              result    =   "Hello, World!"
              return result
        """.trimIndent()
      ),

      // New condition in the `if`
      arrayOf(
        "modify_condition_if",
        """
          def modify_condition_if(value):
              if value > 0:
                  return "positive"
              return "non-positive"
        """.trimIndent(),
        """
          def modify_condition_if(value):
              if value >= 0:
                  return "non-negative"
              return "negative"
        """.trimIndent(),
        """
          def modify_condition_if(value):
              if value >= 0:
                  return "positive"
              return "non-positive"
        """.trimIndent()
      ),

      // New condition in the `while`
      arrayOf(
        "modify_condition_while",
        """
          def modify_condition_while(value):
              while value > 0:
                value-=1
                networkCall()
        """.trimIndent(),
        """
          def modify_condition_while(value):
              while value >= 0:
                value-=1
                return networkCall()
        """.trimIndent(),
        """
          def modify_condition_while(value):
              while value >= 0:
                value-=1
                networkCall()
        """.trimIndent()
      ),

      // New condition in the `for`
      arrayOf(
        "modify_condition_for",
        """
          def modify_condition_for(value):
              for i in range(0, 52):
                  pass
        """.trimIndent(),
        """
          def modify_condition_for(value):
              for i in range(0, 104):
                  data = read_data(i)
                  return data[i]
        """.trimIndent(),
        """
          def modify_condition_for(value):
              for i in range(0, 104):
                  pass
        """.trimIndent()
      ),

      // Update one line
      arrayOf(
        "list_comprehension",
        """
          def list_comprehension(items):
              result = []
              return result
        """.trimIndent(),
        """
          def list_comprehension(items):
              result = [x * 2 for x in items if x > 0]
              return result
        """.trimIndent(),
        """
          def list_comprehension(items):
              result = [x * 2 for x in items if x > 0]
              return result
        """.trimIndent()
      ),

      // For part with spaces
      arrayOf(
        "for_part",
        """
          def for_part(items):
              result = []
              for i in range(0, 9):
                  a  =  i  *  i
              return result
        """.trimIndent(),
        """
          def for_part(items):
              result = []
              for i in range(0, 9):
                  a = i * i
                  result.append(a)
              return result
        """.trimIndent(),
        """
          def for_part(items):
              result = []
              for i in range(0, 9):
                  a  =  i  *  i
                  result.append(a)
              return result
        """.trimIndent()
      ),

      // For part with spaces
      arrayOf(
        "for_part",
        """
          def for_part(items):
              result = []
              for i in range(0, 9):
                  a  =  i  *  i
              return result
        """.trimIndent(),
        """
          def for_part(items):
              result = []
              for i in range(0, 9):
                  a = i * i
                  result.append(a)
              return result
        """.trimIndent(),
        """
          def for_part(items):
              result = []
              for i in range(0, 9):
                  a  =  i  *  i
                  result.append(a)
              return result
        """.trimIndent()
      ),

      // While part with spaces
      arrayOf(
        "while_part_with_spaces",
        """
          def while_part_with_spaces(items):
              result = []
              while i >       10:
                  pass
              return result
        """.trimIndent(),
        """
          def while_part_with_spaces(items):
              result = []
              while i > 10:
                  result.append(i)
              return result
        """.trimIndent(),
        """
          def while_part_with_spaces(items):
              result = []
              while i >       10:
                  result.append(i)
              return result
        """.trimIndent()
      ),

      // While part with spaces 2
      arrayOf(
        "count_down",
        """
          def count_down(start_number):
              current = start_number
              total_sum = 0

              while current   >   0:
                  print  (f"Counting: {current}")
                  total_sum    +=    current

              print    ("Countdown complete!")
              return total_sum
        """.trimIndent(),
        """
          def count_down(start_number):
              current = start_number
              total_sum = 0

              while current > 0:
                  print(f"Counting: {current}")
                  total_sum += current
                  current -= 1

              print("Countdown complete!")
              return total_sum
        """.trimIndent(),
        """
          def count_down(start_number):
              current = start_number
              total_sum = 0

              while current   >   0:
                  print  (f"Counting: {current}")
                  total_sum    +=    current
                  current -= 1

              print    ("Countdown complete!")
              return total_sum
        """.trimIndent()
      ),
    )
  }
}