package com.jetbrains.edu.coursecreator.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.bindItemNullable
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.ext.allTasks
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.isUnitTestMode
import com.jetbrains.edu.learning.messages.EduCoreBundle
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

private class SelectTaskDialog(project: Project, private val course: EduCourse) : DialogWrapper(project) {

  private var selectedTask: Task? = null

  init {
    title = EduCoreBundle.message("course.creator.select.task.dialog.title")
    super.init()
  }

  override fun createCenterPanel(): JComponent {
    val panel = panel {
      row {
        val tasks = course.allTasks
        selectedTask = tasks.firstOrNull()
        // BACKCOMPAT: 2022.3.
        // Use `bindItem` instead of `bindItemNullable`
        @Suppress("UnstableApiUsage", "DEPRECATION")
        comboBox(tasks, TaskRenderer())
          .align(AlignX.FILL)
          .focused()
          .bindItemNullable(::selectedTask)
      }
    }
    panel.minimumSize = JBUI.size(400, 80)
    return panel
  }

  fun showAndGetSelectedTask(): Task? {
    return if (showAndGet()) selectedTask else null
  }

  private class TaskRenderer : DefaultListCellRenderer() {
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
