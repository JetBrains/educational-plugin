package com.jetbrains.edu.python.environment

import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.application.writeAction
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.impl.SdkConfigurationUtil
import com.intellij.openapi.roots.ModuleRootModificationUtil.setModuleSdk
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.util.Disposer
import com.jetbrains.edu.learning.EduNames.UNITTEST
import com.jetbrains.edu.learning.checker.EnvironmentChecker
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.courseFormat.EduFormatNames
import com.jetbrains.edu.learning.courseFormat.ext.allTasks
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.courseFormat.ext.getDir
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseGeneration.CourseGenerationTestBase
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.newproject.environment.InstallationResult
import com.jetbrains.edu.learning.newproject.environment.LanguageEnvironment
import com.jetbrains.edu.python.learning.newproject.PySdkToCreateVirtualEnv
import com.jetbrains.python.PythonLanguage
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test

class PythonEnvironmentCheckerTest : CourseGenerationTestBase<LanguageEnvironment>() {

  override val defaultSettings: LanguageEnvironment = object : LanguageEnvironment {
    override suspend fun installIfNeeded(project: Project, course: Course): InstallationResult {
      // create mock sdk and register it
      val mockSdk = PySdkToCreateVirtualEnv.create("MockPySdk", "", "")
      writeAction {
        ProjectJdkTable.getInstance().addJdk(mockSdk)
      }

      Disposer.register(testRootDisposable) {
        runWriteAction {
          ProjectJdkTable.getInstance().removeJdk(mockSdk)
        }
      }

      // assign the mock SDK to the project
      SdkConfigurationUtil.setDirectoryProjectSdk(project, mockSdk)

      return InstallationResult.Installed
    }
  }

  @Test
  fun `has both project and module SDK`() = testEnvironmentChecker { checker, task, _ ->
    assertNull("Environment checker must report no errors, if both project and module SDKs are configured", checker.getEnvironmentError(project, task))
  }

  @Test
  fun `has project SDK, no module SDK`() = testEnvironmentChecker { checker, task, taskModule ->
    setModuleSdk(taskModule, null)
    assertNull("Environment checker must report no errors, if project SDKs is configured", checker.getEnvironmentError(project, task))
  }

  @Test
  fun `no project SDK, has module SDK`() = testEnvironmentChecker { checker, task, taskModule ->
    val projectSdk = ProjectRootManager.getInstance(project).projectSdk
    SdkConfigurationUtil.setDirectoryProjectSdk(project, null)
    setModuleSdk(taskModule, projectSdk)

    assertNull("Environment checker must report no errors, if module SDKs is configured", checker.getEnvironmentError(project, task))
  }

  @Test
  fun `no project SDK, no module SDK`() = testEnvironmentChecker { checker, task, _ ->
    SdkConfigurationUtil.setDirectoryProjectSdk(project, null)

    val checkResult = checker.getEnvironmentError(project, task) ?: error("Environment checker must return error, if both project and module SDKs are null")

    assertEquals("Status for ${task.name} doesn't match", CheckStatus.Unchecked, checkResult.status)
    assertThat(
      "Checker output for ${task.name} doesn't match",
      checkResult.message,
      containsString(EduCoreBundle.message("error.no.interpreter", EduFormatNames.PYTHON))
    )
  }

  private fun testEnvironmentChecker(test: (EnvironmentChecker, Task, taskModule: Module) -> Unit) {
    val course = course(language = PythonLanguage.INSTANCE, courseMode = CourseMode.EDUCATOR, environment = UNITTEST) {
      lesson {
        eduTask {
          taskFile("task.py")
        }
      }
    }
    createCourseStructure(course)

    val configurator = course.configurator ?: error("No course configurator")
    val environmentChecker = configurator.taskCheckerProvider.envChecker

    val task = course.allTasks[0]
    val taskDir = task.getDir(project.courseDir) ?: error("Task directory is not found")
    val taskModule = ModuleUtilCore.findModuleForFile(taskDir, project) ?: error("Task module is not found")

    test(environmentChecker, task, taskModule)
  }
}