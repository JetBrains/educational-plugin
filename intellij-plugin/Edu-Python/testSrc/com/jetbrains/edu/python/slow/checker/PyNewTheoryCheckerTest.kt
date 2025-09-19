package com.jetbrains.edu.python.slow.checker

import com.jetbrains.edu.learning.checker.CheckActionListener
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.python.PythonLanguage
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test

@Suppress("PyInterpreter", "PyUnresolvedReferences")
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
            xmlTaskFile("CustomRun.run.xml", $$"""
              <component name="ProjectRunConfigurationManager">
                <configuration default="false" name="CustomRun" type="PythonConfigurationType" factoryName="Python">
                  <module name="Python Course7" />
                  <option name="INTERPRETER_OPTIONS" value="" />
                  <option name="PARENT_ENVS" value="true" />
                  <envs>
                    <env name="PYTHONUNBUFFERED" value="1" />
                    <env name="EXAMPLE_ENV" value="Hello!" />
                  </envs>
                  <option name="SDK_HOME" value="$PROJECT_DIR$/.idea/VirtualEnvironment/bin/python" />
                  <option name="WORKING_DIRECTORY" value="$TASK_DIR$" />
                  <option name="IS_MODULE_SDK" value="true" />
                  <option name="ADD_CONTENT_ROOTS" value="true" />
                  <option name="ADD_SOURCE_ROOTS" value="true" />
                  <EXTENSION ID="PythonCoverageRunConfigurationExtension" runner="coverage.py" />
                  <option name="SCRIPT_NAME" value="$TASK_DIR$/main.py" />
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
      frameworkLesson {
        theoryTask("FrameworkTheoryWithCustomRunConfiguration1") {
          pythonTaskFile("main.py", """
            import os
            
            if __name__ == "__main__":
                print(os.getenv("EXAMPLE_ENV"))
          """)
          taskFile("__init__.py")
          dir("runConfigurations") {
            xmlTaskFile("CustomRun.run.xml", $$"""
              <component name="ProjectRunConfigurationManager">
                <configuration default="false" name="CustomRun1" type="PythonConfigurationType" factoryName="Python">
                  <module name="Python Course" />
                  <option name="INTERPRETER_OPTIONS" value="" />
                  <option name="PARENT_ENVS" value="true" />
                  <envs>
                    <env name="PYTHONUNBUFFERED" value="1" />
                    <env name="EXAMPLE_ENV" value="Hello from FrameworkTheory1!" />
                  </envs>
                  <option name="SDK_HOME" value="$PROJECT_DIR$/.idea/VirtualEnvironment/bin/python" />
                  <option name="WORKING_DIRECTORY" value="$TASK_DIR$" />
                  <option name="IS_MODULE_SDK" value="true" />
                  <option name="ADD_CONTENT_ROOTS" value="true" />
                  <option name="ADD_SOURCE_ROOTS" value="true" />
                  <EXTENSION ID="PythonCoverageRunConfigurationExtension" runner="coverage.py" />
                  <option name="SCRIPT_NAME" value="$TASK_DIR$/main.py" />
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
        theoryTask("FrameworkTheoryWithCustomRunConfiguration2") {
          pythonTaskFile("main.py", """
            import os
            
            if __name__ == "__main__":
                print(os.getenv("EXAMPLE_ENV"))
          """)
          taskFile("__init__.py")
          dir("runConfigurations") {
            xmlTaskFile("CustomRun.run.xml", $$"""
              <component name="ProjectRunConfigurationManager">
                <configuration default="false" name="CustomRun2" type="PythonConfigurationType" factoryName="Python">
                  <module name="Python Course" />
                  <option name="INTERPRETER_OPTIONS" value="" />
                  <option name="PARENT_ENVS" value="true" />
                  <envs>
                    <env name="PYTHONUNBUFFERED" value="1" />
                    <env name="EXAMPLE_ENV" value="Hello from FrameworkTheory2!" />
                  </envs>
                  <option name="SDK_HOME" value="$PROJECT_DIR$/.idea/VirtualEnvironment/bin/python" />
                  <option name="WORKING_DIRECTORY" value="$TASK_DIR$" />
                  <option name="IS_MODULE_SDK" value="true" />
                  <option name="ADD_CONTENT_ROOTS" value="true" />
                  <option name="ADD_SOURCE_ROOTS" value="true" />
                  <EXTENSION ID="PythonCoverageRunConfigurationExtension" runner="coverage.py" />
                  <option name="SCRIPT_NAME" value="$TASK_DIR$/main.py" />
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

  @Test
  fun `test python course`() {
    CheckActionListener.setCheckResultVerifier { task, checkResult ->
      val (statusMatcher, messageMatcher) = when (task.name) {
        "Theory" -> equalTo(CheckStatus.Solved) to containsString("Hello!")
        "TheoryWithCustomRunConfiguration" -> equalTo(CheckStatus.Solved) to containsString("Hello!")
        "FrameworkTheoryWithCustomRunConfiguration1" -> equalTo(CheckStatus.Solved) to containsString("Hello from FrameworkTheory1!")
        "FrameworkTheoryWithCustomRunConfiguration2" -> equalTo(CheckStatus.Solved) to containsString("Hello from FrameworkTheory2!")
        else -> error("Unexpected task name: ${task.name}")
      }
      assertThat(checkResult.status, statusMatcher)
      assertThat(checkResult.message, messageMatcher)
    }
    doTest()
  }
}
