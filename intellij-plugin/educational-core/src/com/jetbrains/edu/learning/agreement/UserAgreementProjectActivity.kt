package com.jetbrains.edu.learning.agreement

import com.intellij.ide.projectView.ProjectView
import com.intellij.ide.projectView.impl.ProjectViewPane
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.EDT
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.ToolWindowId
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.util.asSafely
import com.jetbrains.edu.learning.EduUtilsKt.isEduProject
import com.jetbrains.edu.learning.isHeadlessEnvironment
import com.jetbrains.edu.learning.projectView.CourseViewPane
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.annotations.TestOnly
import java.util.concurrent.atomic.AtomicReference

private const val DISABLE_USER_AGREEMENT = "edu.disable.user.agreement"

@Suppress("CompanionObjectInExtension")
class UserAgreementProjectActivity : ProjectActivity {
  override suspend fun execute(project: Project) {
    val condition = condition.get()
    if (isHeadlessEnvironment && condition !is ActivityTestCondition.Enabled) return

    executeImpl(project)

    condition?.asSafely<ActivityTestCondition.Enabled>()?.onFinished?.invoke()
  }

  private suspend fun executeImpl(project: Project) {
    if (!project.isEduProject()) {
      changeProjectView(project)
      return
    }
    if (UserAgreementSettings.getInstance().isNotShown && System.getProperty(DISABLE_USER_AGREEMENT)?.toBoolean() != true) {
      UserAgreementManager.getInstance().showUserAgreement(project)
    }
  }

  private suspend fun changeProjectView(project: Project) {
    val toolWindow = ToolWindowManager.getInstance(project).getToolWindow(ToolWindowId.PROJECT_VIEW) ?: return
    val projectView = ProjectView.getInstance(project)
    val currentViewId = projectView.currentViewId
    withContext(Dispatchers.EDT) {
      if (CourseViewPane.ID == currentViewId) {
        projectView.changeView(ProjectViewPane.ID)
      }
      toolWindow.show()
    }
  }

  companion object
}

private val condition: AtomicReference<ActivityTestCondition> = AtomicReference(ActivityTestCondition.Disabled)

private sealed class ActivityTestCondition() {
  data object Disabled : ActivityTestCondition()
  data class Enabled(val onFinished: () -> Unit) : ActivityTestCondition()
}

@TestOnly
fun UserAgreementProjectActivity.Companion.enableActivityInTests(disposable: Disposable, onFinished: () -> Unit) {
  val successful = condition.compareAndSet(ActivityTestCondition.Disabled, ActivityTestCondition.Enabled(onFinished))
  if (!successful) {
    error("`enabledActivityInTests` should be called at most once")
  }
  Disposer.register(disposable) {
    condition.set(ActivityTestCondition.Disabled)
  }
}
