package com.jetbrains.edu.java

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.psi.JavaDirectoryService
import com.intellij.psi.PsiManager
import com.jetbrains.edu.java.JConfigurator.TEST_JAVA
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.tasks.TaskWithSubtasks
import com.jetbrains.edu.learning.intellij.GradleCourseBuilderBase
import com.jetbrains.edu.learning.intellij.generation.GradleCourseProjectGenerator

class JCourseBuilder : GradleCourseBuilderBase() {

  override val buildGradleTemplateName: String = JAVA_BUILD_GRADLE_TEMPLATE_NAME
  override val taskTemplateName: String = JConfigurator.TASK_JAVA
  override val testTemplateName: String = JConfigurator.TEST_JAVA

  override fun createTestsForNewSubtask(project: Project, task: TaskWithSubtasks) {
    val taskDir = task.getTaskDir(project) ?: return
    val prevSubtaskIndex = task.lastSubtaskIndex
    val taskPsiDir = PsiManager.getInstance(project).findDirectory(taskDir) ?: return
    val nextSubtaskIndex = prevSubtaskIndex + 1
    val nextSubtaskTestsClassName = getSubtaskTestsClassName(nextSubtaskIndex)
    JavaDirectoryService.getInstance().createClass(taskPsiDir, nextSubtaskTestsClassName)
  }

  override fun getCourseProjectGenerator(course: Course): GradleCourseProjectGenerator =
    GradleCourseProjectGenerator(this, course)

  companion object {

    private val JAVA_BUILD_GRADLE_TEMPLATE_NAME = "java-build.gradle"

    private fun getSubtaskTestsClassName(index: Int): String {
      return if (index == 0) TEST_JAVA else "${FileUtil.getNameWithoutExtension(TEST_JAVA)}${EduNames.SUBTASK_MARKER}$index"
    }
  }
}
