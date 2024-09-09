package com.jetbrains.edu.learning.stepik.hyperskill

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseFormat.tasks.TheoryTask
import com.jetbrains.edu.learning.taskToolWindow.ui.TaskToolWindow.Companion.getTaskDescription
import com.jetbrains.edu.learning.taskToolWindow.ui.tab.TabType.THEORY_TAB
import com.jetbrains.edu.learning.taskToolWindow.ui.tab.TaskToolWindowTextTab

/**
 * Constructor is called exclusively in [com.jetbrains.edu.learning.taskToolWindow.ui.tab.TabManager.createTab]
 * and MUST NOT be called in any other places
 */
class TheoryTab(project: Project) : TaskToolWindowTextTab(project, THEORY_TAB) {

  init {
    init()
  }

  override fun update(task: Task) {
    if (task !is TheoryTask) {
      error("Selected task isn't Theory task")
    }

    setText(getTaskDescription(project, task, uiMode))
  }
}