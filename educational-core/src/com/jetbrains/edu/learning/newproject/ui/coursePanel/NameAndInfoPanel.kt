package com.jetbrains.edu.learning.newproject.ui.coursePanel

import com.intellij.ide.plugins.newui.HorizontalLayout
import com.intellij.openapi.ui.VerticalFlowLayout
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.taskDescription.ui.styleManagers.TypographyManager
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Font
import java.text.SimpleDateFormat
import java.util.*
import javax.swing.JLabel
import javax.swing.JPanel

private const val HORIZONTAL_OFFSET = 10
private const val RIGHT_OFFSET = 15
private const val Y_OFFSET = 3
private const val TAGS_TOP_OFFSET = 10
private const val INFO_PANEL_TOP_OFFSET = 5

private const val FONT_SIZE = 26.0f
private val GRAY_TEXT_FOREGROUND: Color = JBColor.namedColor("Plugins.tagForeground", JBColor(0x787878, 0x999999))

class NameAndInfoPanel(errorHandler: CourseStartErrorHandler) : JPanel() {
  private var nameHtmlPanel: CourseNameHtmlPanel = CourseNameHtmlPanel()
  private val infoPanel = InfoPanel()
  private var tagsPanel: TagsPanel = TagsPanel()
  private var baselinePanel: BaselinePanel = BaselinePanel()
  private val startButton: StartCourseButtonBase = StartCourseButton(errorHandler)
  private val editButton: StartCourseButtonBase = EditCourseButton(errorHandler)

  init {
    border = JBUI.Borders.emptyRight(RIGHT_OFFSET)
    layout = VerticalFlowLayout(VerticalFlowLayout.TOP, 0, 0, true, false)

    baselinePanel.setYOffset(JBUI.scale(Y_OFFSET))
    baselinePanel.add(nameHtmlPanel)
    baselinePanel.addButtonComponent(startButton)
    baselinePanel.addButtonComponent(editButton)

    add(baselinePanel)
    add(infoPanel)

    tagsPanel = TagsPanel()
    add(tagsPanel)
    UIUtil.setBackgroundRecursively(this, UIUtil.getEditorPaneBackground())
  }

  fun update(course: Course, settings: CourseDisplaySettings, location: String, projectSettings: Any) {
    nameHtmlPanel.bind(course)
    infoPanel.bind(course, settings)
    tagsPanel.update(course, settings)
    startButton.update(course, location, projectSettings)
    editButton.update(course, location, projectSettings)

    revalidate()
    repaint()
  }

  fun setButtonsEnabled(isEnabled: Boolean) {
    startButton.isEnabled = isEnabled
    editButton.isEnabled = isVisible && isEnabled
  }
}

private class TagsPanel : JPanel(HorizontalLayout(HORIZONTAL_OFFSET)) {

  init {
    border = JBUI.Borders.emptyTop(TAGS_TOP_OFFSET)
  }

  private fun addTags(course: Course) {
    removeAll()
    for (tag in course.tags) {
      // TODO: add listener
      // TODO: is selected
      add(tag.createComponent())
    }
  }

  fun update(course: Course, settings: CourseDisplaySettings) {
    isVisible = settings.showTagsPanel
    if (settings.showTagsPanel) {
      addTags(course)
    }
  }
}

private class InfoPanel : JPanel(HorizontalLayout(HORIZONTAL_OFFSET)) {
  private var authorsLabel: JBLabel = JBLabel()
  private var dateLabel: JBLabel = JBLabel()

  init {
    border = JBUI.Borders.emptyTop(INFO_PANEL_TOP_OFFSET)

    authorsLabel.foreground = GRAY_TEXT_FOREGROUND
    dateLabel.foreground = GRAY_TEXT_FOREGROUND
    add(authorsLabel, BorderLayout.WEST)
    add(dateLabel, BorderLayout.EAST)
  }

  private val Course.formattedDate: String
    get() = SimpleDateFormat("MMM d, yyyy", Locale(languageCode)).format(course.updateDate)

  private val Course.allAuthors: String  // TODO: max size
    get() = course.authorFullNames.joinToString()

  fun bind(course: Course, settings: CourseDisplaySettings) {
    authorsLabel.isVisible = settings.showInstructorField && course.allAuthors.isNotEmpty()
    if (authorsLabel.isVisible) {
      authorsLabel.text = course.allAuthors
    }

    dateLabel.isVisible = course.updateDate != Date(0)
    if (dateLabel.isVisible) {
      dateLabel.text = course.formattedDate
    }
  }
}

private class CourseNameHtmlPanel : CourseHtmlPanel() {
  var baselineComponent: JLabel = JLabel()

  init {
    baselineComponent.font = bodyFont
  }

  /**
   *  copy-pasted from [com.intellij.ide.plugins.newui.PluginDetailsPageComponent.createNameComponent]
   */
  override fun getBaseline(width: Int, height: Int): Int {
    baselineComponent.text = body
    val size = baselineComponent.preferredSize
    return baselineComponent.getBaseline(size.width, size.height)
  }

  override fun getBody(): String {
    course?.let {
      return "<html><span>${it.name ?: ""}</span></html>"
    }
    return ""
  }

  override fun getBodyFont(): Font = Font(TypographyManager().bodyFont, Font.BOLD, JBUI.scaleFontSize(FONT_SIZE))
}