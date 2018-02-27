package com.jetbrains.edu.learning.courseFormat.tasks

import icons.EducationalCoreIcons
import javax.swing.Icon

class IdeTask: Task() {
  override fun getTaskType() = "ide"

  override fun getIcon(): Icon {
    return EducationalCoreIcons.IdeTask
  }
}