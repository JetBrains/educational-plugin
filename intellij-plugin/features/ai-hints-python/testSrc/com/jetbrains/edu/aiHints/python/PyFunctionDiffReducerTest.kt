package com.jetbrains.edu.aiHints.python

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.jetbrains.edu.aiHints.core.EduAIHintsProcessor
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.python.PythonLanguage
import org.junit.Test

class PyFunctionDiffReducerTest : EduTestCase() {
  private val pyFunctionDiffReducer by lazy {
    EduAIHintsProcessor.forCourse(getCourse())?.getFunctionDiffReducer()
  }

  @Test
  fun `test simple function`() = assertCodeHint(
    functionName = "foo",
    currentCode = """
        def foo():
            # TODO
    """,
    codeHint = """
        def foo():
            csv = pd.read_csv("file.csv")
            return csv['col_name']
    """,
    expectedResult = """
        def foo():
            csv = pd.read_csv("file.csv")
    """
  )

  @Test
  fun `test add return statement sum`() = assertCodeHint(
    functionName = "sum",
    currentCode = """
        def sum(a, b):
            c = a + b
    """,
    codeHint = """
        def sum(a, b):
            c = a + b
            return c
    """,
    expectedResult = """
        def sum(a, b):
            c = a + b
            return c
    """
  )

  @Test
  fun `test add return statement sum_typed`() = assertCodeHint(
    functionName = "sum_typed",
    currentCode = """
        def sum_typed(a: int, b: int):
            c = a + b
    """,
    codeHint = """
        def sum_typed(a: int, b: int):
            c = a + b
            return c
    """,
    expectedResult = """
        def sum_typed(a: int, b: int):
            c = a + b
            return c
    """
  )

  @Test
  fun `test add return statement create_series`() = assertCodeHint(
    functionName = "create_series",
    currentCode = """
        def create_series(dict_: dict):
            # TODO
      """,
    codeHint = """
        def create_series(dict_: dict):
            return pd.Series(dict_)
      """,
    expectedResult = """
        def create_series(dict_: dict):
            return pd.Series(dict_)
    """
  )

  @Test
  fun `test add one statement from a solution`() = assertCodeHint(
    functionName = "correct_inconsistency",
    currentCode = """
        def correct_inconsistency(df):
            # Correct inconsistencies in the 'Height' column by converting all values to float
    """,
    codeHint = """
        def correct_inconsistency(df):
            df_final = df.copy()
            df_final['Height'] = df_final['Height'].astype(float)
            return df_final
    """,
    expectedResult = """
        def correct_inconsistency(df):
            df_final = df.copy()
    """
  )

  @Test
  fun `test add a missing while statement`() = assertCodeHint(
    functionName = "while_part",
    currentCode = """
        def while_part():
            a = 52
            b = 12
            return a + b
    """,
    codeHint = """
        def while_part():
            a = 52
            b = 12
            while b < a:
              print(a)
              b = b + 10
            return a + b
    """,
    expectedResult = """
        def while_part():
            a = 52
            b = 12
            while b < a:
                pass
    """
  )

  @Test
  fun `test empty spaces in the parameter list`() = assertCodeHint(
    functionName = "parameter_list_spaces",
    currentCode = """
        def parameter_list_spaces(a , b):
            return a + b
    """,
    codeHint = """
        def parameter_list_spaces(    a     , b     ):
            return a + b + 42
    """,
    expectedResult = """
        def parameter_list_spaces(a , b):
            return a + b + 42
    """
  )

  @Test
  fun `test complete parameter list`() = assertCodeHint(
    functionName = "param_change",
    currentCode = """
        def param_change(a, b):
            return a + b
    """,
    codeHint = """
        def param_change(a, b, c=0):
            return a + b + c
    """,
    expectedResult = """
        def param_change(a, b, c=0):
            return a + b
    """
  )

  @Test
  fun `test complex parameters`() = assertCodeHint(
    functionName = "complex_params",
    currentCode = """
        def complex_params(a, b=10, *args):
            return sum([a, b] + list(args))
    """,
    codeHint = """
        def complex_params(a, b=10, *args, **kwargs):
            return sum([a, b] + list(args)) + sum(kwargs.values())
    """,
    expectedResult = """
        def complex_params(a, b=10, *args, **kwargs):
            return sum([a, b] + list(args))
    """
  )

  @Test
  fun `test change parameter types`() = assertCodeHint(
    functionName = "param_type_change",
    currentCode = """
        def param_type_change(a: int, b: str):
            return str(a) + b
    """,
    codeHint = """
        def param_type_change(a: float, b: str):
            return str(a) + b
    """,
    expectedResult = """
        def param_type_change(a: float, b: str):
            return str(a) + b
    """
  )

  @Test
  fun `test complete the return type`() = assertCodeHint(
    functionName = "return_type_change",
    currentCode = """
        def return_type_change(a: int, b: int) -> int:
            return a + b
    """,
    codeHint = """
        def return_type_change(a: int, b: int) -> float:
            return a + b
    """,
    expectedResult = """
        def return_type_change(a: int, b: int) -> float:
            return a + b
    """
  )

  @Test
  fun `test empty spaces in the return type`() = assertCodeHint(
    functionName = "foo",
    currentCode = """
        def foo() -> int   :
            # TODO
    """,
    codeHint = """
        def foo()     ->     int:
            csv = pd.read_csv("file.csv")
            return csv['col_name']
    """,
    expectedResult = """
        def foo() -> int   :
            csv = pd.read_csv("file.csv")
    """
  )

  @Test
  fun `test removing the return type annotation`() = assertCodeHint(
    functionName = "remove_return_type",
    currentCode = """
        def remove_return_type(a: int, b: int) -> int:
            return a + b
    """,
    codeHint = """
        def remove_return_type(a: int, b: int):
            return a + b
    """,
    expectedResult = """
        def remove_return_type(a: int, b: int):
            return a + b
    """
  )

  @Test
  fun `test add for loop with pass`() = assertCodeHint(
    functionName = "for_loop",
    currentCode = """
        def for_loop(items):
            result = 0
            return result
    """,
    codeHint = """
        def for_loop(items):
            result = 0
            for item in items:
                result += item
            return result
    """,
    expectedResult = """
        def for_loop(items):
            result = 0
            for item in items:
                pass
    """
  )

  @Test
  fun `test add if with pass`() = assertCodeHint(
    functionName = "if_statement",
    currentCode = """
        def if_statement(value):
            result = 0
            return result
    """,
    codeHint = """
        def if_statement(value):
            result = 0
            if value > 0:
                result = value
            return result
    """,
    expectedResult = """
        def if_statement(value):
            result = 0
            if value > 0:
                pass
    """
  )

  @Test
  fun `test add missing if statement`() = assertCodeHint(
    functionName = "replace_statement_type",
    currentCode = """
        def replace_statement_type(value):
            result = 0
            result += value
            return result
    """,
    codeHint = """
        def replace_statement_type(value):
            result = 0
            if value > 0:
                result = value
            return result
    """,
    expectedResult = """
        def replace_statement_type(value):
            result = 0
            if value > 0:
                pass
            return result
    """
  )

  @Test
  fun `test nested structures`() = assertCodeHint(
    functionName = "nested_structures",
    currentCode = """
        def nested_structures(items):
            result = 0
            return result
    """,
    codeHint = """
        def nested_structures(items):
            result = 0
            for item in items:
                if item > 0:
                    while item > 0:
                        result += 1
                        item -= 1
            return result
    """,
    expectedResult = """
        def nested_structures(items):
            result = 0
            for item in items:
                pass
    """
  )

  @Test
  fun `test function with pass body`() = assertCodeHint(
    functionName = "empty_function",
    currentCode = """
        def empty_function():
            pass
    """,
    codeHint = """
        def empty_function():
            result = 42
            return result
    """,
    expectedResult = """
        def empty_function():
            result = 42
    """
  )

  @Test
  fun `test function with comments`() = assertCodeHint(
    functionName = "comments_only",
    currentCode = """
        def comments_only():
            # This function does nothing
            # It just has comments
    """,
    codeHint = """
        def comments_only():
            # This function does nothing
            # It just has comments
            result = "Hello, World!"
            return result
    """,
    expectedResult = """
        def comments_only():
            result = "Hello, World!"
    """
  )

  @Test
  fun `test function with spaces comments`() = assertCodeHint(
    functionName = "spaces",
    currentCode = """
        def spaces():
            result    =   "Hello, World!"
    """,
    codeHint = """
        def spaces():
            result = "Hello, World!"
            return result
    """,
    expectedResult = """
        def spaces():
            result    =   "Hello, World!"
            return result
    """
  )

  @Test
  fun `test new condition in the if`() = assertCodeHint(
    functionName = "modify_condition_if",
    currentCode = """
        def modify_condition_if(value):
            if value > 0:
                return "positive"
            return "non-positive"
    """,
    codeHint = """
        def modify_condition_if(value):
            if value >= 0:
                return "non-negative"
            return "negative"
    """,
    expectedResult = """
        def modify_condition_if(value):
            if value >= 0:
                return "positive"
            return "non-positive"
    """
  )

  @Test
  fun `test new condition in the while`() = assertCodeHint(
    functionName = "modify_condition_while",
    currentCode = """
        def modify_condition_while(value):
            while value > 0:
              value-=1
              networkCall()
    """,
    codeHint = """
        def modify_condition_while(value):
            while value >= 0:
              value-=1
              return networkCall()
    """,
    expectedResult = """
        def modify_condition_while(value):
            while value >= 0:
              value-=1
              networkCall()
    """
  )

  @Test
  fun `test new condition in the for`() = assertCodeHint(
    functionName = "modify_condition_for",
    currentCode = """
        def modify_condition_for(value):
            for i in range(0, 52):
                pass
    """,
    codeHint = """
        def modify_condition_for(value):
            for i in range(0, 104):
                data = read_data(i)
                return data[i]
    """,
    expectedResult = """
        def modify_condition_for(value):
            for i in range(0, 104):
                pass
    """
  )

  @Test
  fun `test update one line`() = assertCodeHint(
    functionName = "list_comprehension",
    currentCode = """
        def list_comprehension(items):
            result = []
            return result
    """,
    codeHint = """
        def list_comprehension(items):
            result = [x * 2 for x in items if x > 0]
            return result
    """,
    expectedResult = """
        def list_comprehension(items):
            result = [x * 2 for x in items if x > 0]
            return result
    """
  )

  @Test
  fun `test for part with spaces`() = assertCodeHint(
    functionName = "for_part",
    currentCode = """
        def for_part(items):
            result = []
            for i in range(0, 9):
                a  =  i  *  i
            return result
    """,
    codeHint = """
        def for_part(items):
            result = []
            for i in range(0, 9):
                a = i * i
                result.append(a)
            return result
    """,
    expectedResult = """
        def for_part(items):
            result = []
            for i in range(0, 9):
                a  =  i  *  i
                result.append(a)
            return result
    """
  )

  @Test
  fun `test while part with spaces`() = assertCodeHint(
    functionName = "while_part_with_spaces",
    currentCode = """
        def while_part_with_spaces(items):
            result = []
            while i >       10:
                pass
            return result
    """,
    codeHint = """
        def while_part_with_spaces(items):
            result = []
            while i > 10:
                result.append(i)
            return result
    """,
    expectedResult = """
        def while_part_with_spaces(items):
            result = []
            while i >       10:
                result.append(i)
            return result
    """
  )

  @Test
  fun `test while part with spaces 2`() = assertCodeHint(
    functionName = "count_down",
    currentCode = """
        def count_down(start_number):
            current = start_number
            total_sum = 0
  
            while current   >   0:
                print  (f"Counting: {current}")
                total_sum    +=    current
  
            print    ("Countdown complete!")
            return total_sum
    """,
    codeHint = """
        def count_down(start_number):
            current = start_number
            total_sum = 0
  
            while current > 0:
                print(f"Counting: {current}")
                total_sum += current
                current -= 1
  
            print("Countdown complete!")
            return total_sum
    """,
    expectedResult = """
        def count_down(start_number):
            current = start_number
            total_sum = 0
  
            while current   >   0:
                print  (f"Counting: {current}")
                total_sum    +=    current
                current -= 1
  
            print    ("Countdown complete!")
            return total_sum
    """
  )

  @Test
  fun `test for loop with else`() = assertCodeHint(
    functionName = "for_loop_with_else",
    currentCode = """
        def for_loop_with_else(items):
            for i in range(0, 10):
                print(i)
            else:
                print("Irrelevant line")
    """,
    codeHint = """
        def for_loop_with_else(items):
            for i in range(0, 10):
                print(i)
            else:
                print("Done!")
    """,
    expectedResult = """
        def for_loop_with_else(items):
            for i in range(0, 10):
                print(i)
            else:
                print("Done!")
    """,
  )

  @Test
  fun `test adding else part to function with for loop`() = assertCodeHint(
    functionName = "for_loop_with_else",
    currentCode = """
        def for_loop_with_else(items):
            for i in range(0, 10):
                print(i)
    """,
    codeHint = """
        def for_loop_with_else(items):
            for i in range(0, 10):
                print(i)
            else:
                print("Done!")
    """,
    expectedResult = """
        def for_loop_with_else(items):
            for i in range(0, 10):
                print(i)
            else:
                pass
    """,
  )

  @Test
  fun `test while loop with else`() = assertCodeHint(
    functionName = "while_loop_with_else",
    currentCode = """
      def while_loop_with_else(count):
          i = 0
          while i < count:
              print(i)
              i += 1
          else:
              print("Irrelevant line")
  """,
    codeHint = """
      def while_loop_with_else(count):
          i = 0
          while i < count:
              print(i)
              i += 1
          else:
              print("Loop completed normally")
  """,
    expectedResult = """
      def while_loop_with_else(count):
          i = 0
          while i < count:
              print(i)
              i += 1
          else:
              print("Loop completed normally")
  """,
  )

  @Test
  fun `test adding else part to function with while loop`() = assertCodeHint(
    functionName = "while_loop_with_else",
    currentCode = """
      def while_loop_with_else(count):
          i = 0
          while i < count:
              print(i)
              i += 1
  """,
    codeHint = """
      def while_loop_with_else(count):
          i = 0
          while i < count:
              print(i)
              i += 1
          else:
              print("Loop completed normally")
  """,
    expectedResult = """
      def while_loop_with_else(count):
          i = 0
          while i < count:
              print(i)
              i += 1
          else:
              pass
  """,
  )

  @Test
  fun `test if-else structure modification`() = assertCodeHint(
    functionName = "check_number",
    currentCode = """
      def check_number(num):
          if num > 0:
              print("Positive")
          else:
              print("Irrelevant line")
  """,
    codeHint = """
      def check_number(num):
          if num > 0:
              print("Positive")
          else:
              print("Zero")
  """,
    expectedResult = """
      def check_number(num):
          if num > 0:
              print("Positive")
          else:
              print("Zero")
  """,
  )

  @Test
  fun `test adding else part to function with if statement`() = assertCodeHint(
    functionName = "check_number",
    currentCode = """
      def check_number(num):
          if num > 0:
              print("Positive")
  """,
    codeHint = """
      def check_number(num):
          if num > 0:
              print("Positive")
          else:
              print("Zero")
  """,
    expectedResult = """
      def check_number(num):
          if num > 0:
              print("Positive")
          else:
              pass
  """,
  )

  @Test
  fun `test if-elif structure modification`() = assertCodeHint(
    functionName = "check_number",
    currentCode = """
      def check_number(num):
          if num > 0:
              print("Positive")
  """,
    codeHint = """
      def check_number(num):
          if num > 0:
              print("Positive")
          elif num < 0:
              print("Negative")
  """,
    expectedResult = """
      def check_number(num):
          if num > 0:
              print("Positive")
          elif num < 0:
              pass
  """,
  )

  @Test
  fun `test adding elif part to function with if statement`() = assertCodeHint(
    functionName = "check_number",
    currentCode = """
      def check_number(num):
          if num > 0:
              print("Positive")
          elif num < 0:
              print("something else")
  """,
    codeHint = """
      def check_number(num):
          if num > 0:
              print("Positive")
          elif num < 0:
              print("Negative")
  """,
    expectedResult = """
      def check_number(num):
          if num > 0:
              print("Positive")
          elif num < 0:
              print("Negative")
  """,
  )

  @Test
  fun `test adding elif part to function with another elif statement`() = assertCodeHint(
    functionName = "check_number",
    currentCode = """
      def check_number(num):
          if num > 0:
              print("Positive")
          elif num < 0:
              print("Negative")
  """,
    codeHint = """
      def check_number(num):
          if num > 0:
              print("Positive")
          elif num < 0:
              print("Negative")
          elif num == 0:
              print("Zero")
  """,
    expectedResult = """
      def check_number(num):
          if num > 0:
              print("Positive")
          elif num < 0:
              print("Negative")
          elif num == 0:
              pass
  """,
  )

  @Test
  fun `test removing elif parts when there are no in the CodeHint`() = assertCodeHint(
    functionName = "check_number",
    currentCode = """
      def check_number(num):
          if num > 0:
              print("Positive")
          elif num < 0:
              print("Negative")
          elif num == 0:
              print("Zero")
  """,
    codeHint = """
      def check_number(num):
          if num > 0:
              print("Positive")
          return num
  """,
    expectedResult = """
      def check_number(num):
          if num > 0:
              print("Positive")
  """,
  )

  @Test
  fun `test if-elif content modification in elif parts`() = assertCodeHint(
    functionName = "check_number",
    currentCode = """
      def check_number(num):
          if num > 0:
              print("Positive")
          elif num < 0:
              print("Nogative")
          elif num == 0:
              print("WA")
  """,
    codeHint = """
      def check_number(num):
          if num > 0:
              print("Positive")
          elif num < 0:
              print("Negative")
          elif num == 0:
              print("Zero")
  """,
    expectedResult = """
      def check_number(num):
          if num > 0:
              print("Positive")
          elif num < 0:
              print("Negative")
          elif num == 0:
              print("WA")
  """,
  )

  // TODO: Tests for the case when there is a new function only

  private fun assertCodeHint(
    functionName: String,
    currentCode: String,
    codeHint: String,
    expectedResult: String
  ) {
    // when
    courseWithFiles(language = PythonLanguage.INSTANCE) {
      lesson(PY_LESSON) {
        eduTask(PY_TASK) {
          pythonTaskFile(PY_TASK_FILE, currentCode.trimIndent())
        }
      }
    }
    val current = getPsiFile(project, PY_LESSON, PY_TASK, PY_TASK_FILE)
    val codeHint = PsiFileFactory.getInstance(project).createFileFromText("codeHint.py", PythonLanguage.INSTANCE, codeHint.trimIndent())
    val functionFromCode = getFunctionPsiWithName(current, functionName) ?: error("Current PSI File is null")
    val functionFromCodeHint = getFunctionPsiWithName(codeHint, functionName) ?: error("PSI File for CodeHint is null")

    // then
    val resultPsiElement = pyFunctionDiffReducer?.reduceDiffFunctions(functionFromCode, functionFromCodeHint)

    // verify
    assertEquals(expectedResult.trimIndent(), resultPsiElement?.text)
  }

  private fun getFunctionPsiWithName(codePsiFile: PsiFile, functionName: String): PsiElement? {
    return EduAIHintsProcessor.forCourse(getCourse())?.getFunctionSignatureManager()?.getFunctionBySignature(codePsiFile, functionName)
  }
}