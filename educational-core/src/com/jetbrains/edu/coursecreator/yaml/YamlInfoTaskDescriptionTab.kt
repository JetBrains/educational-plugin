package com.jetbrains.edu.coursecreator.yaml

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.content.ContentManager
import com.intellij.ui.jcef.JBCefBrowser
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.EduSettings
import com.jetbrains.edu.learning.JavaUILibrary
import com.jetbrains.edu.learning.taskDescription.ui.*
import com.jetbrains.edu.learning.taskDescription.ui.styleManagers.StyleManager
import com.jetbrains.edu.learning.taskDescription.ui.styleManagers.StyleResourcesManager.resourceUrl
import org.apache.commons.lang.text.StrSubstitutor
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JPanel

private const val NAME = "YAML Help"

class YamlInfoTaskDescriptionTab(val project: Project) : JPanel(), Disposable {

  init {
    var templateText = loadText("/yaml-tab/yaml-tab-template.html") ?: "Cannot load yaml documentation"
    templateText = StrSubstitutor(StyleManager.resources(templateText)).replace(templateText)
    val yamlCss = if (UIUtil.isUnderDarcula()) "/yaml-tab/yaml-base-darcula.css" else "/yaml-tab/yaml-base.css"
    templateText = templateText.replace("\${yaml-base-css}", resourceUrl(yamlCss))

    val defaultSwingPanel: (String) -> JComponent = {
      val textPane = createTextPane()
      textPane.text = templateText
      val scrollPane = JBScrollPane(textPane)
      scrollPane.border = JBUI.Borders.empty(20, 0, 0, 10)
      scrollPane
    }

    val panel = if (EduSettings.getInstance().javaUiLibraryWithCheck == JavaUILibrary.JCEF) {
      val browser = JBCefBrowser()
      Disposer.register(this, browser)
      browser.loadHTML(templateText)
      browser.component
    }
    else {
      defaultSwingPanel(templateText)
    }

    layout = BorderLayout()
    add(AdditionalTabPanel.getBackLinkPanel(project), BorderLayout.NORTH)
    add(panel, BorderLayout.CENTER)
    background = TaskDescriptionView.getTaskDescriptionBackgroundColor()
    border = JBUI.Borders.empty(0, 15, 0, 0)
  }

  override fun dispose() {}
}

fun addTabToTaskDescription(project: Project) {
  val contentManager = project.taskDescriptionTWContentManager ?: return
  val content = contentManager.findContent(NAME)
  content?.let { contentManager.removeContent(it, true) }
  val descriptionTab = YamlInfoTaskDescriptionTab(project)
  Disposer.register(contentManager, descriptionTab)
  val yamlInfoTab = ContentFactory.SERVICE.getInstance().createContent(descriptionTab, NAME, false)
  yamlInfoTab.isCloseable = false
  contentManager.addContent(yamlInfoTab, contentManager.contents.size)
}

private val Project.taskDescriptionTWContentManager: ContentManager?
  get() {
    val toolWindow = ToolWindowManager.getInstance(this).getToolWindow(TaskDescriptionToolWindowFactory.STUDY_TOOL_WINDOW)
    return toolWindow?.contentManager
  }

fun showYamlTab(project: Project) {
  val contentManager = project.taskDescriptionTWContentManager ?: return
  val yamlTab = contentManager.findContent(NAME) ?: return
  contentManager.setSelectedContent(yamlTab)
}
