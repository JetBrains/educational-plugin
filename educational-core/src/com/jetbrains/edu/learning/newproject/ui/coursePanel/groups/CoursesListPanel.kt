package com.jetbrains.edu.learning.newproject.ui.coursePanel.groups

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.newproject.ui.coursePanel.CourseInfo
import com.jetbrains.edu.learning.newproject.ui.coursePanel.CourseMode
import com.jetbrains.edu.learning.newproject.ui.coursePanel.NewCoursePanel
import com.jetbrains.edu.learning.taskDescription.ui.LightColoredActionLink
import com.jetbrains.edu.learning.taskDescription.ui.TaskDescriptionView
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.ScrollPaneConstants

class CoursesListPanel(selectionChanged: () -> Unit, joinCourse: (CourseInfo, CourseMode) -> Unit, importCourseAction: AnAction) : JPanel(BorderLayout()) {
  var bottomPanel: JPanel = JPanel(BorderLayout())
  private val groupsComponent: GroupsComponent = GroupsComponent(selectionChanged, joinCourse)

  init {
    background = TaskDescriptionView.getTaskDescriptionBackgroundColor()

    val scrollPane = createScrollPane(groupsComponent)
    scrollPane.border = JBUI.Borders.customLine(NewCoursePanel.DIVIDER_COLOR, 0, 0, 1, 0)
    add(scrollPane)

    bottomPanel.isOpaque = true
    bottomPanel.background = TaskDescriptionView.getTaskDescriptionBackgroundColor()
    // TODO: replace with separate "open course from disk" and "import stepik course" actions
    val actionLink = LightColoredActionLink("Import Course...", importCourseAction)
    bottomPanel.add(actionLink, BorderLayout.WEST)
    actionLink.border = JBUI.Borders.empty(TOP_BOTTOM, LEFT_RIGHT, TOP_BOTTOM, LEFT_RIGHT)
    add(bottomPanel, BorderLayout.SOUTH)
  }

  private fun createScrollPane(panel: JPanel): JComponent {
    val pane = JBScrollPane(panel,
                            ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER)
    pane.background = TaskDescriptionView.getTaskDescriptionBackgroundColor()
    pane.border = JBUI.Borders.empty()
    return pane
  }

  fun addGroup(titleString: String, courseInfos: List<CourseInfo>) {
    groupsComponent.addGroup(titleString, courseInfos)
  }

  fun clear() {
    groupsComponent.clear()
  }

  fun setSelectedValue(newCourseToSelect: Course?) {
    groupsComponent.setSelectedValue(newCourseToSelect)
  }

  fun initialSelection() {
    groupsComponent.initialSelection()
  }

  val selectedValue: Course?
    get() = groupsComponent.selectedValue

}