package com.jetbrains.edu.learning.ui

import com.intellij.openapi.ui.VerticalFlowLayout
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.coursecreator.actions.CCPluginToggleAction
import com.jetbrains.edu.learning.messages.EduCoreBundle
import icons.EducationalCoreIcons
import java.awt.Cursor
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants


class SelectRolePanel : JPanel() {
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
    CCPluginToggleAction.isCourseCreatorFeaturesEnabled = false
    myStudent.addMouseListener(object : MouseAdapter() {
      override fun mousePressed(e: MouseEvent?) {
        CCPluginToggleAction.isCourseCreatorFeaturesEnabled = false
        myStudent.icon = EducationalCoreIcons.StudentHover
        teacher.icon = EducationalCoreIcons.Teacher
      }

      override fun mouseEntered(e: MouseEvent?) {
        UIUtil.setCursor(myStudent, Cursor.getPredefinedCursor(Cursor.HAND_CURSOR))
      }

      override fun mouseExited(e: MouseEvent?) {
        UIUtil.setCursor(myStudent, Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR))
      }
    })
    studentPanel.add(myStudent)
    studentPanel.add(JLabel(EduCoreBundle.message("select.role.dialog.learner"), SwingConstants.CENTER))
    add(studentPanel)

    teacher.preferredSize = Dimension(iconSize, iconSize)
    val teacherPanel = JPanel(VerticalFlowLayout())
    teacherPanel.add(teacher)
    teacherPanel.add(JLabel(EduCoreBundle.message("select.role.dialog.educator"), SwingConstants.CENTER))
    teacher.addMouseListener(object : MouseAdapter() {
      override fun mousePressed(e: MouseEvent?) {
        CCPluginToggleAction.isCourseCreatorFeaturesEnabled = true
        myStudent.icon = EducationalCoreIcons.Student
        teacher.icon = EducationalCoreIcons.TeacherHover
      }

      override fun mouseEntered(e: MouseEvent?) {
        UIUtil.setCursor(teacher, Cursor.getPredefinedCursor(Cursor.HAND_CURSOR))
      }

      override fun mouseExited(e: MouseEvent?) {
        UIUtil.setCursor(teacher, Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR))
      }
    })
    add(teacherPanel)
  }

  fun getStudentButton(): JComponent = myStudent

}