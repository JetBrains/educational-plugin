package com.jetbrains.edu.learning.courseFormat.tasks

import com.jetbrains.edu.learning.courseFormat.CheckStatus
import icons.EducationalCoreIcons
import java.util.*
import javax.swing.Icon

class IdeTask : Task {

  @Suppress("unused") //used for deserialization
  constructor() : super()

  constructor(name: String, id: Int, position: Int, updateDate: Date, status: CheckStatus) : super(name, id, position, updateDate, status)

  override fun getItemType() = "ide"

  override fun getIcon(): Icon {
    if (myStatus == CheckStatus.Unchecked) {
      return EducationalCoreIcons.IdeTask
    }
    return if (myStatus == CheckStatus.Solved) EducationalCoreIcons.IdeTaskSolved else EducationalCoreIcons.TaskFailed
  }

  override fun isToSubmitToStepik(): Boolean {
    return myStatus != CheckStatus.Unchecked
  }
}