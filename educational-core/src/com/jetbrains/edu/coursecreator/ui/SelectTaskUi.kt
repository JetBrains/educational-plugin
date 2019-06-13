package com.jetbrains.edu.coursecreator.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.layout.*
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.ext.allTasks
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.isUnitTestMode
import org.jetbrains.annotations.TestOnly
import java.awt.Component
import javax.swing.DefaultListCellRenderer
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JList

private var MOCK: SelectTaskUi? = null

fun showSelectTaskDialog(project: Project, course: EduCourse): Task? {
  val ui = if (isUnitTestMode) {
    MOCK ?: error("You should set mock ui via `withMockSelectTaskUi`")
  } else {
    SelectTaskDialogUI()
  }
  return ui.selectTask(project, course)
}

@TestOnly
fun withMockSelectTaskUi(ui: SelectTaskUi, action: () -> Unit) {
  MOCK = ui
  try {
    action()
  } finally {
    MOCK = null
  }
}

interface SelectTaskUi {
  fun selectTask(project: Project, course: EduCourse): Task?
}

private class SelectTaskDialogUI : SelectTaskUi {
  override fun selectTask(project: Project, course: EduCourse): Task? = SelectTaskDialog(project, course).showAndGetSelectedTask()
}

private class SelectTaskDialog(project: Project, course: EduCourse) : DialogWrapper(project) {

  private val tasks: ComboBox<Task> = ComboBox(course.allTasks.toTypedArray())

  init {
    title = "Select Task"
    tasks.renderer = object : DefaultListCellRenderer() {
      override fun getListCellRendererComponent(
        list: JList<*>,
        value: Any?,
        index: Int,
        isSelected: Boolean,
        cellHasFocus: Boolean
      ): Component {
        val component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
        if (component is JLabel && value is Task) {
          component.text = value.fullName
        }
        return component
      }
    }
    super.init()
  }

  override fun createCenterPanel(): JComponent? {
    val panel = panel {
      row { tasks(CCFlags.growX) }
    }
    panel.minimumSize = JBUI.size(400, 80)
    return panel
  }

  fun showAndGetSelectedTask(): Task? {
    return if (showAndGet()) tasks.selectedItem as Task else null
  }
}

private val Task.fullName: String get() {
  val lesson = lesson
  val section = lesson.section
  return buildString {
    if (section != null) {
      append("${section.name}.")
    }
    append("${lesson.name}.")
    append(name)
  }
}
