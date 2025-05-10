package com.jetbrains.edu.learning

import com.intellij.ide.impl.OpenProjectTask
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.project.ex.ProjectManagerEx
import com.intellij.testFramework.runInEdtAndWait
import com.jetbrains.edu.coursecreator.yaml.createConfigFiles
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseGeneration.CourseGenerationTestBase
import com.jetbrains.edu.learning.newproject.EduProjectSettings

abstract class CourseReopeningTestBase<Settings : EduProjectSettings> : CourseGenerationTestBase<Settings>() {

  /**
   * The version of the [openStudentProjectThenReopenStudentProject(firstProjectOpened, secondProjectOpened)] method,
   * where both tests are [projectOpened].
   */
  protected fun openStudentProjectThenReopenStudentProject(course: Course, projectOpened: (Project) -> Unit) =
    openStudentProjectThenReopenStudentProject(course, projectOpened, projectOpened)

  /**
   * Simulates opening and then reopening a student project.
   * A new student project is created from the [course], and then opened.
   * [projectOpenedFirstTime] is the first set of tests to run on the opened project.
   * The student project is closed and then opened again.
   * [projectOpenedSecondTime] is the set of tests to run on the reopened project.
   */
  protected fun openStudentProjectThenReopenStudentProject(
    course: Course,
    projectOpenedFirstTime: (Project) -> Unit,
    projectOpenedSecondTime: (Project) -> Unit
  ) {
    createCourseStructure(course)
    createConfigFiles(project)

    projectOpenedFirstTime(project)
    reopenProject()
    projectOpenedSecondTime(project)
  }

  private fun reopenProject() {
    ProjectManager.getInstance().closeAndDispose(project)

    @Suppress("UnstableApiUsage")
    val reopenedProject = ProjectManagerEx.getInstanceEx().openProject(
      rootDir.toNioPath(),
      OpenProjectTask(forceOpenInNewFrame = true)
    ) ?: error("failed to reopen the project")

    waitForCourseConfiguration(reopenedProject)
    runInEdtAndWait {
      myProject = reopenedProject
    }
  }
}