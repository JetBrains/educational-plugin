package com.jetbrains.edu.coursecreator.ui

import com.intellij.openapi.project.Project
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.taskDescription.ui.TaskDescriptionView
import com.jetbrains.edu.learning.taskDescription.ui.loadText
import com.jetbrains.edu.learning.taskDescription.ui.styleManagers.StyleManager
import com.jetbrains.edu.learning.taskDescription.ui.styleManagers.StyleResourcesManager
import com.jetbrains.edu.learning.taskDescription.ui.tab.AdditionalTab
import com.jetbrains.edu.learning.taskDescription.ui.tab.TabManager.TabType.YAML_HELP_TAB
import org.apache.commons.lang.text.StrSubstitutor

class YamlHelpTab(project: Project) : AdditionalTab(project, YAML_HELP_TAB) {

  init {
    var templateText = loadText("/yaml-tab/yaml-tab-template.html") ?: "Cannot load yaml documentation"
    templateText = StrSubstitutor(StyleManager.resources(templateText)).replace(templateText)
    val yamlCss = if (UIUtil.isUnderDarcula()) "/yaml-tab/yaml-base-darcula.css" else "/yaml-tab/yaml-base.css"
    templateText = templateText.replace("\${yaml-base-css}", StyleResourcesManager.resourceUrl(yamlCss))

    setText(templateText, plain = true)
  }

  companion object {
    @JvmStatic
    fun show(project: Project) = TaskDescriptionView.getInstance(project).showTab(YAML_HELP_TAB)
  }
}
