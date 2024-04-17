package com.jetbrains.edu.python.slow

import com.jetbrains.edu.learning.codeforces.CodeforcesNames
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.codeforces.CodeforcesCourse
import com.jetbrains.edu.python.slow.checker.PyCheckersTestBase
import com.jetbrains.python.PythonLanguage
import org.junit.Test

@Suppress("PyInterpreter", "PyUnresolvedReferences")
class PyCodeforcesRunConfigurationTest : PyCheckersTestBase() {
  @Test
  fun `test application configuration`() {
    doCodeforcesTest("123456")
  }

  override fun createCourse(): Course = course(language = PythonLanguage.INSTANCE, courseProducer = ::CodeforcesCourse) {
    lesson(CodeforcesNames.CODEFORCES_PROBLEMS) {
      codeforcesTask {
        pythonTaskFile("src/main.py", """
          if __name__ == "__main__":
            a = input()
            print(a)
        """)
        taskFile("${CodeforcesNames.TEST_DATA_FOLDER}/1/input.txt", "123456")
        taskFile("${CodeforcesNames.TEST_DATA_FOLDER}/1/output.txt", "4")
      }
    }
  }
}
