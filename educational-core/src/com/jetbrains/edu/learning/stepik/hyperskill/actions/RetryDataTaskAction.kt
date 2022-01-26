package com.jetbrains.edu.learning.stepik.hyperskill.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.EduUtilsKt.showPopup
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.CheckStatus.Failed
import com.jetbrains.edu.learning.courseFormat.tasks.data.DataTask
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer
import org.jetbrains.annotations.NonNls

class RetryDataTaskAction : DownloadDatasetActionBase(EduCoreBundle.lazyMessage("retry"), PROCESS_MESSAGE, Failed) {

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val task = EduUtils.getCurrentTask(project) as? DataTask ?: return
    task.attempt = null
    task.status = CheckStatus.Unchecked
    YamlFormatSynchronizer.saveItemWithRemoteInfo(task)

    if (!checkAuthorized(project, task.course)) return

    if (!DownloadDataset.lock(project)) {
      e.dataContext.showPopup(EduCoreBundle.message("hyperskill.download.dataset.already.running"))
      return
    }

    downloadDatasetInBackground(project, task, true)
  }

  companion object {
    @NonNls
    const val ACTION_ID: String = "Hyperskill.RetryDataTask"

    @NonNls
    private const val PROCESS_MESSAGE: String = "Retry data task in progress"
  }
}