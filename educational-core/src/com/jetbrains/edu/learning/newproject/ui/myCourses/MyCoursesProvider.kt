package com.jetbrains.edu.learning.newproject.ui.myCourses

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.KeyWithDefaultValue
import com.intellij.ui.ColorUtil
import com.intellij.ui.components.JBLabel
import com.intellij.ui.scale.JBUIScale
import com.jetbrains.edu.learning.CoursesStorage
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.newproject.ui.CoursesPanel
import com.jetbrains.edu.learning.newproject.ui.CoursesPlatformProvider
import com.jetbrains.edu.learning.newproject.ui.GRAY_COLOR
import com.jetbrains.edu.learning.newproject.ui.coursePanel.groups.CoursesGroup
import com.jetbrains.edu.learning.newproject.ui.coursePanel.groups.asList
import kotlinx.coroutines.CoroutineScope
import javax.swing.Icon

class MyCoursesProvider(private val disposable: Disposable) : CoursesPlatformProvider() {
  override val name: String get() = EduCoreBundle.message("course.dialog.my.courses")

  override val icon: Icon? = null

  override fun createPanel(scope: CoroutineScope): CoursesPanel = MyCoursesPanel(this, scope, disposable)

  override suspend fun loadCourses(): List<CoursesGroup> {
    return CoursesGroup(CoursesStorage.getInstance().state.courses).asList()
  }

  val additionalText: String
    get() {
      val courses = CoursesStorage.getInstance().state.courses
      val studyCourses = courses.filter { it.courseMode == EduNames.STUDY }
      val completedCourses = studyCourses.count { it.tasksTotal != 0 && it.tasksSolved == it.tasksTotal }
      val inProgressCourses = studyCourses.size - completedCourses

      val additionalText = if (completedCourses != 0) {
        EduCoreBundle.message("course.dialog.my.courses.additional.text.full", inProgressCourses, completedCourses)
      }
      else {
        EduCoreBundle.message("course.dialog.my.courses.additional.text", inProgressCourses)
      }
      val additionalTextFontSize = JBLabel().font.size - 2
      val style = "font-size: ${additionalTextFontSize}; color: #${ColorUtil.toHex(GRAY_COLOR)}; margin-top: ${JBUIScale.scale(4)}px"
      return """<div style="$style"}>$additionalText</span>"""
    }

  companion object {
    val IS_FROM_MY_COURSES = KeyWithDefaultValue.create<Boolean>("IS_FROM_MY_COURSES", false)
  }
}