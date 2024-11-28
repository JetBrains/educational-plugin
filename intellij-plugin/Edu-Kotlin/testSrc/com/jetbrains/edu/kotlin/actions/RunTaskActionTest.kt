package com.jetbrains.edu.kotlin.actions

import com.intellij.execution.RunManager
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationTypeUtil
import com.jetbrains.edu.learning.EduActionTestCase
import com.jetbrains.edu.learning.actions.RunTaskAction
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.navigation.NavigationUtils.navigateToTask
import com.jetbrains.edu.learning.testAction
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.idea.run.KotlinRunConfiguration
import org.jetbrains.kotlin.idea.run.KotlinRunConfigurationType
import org.junit.Test

class RunTaskActionTest : EduActionTestCase() {

  @Test
  fun `test run action`() {
    courseWithFiles(language = KotlinLanguage.INSTANCE, courseMode = CourseMode.EDUCATOR) {
      lesson("lesson1") {
        eduTask("task1") {
          taskFile("src/Task.kt", "")
          xmlTaskFile("runConfigurations/runner.run.xml", """
            <component name="ProjectRunConfigurationManager">
              <configuration default="false" name="Task1 from Lesson1" type="JetRunConfigurationType" activateToolWindowBeforeRun="false">
                <option name="MAIN_CLASS_NAME" value="TaskKt" />
                <module name="light_idea_test_case.lesson1-task1.main" />
                <shortenClasspath name="NONE" />
                <method v="2">
                  <option name="Make" enabled="true" />
                </method>
              </configuration>
            </component>
          """.trimIndent())
        }
        eduTask("task2") {
          taskFile("src/Task.kt", "")
          xmlTaskFile("runConfigurations/some-other-name.run.xml", """
            <component name="ProjectRunConfigurationManager">
              <configuration default="false" name="Task1 from Lesson1" type="JetRunConfigurationType" activateToolWindowBeforeRun="false">
                <option name="MAIN_CLASS_NAME" value="TaskKt" />
                <module name="light_idea_test_case.lesson1-task2.main" />
                <shortenClasspath name="NONE" />
                <method v="2">
                  <option name="Make" enabled="true" />
                </method>
              </configuration>
            </component>
          """.trimIndent())
        }
        theoryTask("task3") {
          taskFile("src/Task.kt", "")
          xmlTaskFile("runConfigurations/runner.run.xml", """
            <component name="ProjectRunConfigurationManager">
              <configuration default="false" name="Task3 from lesson1" type="JetRunConfigurationType">
                <option name="MAIN_CLASS_NAME" value="MainKt" />
                <module name="light_idea_test_case.lesson1-task3.main" />
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

    createConfiguration("lesson1/task1", "runner.run.xml")
    createConfiguration("lesson1/task2", "another-name.run.xml")
    createConfiguration("lesson1/task3", "runner.run.xml")

    navigateToTask(project, findTask(0, 0))
    testAction(RunTaskAction.ACTION_ID, shouldBeEnabled = true, runAction = false)

    navigateToTask(project, findTask(0, 1))
    testAction(RunTaskAction.ACTION_ID, shouldBeEnabled = false, runAction = false)

    navigateToTask(project, findTask(0, 2))
    testAction(RunTaskAction.ACTION_ID, shouldBeEnabled = true, runAction = false)
  }

  private fun createConfiguration(taskFolder: String, configName: String) {
    val runManager = RunManager.getInstance(project)
    val configurationType = ConfigurationTypeUtil.findConfigurationType(KotlinRunConfigurationType::class.java)
    val factory: ConfigurationFactory = configurationType.configurationFactories[0]
    val runConfiguration = factory.createTemplateConfiguration(project) as KotlinRunConfiguration

    runConfiguration.name = "Run MainKt for $taskFolder"
    runConfiguration.setModule(module)

    val settings = runManager.createConfiguration(runConfiguration, factory)
    settings.storeInArbitraryFileInProject(project.courseDir.path + "/$taskFolder/runConfigurations/$configName")
    runManager.addConfiguration(settings)
  }
}