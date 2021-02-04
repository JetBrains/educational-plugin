package com.jetbrains.edu.python.slow.checker

import com.jetbrains.edu.learning.checker.CheckActionListener
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.python.PythonLanguage
import org.hamcrest.CoreMatchers
import org.junit.Assert

@Suppress("PyInterpreter")
class PyNewTheoryCheckerTest : PyCheckersTestBase() {

  override fun createCourse(): Course {
    return course(language = PythonLanguage.INSTANCE, environment = "unittest") {
      lesson("lesson1") {
        theoryTask("Theory") {
          pythonTaskFile("main.py", """
            if __name__ == "__main__":
                print("Hello!")
          """)
          taskFile("__init__.py")
        }
        theoryTask("TheoryWithCustomRunConfiguration") {
          pythonTaskFile("main.py", """
            import os
            
            if __name__ == "__main__":
                print(os.getenv("EXAMPLE_ENV"))
          """)
          taskFile("__init__.py")
          dir("runConfigurations") {
            taskFile("CustomRun.run.xml", """
              <component name="ProjectRunConfigurationManager">
                <configuration default="false" name="CustomRun" type="PythonConfigurationType" factoryName="Python">
                  <module name="Python Course7" />
                  <option name="INTERPRETER_OPTIONS" value="" />
                  <option name="PARENT_ENVS" value="true" />
                  <envs>
                    <env name="PYTHONUNBUFFERED" value="1" />
                    <env name="EXAMPLE_ENV" value="Hello!" />
                  </envs>
                  <option name="SDK_HOME" value="${'$'}PROJECT_DIR${'$'}/.idea/VirtualEnvironment/bin/python" />
                  <option name="WORKING_DIRECTORY" value="${'$'}PROJECT_DIR${'$'}/lesson1/TheoryWithCustomRunConfiguration" />
                  <option name="IS_MODULE_SDK" value="true" />
                  <option name="ADD_CONTENT_ROOTS" value="true" />
                  <option name="ADD_SOURCE_ROOTS" value="true" />
                  <EXTENSION ID="PythonCoverageRunConfigurationExtension" runner="coverage.py" />
                  <option name="SCRIPT_NAME" value="${'$'}PROJECT_DIR${'$'}/lesson1/TheoryWithCustomRunConfiguration/main.py" />
                  <option name="PARAMETERS" value="" />
                  <option name="SHOW_COMMAND_LINE" value="false" />
                  <option name="EMULATE_TERMINAL" value="false" />
                  <option name="MODULE_MODE" value="false" />
                  <option name="REDIRECT_INPUT" value="false" />
                  <option name="INPUT_FILE" value="" />
                  <method v="2" />
                </configuration>
              </component>             
            """)
          }
        }
      }
    }
  }

  fun `test python course`() {
    CheckActionListener.setCheckResultVerifier { task, checkResult ->
      val (statusMatcher, messageMatcher) = when (task.name) {
        "Theory" -> CoreMatchers.equalTo(CheckStatus.Solved) to CoreMatchers.containsString("Hello!")
        "TheoryWithCustomRunConfiguration" -> CoreMatchers.equalTo(CheckStatus.Solved) to CoreMatchers.containsString("Hello!")
        else -> error("Unexpected task name: ${task.name}")
      }
      Assert.assertThat(checkResult.status, statusMatcher)
      Assert.assertThat(checkResult.message, messageMatcher)
    }
    doTest()
  }
}
