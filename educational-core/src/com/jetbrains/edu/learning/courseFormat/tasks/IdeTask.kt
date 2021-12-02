package com.jetbrains.edu.learning.courseFormat.tasks

import com.jetbrains.edu.EducationalCoreIcons
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import java.util.*
import javax.swing.Icon

class IdeTask : Task {

  //used for deserialization
  constructor() : super()

  constructor(name: String) : super(name)

  constructor(name: String, id: Int, position: Int, updateDate: Date, status: CheckStatus) : super(name, id, position, updateDate, status)

  override fun getItemType() = IDE_TASK_TYPE

  override fun getIcon(): Icon {
    if (myStatus == CheckStatus.Unchecked) {
      return EducationalCoreIcons.IdeTask
    }
    return if (myStatus == CheckStatus.Solved) EducationalCoreIcons.IdeTaskSolved else EducationalCoreIcons.TaskFailed
  }

  override fun isToSubmitToRemote(): Boolean {
    return myStatus != CheckStatus.Unchecked
  }

  companion object {
    const val IDE_TASK_TYPE: String = "ide"
  }
}