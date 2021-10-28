package com.jetbrains.edu.learning.taskDescription.ui.tab

import com.intellij.openapi.project.Project
import com.jetbrains.edu.coursecreator.ui.YamlHelpTab
import com.jetbrains.edu.learning.messages.BUNDLE
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.stepik.hyperskill.TheoryTab
import com.jetbrains.edu.learning.stepik.hyperskill.TopicsTab
import com.jetbrains.edu.learning.submissions.SubmissionsTab
import org.jetbrains.annotations.PropertyKey

enum class TabType(@PropertyKey(resourceBundle = BUNDLE) private val nameId: String) {
  THEORY_TAB("hyperskill.theory.tab.name"),
  TOPICS_TAB("hyperskill.topics.tab.name"),
  SUBMISSIONS_TAB("submissions.tab.name"),
  YAML_HELP_TAB("yaml.help.tab.name");

  val tabName: String get() = EduCoreBundle.message(nameId)

  fun createTab(project: Project): AdditionalTab {
    return when (this) {
      THEORY_TAB -> TheoryTab(project)
      TOPICS_TAB -> TopicsTab(project)
      SUBMISSIONS_TAB -> SubmissionsTab(project)
      YAML_HELP_TAB -> YamlHelpTab(project)
    }
  }
}