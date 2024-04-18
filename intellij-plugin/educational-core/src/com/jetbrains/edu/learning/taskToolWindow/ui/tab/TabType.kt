package com.jetbrains.edu.learning.taskToolWindow.ui.tab

import com.jetbrains.edu.learning.messages.BUNDLE
import com.jetbrains.edu.learning.messages.EduCoreBundle
import org.jetbrains.annotations.PropertyKey

enum class TabType(@PropertyKey(resourceBundle = BUNDLE) private val nameId: String) {
  DESCRIPTION_TAB("description.tab.name"),
  THEORY_TAB("hyperskill.theory.tab.name"),
  TOPICS_TAB("hyperskill.topics.tab.name"),
  SUBMISSIONS_TAB("submissions.tab.name");

  val tabName: String get() = EduCoreBundle.message(nameId)
}