package com.jetbrains.edu.learning.stepik.alt

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.DumbAwareAction
import com.jetbrains.edu.learning.CoursesProvider
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.actions.ImportLocalCourseAction
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseLoading.CourseLoader
import com.jetbrains.edu.learning.newproject.ui.BrowseCoursesDialog
import com.jetbrains.edu.learning.stepik.alt.courseFormat.HyperskillCourse

class HyperskillProjectAction : DumbAwareAction("Start Hyperskill Project") {
  override fun actionPerformed(e: AnActionEvent) {
    val courses = CourseLoader.getCourseInfosUnderProgress {
      CoursesProvider.loadAllCourses(listOf(HyperskillProjectsProvider))
    } ?: return
    val dialog = BrowseCoursesDialog(courses, DefaultActionGroup(ImportHyperskillProject()))
    dialog.title = "Select Project"
    dialog.show()
  }
}

private object HyperskillProjectsProvider : CoursesProvider {
  override fun loadCourses(): List<Course> {
    val courses = mutableListOf<Course>()
    val projects = HyperskillConnector.getProjects() ?: return emptyList()

    for (project in projects) {
      val isKotlin = project.title.contains("Kotlin")   // TODO: get language properly
      if (!isKotlin) { // TODO: kotlin part
        val languageId = "Hyperskill-" + EduNames.JAVA
        val hyperskillCourse = HyperskillCourse(project.title, languageId)
        hyperskillCourse.id = project.id
        courses.add(hyperskillCourse)
      }
    }
    return courses
  }
}

class ImportHyperskillProject : ImportLocalCourseAction("Start Hyperskill Project") {

  override fun initCourse(course: Course) {
    super.initCourse(course)
    course.courseType = "Hyperskill"
  }

  override fun update(e: AnActionEvent) {
    e.presentation.icon = AllIcons.ToolbarDecorator.Import
    e.presentation.isEnabledAndVisible = true
  }
}