package com.jetbrains.edu.learning.stepik.hyperskill.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.EduUtilsKt.showPopup
import com.jetbrains.edu.learning.courseFormat.CheckStatus.Unchecked
import com.jetbrains.edu.learning.courseFormat.tasks.data.DataTask
import com.jetbrains.edu.learning.messages.EduCoreBundle
import org.jetbrains.annotations.NonNls

class DownloadDatasetAction :
  DownloadDatasetActionBase(EduCoreBundle.lazyMessage("hyperskill.action.download.dataset"), PROCESS_MESSAGE, Unchecked) {

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val task = EduUtils.getCurrentTask(project) as? DataTask ?: return

    if (!checkAuthorized(project, task.course)) return

    if (!DownloadDataset.lock(project)) {
      e.dataContext.showPopup(EduCoreBundle.message("hyperskill.download.dataset.already.running"))
      return
    }

    downloadDatasetInBackground(project, task, false)
  }

  companion object {
    @NonNls
    const val ACTION_ID: String = "Hyperskill.DownloadDataset"

    @NonNls
    private const val PROCESS_MESSAGE: String = "Download dataset in progress"
  }
}