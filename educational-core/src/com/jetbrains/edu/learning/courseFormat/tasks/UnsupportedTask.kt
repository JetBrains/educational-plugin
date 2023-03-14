package com.jetbrains.edu.learning.courseFormat.tasks

import com.jetbrains.edu.learning.courseFormat.CheckStatus
import java.util.*

class UnsupportedTask : Task {
  constructor()

  constructor(name: String) : super(name)

  constructor(name: String, id: Int, position: Int, updateDate: Date, status: CheckStatus) :
    super(name, id, position, updateDate, status)

  override val itemType: String = UNSUPPORTED_TASK_TYPE

  companion object {
    const val UNSUPPORTED_TASK_TYPE: String = "unsupported"

    fun getDescriptionTextTemplate(name: String, link: String, platformName: String): String {
      val fixedTaskName = name.lowercase().replaceFirstChar { it.titlecaseChar() }
      return "$fixedTaskName tasks are not supported yet. <br>" +
             "Solve this step on <a href=\"$link\">$platformName</a>. <br><br>" +
             "After you have solved the step, click \"Sync with Browser\"  to move on."
    }
  }
}