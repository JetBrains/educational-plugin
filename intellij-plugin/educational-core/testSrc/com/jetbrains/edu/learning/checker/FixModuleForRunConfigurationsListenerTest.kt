package com.jetbrains.edu.learning.checker

import com.intellij.execution.Executor
import com.intellij.execution.RunManager
import com.intellij.execution.configurations.*
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.icons.AllIcons
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.RootsChangeRescanningInfo
import com.intellij.openapi.roots.ex.ProjectRootManagerEx
import com.intellij.openapi.util.EmptyRunnable
import com.intellij.testFramework.PlatformTestUtil
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.courseGeneration.CourseGenerationTestBase
import com.jetbrains.edu.learning.newproject.EmptyProjectSettings
import org.junit.Test

class FixModuleForRunConfigurationsListenerTest : CourseGenerationTestBase<EmptyProjectSettings>() {

  override val defaultSettings: EmptyProjectSettings = EmptyProjectSettings

  override fun setUp() {
    super.setUp()
    ConfigurationType.CONFIGURATION_TYPE_EP.point.registerExtension(TestModuleBasedConfigurationType, testRootDisposable)
  }

  @Test
  fun `test module is set for added task run configuration in student course`() {
    createCourse(CourseMode.STUDENT)
    val configuration = addTaskRunConfiguration()
    assertEquals(taskModule(), configuration.configurationModule.module)
  }

  @Test
  fun `test module is set for task run configuration after roots change in student course`() {
    createCourse(CourseMode.STUDENT)
    val configuration = addTaskRunConfiguration()
    configuration.setModule(null)

    runWriteAction {
      ProjectRootManagerEx.getInstanceEx(project).makeRootsChange(EmptyRunnable.getInstance(), RootsChangeRescanningInfo.NO_RESCAN_NEEDED)
    }

    val expectedModule = taskModule()
    PlatformTestUtil.waitWithEventsDispatching(
      "Module of task run configuration is not updated after roots change",
      { configuration.configurationModule.module == expectedModule },
      10
    )
  }

  @Test
  fun `test module is not set for added task run configuration in educator course`() {
    createCourse(CourseMode.EDUCATOR)
    val configuration = addTaskRunConfiguration()
    assertNull(configuration.configurationModule.module)
  }

  private fun createCourse(courseMode: CourseMode) {
    val course = course(courseMode = courseMode) {
      lesson("lesson1") {
        eduTask("task1") {
          taskFile("Task.txt")
          taskFile("${EduNames.RUN_CONFIGURATION_DIR}/$RUN_CONFIGURATION_FILE_NAME", """<component name="ProjectRunConfigurationManager"/>""")
        }
      }
    }
    createCourseStructure(course)
  }

  private fun taskDirPath(): String = "${project.courseDir.path}/lesson1/task1"

  private fun taskModule(): Module {
    val taskDir = project.courseDir.findFileByRelativePath("lesson1/task1") ?: error("Failed to find task dir")
    return ModuleUtilCore.findModuleForFile(taskDir, project) ?: error("Failed to find module for task dir")
  }

  private fun addTaskRunConfiguration(): TestModuleBasedConfiguration {
    val runManager = RunManager.getInstance(project)
    val factory = TestModuleBasedConfigurationType.configurationFactories.single()
    val configuration = factory.createTemplateConfiguration(project) as TestModuleBasedConfiguration
    configuration.name = "Test configuration"

    val settings = runManager.createConfiguration(configuration, factory)
    settings.storeInArbitraryFileInProject("${taskDirPath()}/${EduNames.RUN_CONFIGURATION_DIR}/$RUN_CONFIGURATION_FILE_NAME")
    runManager.addConfiguration(settings)
    return configuration
  }

  private object TestModuleBasedConfigurationType : ConfigurationTypeBase(
    "EduTestModuleBasedConfiguration",
    "Edu test module based configuration",
    null,
    AllIcons.RunConfigurations.Application
  ) {
    init {
      addFactory(object : ConfigurationFactory(this) {
        override fun getId(): String = "EduTestModuleBasedConfigurationFactory"
        override fun createTemplateConfiguration(project: Project): RunConfiguration =
          TestModuleBasedConfiguration(project, this)
      })
    }
  }

  private class TestModuleBasedConfiguration(
    project: Project,
    factory: ConfigurationFactory
  ) : ModuleBasedConfiguration<RunConfigurationModule, Any>(RunConfigurationModule(project), factory) {
    override fun getValidModules(): Collection<Module> = allModules
    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> = throw UnsupportedOperationException()
    override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState? = null
  }

  companion object {
    private const val RUN_CONFIGURATION_FILE_NAME = "task.run.xml"
  }
}
