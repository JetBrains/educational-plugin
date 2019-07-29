package com.jetbrains.edu.coursecreator.yaml

import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.content.ContentManager
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.EduSettings
import com.jetbrains.edu.learning.ui.taskDescription.*
import com.jetbrains.edu.learning.ui.taskDescription.check.CheckDetailsPanel
import com.jetbrains.edu.learning.ui.taskDescription.styleManagers.StyleManager
import com.jetbrains.edu.learning.ui.taskDescription.styleManagers.resourceUrl
import org.apache.commons.lang.text.StrSubstitutor
import java.awt.BorderLayout
import javax.swing.JPanel

private const val NAME = "YAML Help"

class YamlInfoTaskDescriptionTab(val project: Project) : JPanel() {

  init {
    var templateText = loadText("/yaml-tab/yaml-tab-template.html") ?: "Cannot load yaml documentation"
    val styleManager = StyleManager()

    templateText = StrSubstitutor(styleManager.resources(templateText)).replace(templateText)
    val yamlCss = if (UIUtil.isUnderDarcula()) "/yaml-tab/yaml-base-darcula.css" else "/yaml-tab/yaml-base.css"
    templateText = templateText.replace("\${yaml-base-css}", resourceUrl(yamlCss))
    templateText = templateText.replace("\${base-css}", styleManager.baseStylesheet)


    val panel = if (EduSettings.getInstance().shouldUseJavaFx()) {
      val browserWindow = BrowserWindow(project, false)
      browserWindow.loadContent(templateText, null)
      browserWindow.panel
    }
    else {
      val textPane = createTextPane()
      textPane.text = templateText
      val scrollPane = JBScrollPane(textPane)
      scrollPane.border = JBUI.Borders.empty(20, 0, 0, 10)
      scrollPane
    }
    layout = BorderLayout()
    add(LightColoredActionLink(
      "Back to the task description",
      CheckDetailsPanel.SwitchTaskTabAction(project, 0),
      AllIcons.Actions.Back), BorderLayout.NORTH)
    add(panel, BorderLayout.CENTER)
    background = TaskDescriptionView.getTaskDescriptionBackgroundColor()
    border = JBUI.Borders.empty(8, 15, 0, 0)
  }
}

fun addTabToTaskDescription(project: Project) {
  val contentManager = project.taskDescriptionTWContentManager ?: return
  val content = contentManager.findContent(NAME)
  content?.let { contentManager.removeContent(it, true) }
  val yamlInfoTab = ContentFactory.SERVICE.getInstance().createContent(YamlInfoTaskDescriptionTab(project), NAME, false)
  yamlInfoTab.isCloseable = false
  contentManager.addContent(yamlInfoTab, contentManager.contents.size)
}

private val Project.taskDescriptionTWContentManager: ContentManager?
  get() {
    val toolWindow = ToolWindowManager.getInstance(this).getToolWindow(TaskDescriptionToolWindowFactory.STUDY_TOOL_WINDOW)
    return toolWindow.contentManager
  }

fun showYamlTab(project: Project) {
  val contentManager = project.taskDescriptionTWContentManager ?: return
  val yamlTab = contentManager.findContent(NAME) ?: return
  contentManager.setSelectedContent(yamlTab)
}