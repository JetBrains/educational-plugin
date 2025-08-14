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
import com.jetbrains.edu.learning.marketplace.api.MarketplaceSubmissionsConnector
import com.jetbrains.edu.learning.marketplace.api.UserAgreement
import com.jetbrains.edu.learning.onError
import com.jetbrains.edu.learning.projectView.CourseViewPane
import com.jetbrains.edu.learning.submissions.UserAgreementState
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
    // If it's not JetBrains Academy project, switch from Course View to Project view.
    // It's necessary if a user declined user agreement and the plugin reloaded the project to open it as a common one
    if (!project.isEduProject()) {
      changeProjectView(project)
      return
    }
    val currentLocalAgreementState = UserAgreementSettings.getInstance().userAgreementProperties.value
    // If user agreement was shown before or suppressed by system properties, do nothing
    if (currentLocalAgreementState.pluginAgreement != UserAgreementState.NOT_SHOWN ||
        System.getProperty(DISABLE_USER_AGREEMENT)?.toBoolean() == true) return

    // Load the remote agreement state only if the current state is not caused by direct user action.
    // Important for `Reset User Agreement` action
    if (!currentLocalAgreementState.isChangedByUser) {
      val remoteAgreementState = withContext(Dispatchers.IO) {
        MarketplaceSubmissionsConnector.getInstance().getUserAgreement().onError {
          UserAgreement(UserAgreementState.NOT_SHOWN, UserAgreementState.NOT_SHOWN)
        }
      }

      if (remoteAgreementState.pluginAgreement != UserAgreementState.NOT_SHOWN) {
        val newLocalAgreementState = UserAgreementSettings.UserAgreementProperties(
          remoteAgreementState.pluginAgreement,
          remoteAgreementState.aiTermsOfService,
          isChangedByUser = false
        )
        // Apply remote agreement state for local settings and don't show the agreement dialog
        UserAgreementSettings.getInstance().updatePluginAgreementState(newLocalAgreementState)
        return
      }
    }

    UserAgreementManager.getInstance().showUserAgreement(project)
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
