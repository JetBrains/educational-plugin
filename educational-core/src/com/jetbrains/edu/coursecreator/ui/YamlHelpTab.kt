package com.jetbrains.edu.coursecreator.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.taskDescription.ui.TaskDescriptionView
import com.jetbrains.edu.learning.taskDescription.ui.loadText
import com.jetbrains.edu.learning.taskDescription.ui.styleManagers.StyleManager
import com.jetbrains.edu.learning.taskDescription.ui.tab.AdditionalTab
import com.jetbrains.edu.learning.taskDescription.ui.tab.TabType.YAML_HELP_TAB
import com.jetbrains.edu.learning.taskDescription.ui.uihtml.HtmlTransformerContext
import com.jetbrains.edu.learning.taskDescription.ui.uihtml.viewers.createViewerDependingOnCurrentUILibrary
import org.apache.commons.lang.text.StrSubstitutor
import org.jetbrains.annotations.Nls

class YamlHelpTab(project: Project) : AdditionalTab(project, YAML_HELP_TAB) {

  private val htmlViewer = createViewerDependingOnCurrentUILibrary(project)
  override val innerTextPanel
    get() = htmlViewer.component

  init {
    setupTextViewer()
    Disposer.register(this, htmlViewer)
    val template = loadText(YAML_TAB_TEMPLATE_FILE) ?: getCannotLoadYamlDocumentationText()
    val text = StrSubstitutor(StyleManager.resources()).replace(template)

    htmlViewer.setHtmlWithContext(text, HtmlTransformerContext(project, null))
  }

  override fun update(task: Task) {}

  @Nls
  private fun getCannotLoadYamlDocumentationText(): String = EduCoreBundle.message("course.creator.cannot.load.yaml.documentation")

  companion object {
    private const val YAML_TAB_TEMPLATE_FILE = "/style/yaml-tab/yaml-tab-template.html.ft"

    @JvmStatic
    fun show(project: Project) = TaskDescriptionView.getInstance(project).showTab(YAML_HELP_TAB)
  }
}
