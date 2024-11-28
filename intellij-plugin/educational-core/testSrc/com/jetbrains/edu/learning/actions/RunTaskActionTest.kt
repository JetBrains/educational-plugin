package com.jetbrains.edu.learning.actions

import com.intellij.testFramework.PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue
import com.jetbrains.edu.learning.EduActionTestCase
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.navigation.NavigationUtils.navigateToTask
import com.jetbrains.edu.learning.testAction
import org.junit.Test

class RunTaskActionTest : EduActionTestCase() {

  @Test
  fun `test run action`() {
    courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson("lesson1") {
        eduTask("task1") {
          taskFile("runConfigurations/runner.run.xml", """
            <component name="ProjectRunConfigurationManager">
              <configuration default="false" name="Task1 from Lesson1" type="JetRunConfigurationType" activateToolWindowBeforeRun="false">
                <option name="MAIN_CLASS_NAME" value="TaskKt" />
                <module name="Kotlin_Course75.lesson1-task1.main" />
                <shortenClasspath name="NONE" />
                <method v="2">
                  <option name="Make" enabled="true" />
                </method>
              </configuration>
            </component>
          """.trimIndent())
        }
        eduTask("task2") {
          taskFile("runConfigurations/some-other-name.run.xml", """
            <component name="ProjectRunConfigurationManager">
              <configuration default="false" name="Task1 from Lesson1" type="JetRunConfigurationType" activateToolWindowBeforeRun="false">
                <option name="MAIN_CLASS_NAME" value="TaskKt" />
                <module name="Kotlin_Course75.lesson1-task1.main" />
                <shortenClasspath name="NONE" />
                <method v="2">
                  <option name="Make" enabled="true" />
                </method>
              </configuration>
            </component>
          """.trimIndent())
        }
        theoryTask("task3") {
          taskFile("runConfigurations/runner.run.xml", """
            <component name="ProjectRunConfigurationManager">
              <configuration default="false" name="Task3 from lesson1" type="JetRunConfigurationType">
                <option name="MAIN_CLASS_NAME" value="MainKt" />
                <module name="Kotlin_Course75.lesson1-task3.main" />
                <shortenClasspath name="NONE" />
                <method v="2">
                  <option name="Make" enabled="true" />
                </method>
              </configuration>
            </component>
          """.trimIndent())
        }
      }
    }

    dispatchAllInvocationEventsInIdeEventQueue()

    navigateToTask(project, findTask(0, 0))
    testAction(RunTaskAction.ACTION_ID, shouldBeEnabled = true)

    navigateToTask(project, findTask(0, 1))
    testAction(RunTaskAction.ACTION_ID, shouldBeEnabled = false)

    navigateToTask(project, findTask(0, 2))
    testAction(RunTaskAction.ACTION_ID, shouldBeEnabled = true)
  }

}