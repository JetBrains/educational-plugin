package com.jetbrains.edu.learning.courseView

import com.intellij.ide.projectView.ProjectView
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.testFramework.ProjectViewTestUtil
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseGeneration.CourseGenerationTestBase
import com.jetbrains.edu.learning.navigation.NavigationUtils
import com.jetbrains.edu.learning.newproject.EmptyProjectSettings
import com.jetbrains.edu.learning.projectView.CourseViewPane

abstract class CourseViewHeavyTestBase : CourseGenerationTestBase<EmptyProjectSettings>() {

  override val defaultSettings: EmptyProjectSettings = EmptyProjectSettings

  protected fun createCourseAndChangeView(course: Course, openFirstTask: Boolean = true): ProjectView {
    createCourseStructure(course)

    // can't do it in setUp because project is not opened at that point
    ProjectViewTestUtil.setupImpl(project, true)

    return ProjectView.getInstance(project).apply {
      refresh()
      changeView(CourseViewPane.ID)
      if (openFirstTask) {
        NavigationUtils.openFirstTask(course, project)
      }
      PlatformTestUtil.waitWhileBusy(currentProjectViewPane.tree)
    }
  }
}
