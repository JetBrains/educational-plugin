package com.jetbrains.edu.jvm.gradle.checker

import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.jetbrains.edu.jvm.gradle.GradleCourseRefresher
import com.jetbrains.edu.jvm.gradle.generation.EduGradleUtils
import com.jetbrains.edu.jvm.messages.EduJVMBundle
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.EduNames.ENVIRONMENT_CONFIGURATION_LINK_GRADLE
import com.jetbrains.edu.learning.RefreshCause
import com.jetbrains.edu.learning.checker.CheckResult
import com.jetbrains.edu.learning.checker.EnvironmentChecker
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.ext.getDir
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.taskDescription.ui.EduBrowserHyperlinkListener
import org.jetbrains.plugins.gradle.settings.GradleSettings
import javax.swing.event.HyperlinkEvent

class GradleEnvironmentChecker : EnvironmentChecker() {
  override fun getEnvironmentError(project: Project, task: Task): CheckResult? {
    ProjectRootManager.getInstance(project).projectSdk ?: return noSdkConfiguredResult
    val taskDir = task.getDir(project.courseDir) ?: return getFailedToLaunchCheckingResult(project)
    val module = invokeAndWaitIfNeeded {  ModuleUtil.findModuleForFile(taskDir, project) } ?: return getFailedToLaunchCheckingResult(project)

    val path = ExternalSystemApiUtil.getExternalRootProjectPath(module) ?: return getGradleNotImportedResult(project)
    GradleSettings.getInstance(project).getLinkedProjectSettings(path) ?: return getGradleNotImportedResult(project)
    return null
  }

  private class ReloadGradleHyperlinkListener(private val project: Project) : EduBrowserHyperlinkListener() {
    override fun hyperlinkActivated(e: HyperlinkEvent) {
      if (e.url == null && e.description == RELOAD_GRADLE_LINK) {
        EduGradleUtils.updateGradleSettings(project)
        EduGradleUtils.setupGradleProject(project)
        val refresher = GradleCourseRefresher.firstAvailable() ?: error("Can not find Gradle course refresher")
        refresher.refresh(project, RefreshCause.STRUCTURE_MODIFIED)
      }
      else {
        super.hyperlinkActivated(e)
      }
    }
  }

  companion object {
    private const val RELOAD_GRADLE_LINK: String = "reload_gradle"

    private val noSdkConfiguredResult: CheckResult
      get() = CheckResult(CheckStatus.Unchecked, EduCoreBundle.message("error.no.sdk.gradle", ENVIRONMENT_CONFIGURATION_LINK_GRADLE))

    fun getFailedToLaunchCheckingResult(project: Project): CheckResult {
      return CheckResult(CheckStatus.Unchecked,
                         EduCoreBundle.message("error.failed.to.launch.checking.with.message", RELOAD_GRADLE_LINK, EduNames.NO_TESTS_URL),
                         hyperlinkListener = ReloadGradleHyperlinkListener(project))
    }

    private fun getGradleNotImportedResult(project: Project): CheckResult {
      return CheckResult(CheckStatus.Unchecked,
                         EduJVMBundle.message("error.gradle.not.imported", RELOAD_GRADLE_LINK, EduNames.NO_TESTS_URL),
                         hyperlinkListener = ReloadGradleHyperlinkListener(project))
    }
  }
}
