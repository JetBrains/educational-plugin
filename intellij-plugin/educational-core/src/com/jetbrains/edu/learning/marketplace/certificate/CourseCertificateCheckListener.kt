package com.jetbrains.edu.learning.marketplace.certificate

import com.intellij.openapi.application.EDT
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.checker.CheckListener
import com.jetbrains.edu.learning.courseFormat.CheckResult
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.marketplace.api.MarketplaceConnector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CourseCertificateCheckListener : CheckListener {

  override fun afterCheck(project: Project, task: Task, result: CheckResult) {
    if (!result.isSolved) return
    with(CourseCertificateManager.getInstance(project)) {
      scope.launch {
        val state = updateCertificateState()
        if (state == CourseCertificationState.Unsupported) {
          log.debug("Course suddenly stopped being certifiable")
          return@launch
        }
        val dialogUIFactory = CourseCertificateDialogUIFactory.getInstance(project)
        val isLoggedIn = MarketplaceConnector.getInstance().isLoggedIn()
        // If the user has already been notified about this certificate, the dialog shouldn't be shown again
        if (!isLoggedIn && dialogUIFactory.isDismissed) return@launch

        when (state) {
          is CourseCertificationState.Issued -> {
            if (state.isFirstClientRequest) {
              val certificateId = state.certificateId
              withContext(Dispatchers.EDT) {
                dialogUIFactory.create(task.course, certificateId).show()
              }
            }
          }

          else -> {
            if (isEligibleForCertificate(task.course)) {
              withContext(Dispatchers.EDT) {
                dialogUIFactory.create(task.course, null).show()
              }
            }
          }
        }
      }
    }
  }

  companion object {
    private val log = logger<CourseCertificateCheckListener>()
  }
}
