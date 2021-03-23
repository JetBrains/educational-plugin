package com.jetbrains.edu.learning.stepik.hyperskill

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.tasks.TheoryTask
import com.jetbrains.edu.learning.taskDescription.ui.EduBrowserHyperlinkListener
import com.jetbrains.edu.learning.taskDescription.ui.TaskDescriptionToolWindow.Companion.getTaskDescriptionWithCodeHighlighting
import com.jetbrains.edu.learning.taskDescription.ui.tab.AdditionalTab
import com.jetbrains.edu.learning.taskDescription.ui.tab.TabManager.TabType.THEORY_TAB

class TheoryTab(project: Project, theoryTask: TheoryTask) : AdditionalTab(project, THEORY_TAB) {

  init {
    addHyperlinkListener(EduBrowserHyperlinkListener.INSTANCE)

    val text = getTaskDescriptionWithCodeHighlighting(project, theoryTask)
    setText(text, plain = false)
  }
}