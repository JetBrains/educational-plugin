package com.jetbrains.edu.learning.courseFormat.tasks

import com.jetbrains.edu.learning.courseFormat.CheckStatus
import icons.EducationalCoreIcons
import javax.swing.Icon

class IdeTask: Task() {
  override fun getTaskType() = "ide"

  override fun getIcon(): Icon {
    if (myStatus == CheckStatus.Unchecked) {
      return EducationalCoreIcons.IdeTask
    }
    return if (myStatus == CheckStatus.Solved) EducationalCoreIcons.IdeTaskSolved else EducationalCoreIcons.TaskFailed
  }
}