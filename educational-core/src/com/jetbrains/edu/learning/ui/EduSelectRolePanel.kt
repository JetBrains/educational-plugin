package com.jetbrains.edu.learning.ui

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.ui.VerticalFlowLayout
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.coursecreator.actions.CCPluginToggleAction
import icons.EducationalCoreIcons
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants


class EduSelectRolePanel : JPanel() {
  private var myStudent: JLabel

  init {
    val layout = FlowLayout()
    layout.hgap = 20
    setLayout(layout)
    val iconSize = JBUI.scale(180)
    val studentPanel = JPanel(VerticalFlowLayout())
    val teacher = JLabel(EducationalCoreIcons.Teacher)
    myStudent = JLabel(EducationalCoreIcons.StudentHover)
    myStudent.preferredSize = Dimension(iconSize, iconSize)
    PropertiesComponent.getInstance().setValue(CCPluginToggleAction.COURSE_CREATOR_ENABLED, false, true)
    myStudent.addMouseListener(object : MouseAdapter() {
      override fun mouseClicked(e: MouseEvent?) {
        PropertiesComponent.getInstance().setValue(CCPluginToggleAction.COURSE_CREATOR_ENABLED, false)
        myStudent.icon = EducationalCoreIcons.StudentHover
        teacher.icon = EducationalCoreIcons.Teacher
      }
    })
    studentPanel.add(myStudent)
    studentPanel.add(JLabel("Student", SwingConstants.CENTER))
    add(studentPanel)

    teacher.preferredSize = Dimension(iconSize, iconSize)
    val teacherPanel = JPanel(VerticalFlowLayout())
    teacherPanel.add(teacher)
    teacherPanel.add(JLabel("Teacher", SwingConstants.CENTER))
    teacher.addMouseListener(object : MouseAdapter() {
      override fun mouseClicked(e: MouseEvent?) {
        PropertiesComponent.getInstance().setValue(CCPluginToggleAction.COURSE_CREATOR_ENABLED, true)
        myStudent.icon = EducationalCoreIcons.Student
        teacher.icon = EducationalCoreIcons.TeacherHover
      }
    })
    add(teacherPanel)
  }

  fun getStudentButton(): JComponent {
    return myStudent
  }

}