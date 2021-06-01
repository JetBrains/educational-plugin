package com.jetbrains.edu.coursecreator.ui

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.taskDescription.ui.TaskDescriptionView
import com.jetbrains.edu.learning.taskDescription.ui.loadText
import com.jetbrains.edu.learning.taskDescription.ui.styleManagers.StyleManager
import com.jetbrains.edu.learning.taskDescription.ui.tab.AdditionalTab
import com.jetbrains.edu.learning.taskDescription.ui.tab.TabManager.TabType.YAML_HELP_TAB
import org.apache.commons.lang.text.StrSubstitutor

class YamlHelpTab(project: Project) : AdditionalTab(project, YAML_HELP_TAB) {

  init {
    init()
    val template = loadText(YAML_TAB_TEMPLATE_FILE) ?: "Cannot load yaml documentation"
    val text = StrSubstitutor(StyleManager.resources()).replace(template)
    setText(text, plain = true)
  }

  companion object {
    private const val YAML_TAB_TEMPLATE_FILE = "/style/yaml-tab/yaml-tab-template.html.ft"

    @JvmStatic
    fun show(project: Project) = TaskDescriptionView.getInstance(project).showTab(YAML_HELP_TAB)
  }
}
