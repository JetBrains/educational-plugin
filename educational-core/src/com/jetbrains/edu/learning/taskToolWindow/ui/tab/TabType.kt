package com.jetbrains.edu.learning.taskToolWindow.ui.tab

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.messages.BUNDLE
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.stepik.hyperskill.TheoryTab
import com.jetbrains.edu.learning.stepik.hyperskill.TopicsTab
import com.jetbrains.edu.learning.submissions.SubmissionsTab
import org.jetbrains.annotations.PropertyKey

enum class TabType(@PropertyKey(resourceBundle = BUNDLE) private val nameId: String) {
  DESCRIPTION_TAB("description.tab.name"),
  THEORY_TAB("hyperskill.theory.tab.name"),
  TOPICS_TAB("hyperskill.topics.tab.name"),
  SUBMISSIONS_TAB("submissions.tab.name");

  val tabName: String get() = EduCoreBundle.message(nameId)

  fun createTab(project: Project): TaskToolWindowTab {
    return when (this) {
      DESCRIPTION_TAB -> DescriptionTab(project)
      THEORY_TAB -> TheoryTab(project)
      TOPICS_TAB -> TopicsTab(project)
      SUBMISSIONS_TAB -> SubmissionsTab(project)
    }
  }
}