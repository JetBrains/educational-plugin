package com.jetbrains.edu.coursecreator.ui

import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.util.NlsContexts.Button
import com.intellij.openapi.util.NlsContexts.DialogTitle
import com.intellij.ui.JBCardLayout
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.FrameworkLesson
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.messages.EduCoreBundle
import java.awt.event.ActionEvent
import javax.swing.AbstractAction
import javax.swing.Action
import javax.swing.JComponent
import javax.swing.JPanel

class CCNewCourseDialog @Suppress("UnstableApiUsage") constructor(
  @DialogTitle title: String,
  @Button okButtonText: String,
  course: Course? = null,
  courseProducer: () -> Course = ::EduCourse,
  private val onOKAction: () -> Unit = {}
) : DialogWrapper(true) {
  private val newCoursePanel: CCNewCoursePanel = CCNewCoursePanel(disposable, course, courseProducer)
  private val lessonTypeSelectionPanel: LessonTypeSelectionPanel = LessonTypeSelectionPanel(disposable, newCoursePanel)

  private val panel = CCNewCourseCardPanel()

  private val nextAction = object : AbstractAction(EduCoreBundle.message("cc.new.course.button.next")) {
    override fun actionPerformed(e: ActionEvent?) {
      prepareLessonSelectionPanel()
    }
  }

  private val backAction = object : AbstractAction(EduCoreBundle.message("cc.new.course.button.back")) {
    override fun actionPerformed(e: ActionEvent?) {
      prepareCreateCoursePanel()
    }
  }

  init {
    setTitle(title)
    setOKButtonText(okButtonText)
    init()
    prepareCreateCoursePanel()
    newCoursePanel.setValidationListener(object : CCNewCoursePanel.ValidationListener {
      override fun onInputDataValidated(isInputDataComplete: Boolean) {
        getButton(nextAction)?.isEnabled = isInputDataComplete
      }
    })
  }

  override fun createCenterPanel(): JComponent = panel

  override fun doOKAction() {
    val validationResult = lessonTypeSelectionPanel.validateAll()
    if (validationResult.isNotEmpty()) return

    val isFrameworkLessonSelected = lessonTypeSelectionPanel.isFrameworkLessonSelected ?: return
    close(OK_EXIT_CODE)
    onOKAction()
    val course = newCoursePanel.course
    val projectSettings = newCoursePanel.projectSettings
    val location = newCoursePanel.locationString
    val lessonProducer = if (isFrameworkLessonSelected) ::FrameworkLesson else ::Lesson
    course.configurator
      ?.courseBuilder
      ?.getCourseProjectGenerator(course)
      ?.doCreateCourseProject(location, projectSettings, lessonProducer)
  }

  override fun createActions(): Array<Action> = arrayOf(backAction, nextAction, okAction)

  override fun createLeftSideActions(): Array<Action> {
    return arrayOf(cancelAction)
  }

  private fun prepareCreateCoursePanel() {
    getButton(backAction)?.isVisible = false
    getButton(okAction)?.isVisible = false
    getButton(nextAction)?.isVisible = true
    rootPane.defaultButton = getButton(nextAction)
    panel.showCoursePanel()
  }

  private fun prepareLessonSelectionPanel() {
    val validationInfos = newCoursePanel.validateAll()
    if (validationInfos.isNotEmpty()) return
    newCoursePanel.validateLocation()
    if (getButton(nextAction)?.isEnabled != true) {
      return
    }

    getButton(backAction)?.isVisible = true
    getButton(okAction)?.isVisible = true
    getButton(nextAction)?.isVisible = false
    rootPane.defaultButton = getButton(okAction)
    panel.showLessonSelectionPanel()
  }

  inner class CCNewCourseCardPanel : JPanel() {
    private val courseCardName = "NEW_COURSE"
    private val lessonSelectionCardName = "LESSON_SELECTION"

    private val cardLayout = JBCardLayout()

    init {
      layout = cardLayout
      add(courseCardName, newCoursePanel)
      add(lessonSelectionCardName, lessonTypeSelectionPanel)
    }

    fun showCoursePanel() {
      cardLayout.show(this, courseCardName)
    }

    fun showLessonSelectionPanel() {
      cardLayout.show(this, lessonSelectionCardName)
    }
  }
}