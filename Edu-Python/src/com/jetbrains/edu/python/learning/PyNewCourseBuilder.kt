package com.jetbrains.edu.python.learning

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.coursecreator.actions.NewStudyItemInfo
import com.jetbrains.edu.learning.EduCourseBuilder
import com.jetbrains.edu.learning.LanguageSettings
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator
import com.jetbrains.edu.python.learning.PyConfigurator.TASK_PY
import com.jetbrains.edu.python.learning.PyNewConfigurator.Companion.TEST_FILE_NAME
import com.jetbrains.edu.python.learning.PyNewConfigurator.Companion.TEST_FOLDER
import com.jetbrains.edu.python.learning.newproject.PyCourseProjectGenerator
import com.jetbrains.edu.python.learning.newproject.PyLanguageSettings
import com.jetbrains.python.PyNames
import com.jetbrains.python.newProject.PyNewProjectSettings

open class PyNewCourseBuilder : EduCourseBuilder<PyNewProjectSettings> {
  override fun getTaskTemplateName(): String = TASK_PY
  override fun getTestTemplateName(): String = TEST_FILE_NAME
  override fun getLanguageSettings(): LanguageSettings<PyNewProjectSettings> = PyLanguageSettings()

  override fun initNewTask(project: Project, lesson: Lesson, task: Task, info: NewStudyItemInfo) {
    if (task.taskFiles.isEmpty()) {
      super.initNewTask(project, lesson, task, info)
      task.addTaskFile(PyNames.INIT_DOT_PY)
      val testInitPy = TaskFile("$TEST_FOLDER/${PyNames.INIT_DOT_PY}", "")
      testInitPy.isVisible = false
      task.addTaskFile(testInitPy)
    }
  }

  override fun getCourseProjectGenerator(course: Course): CourseProjectGenerator<PyNewProjectSettings>? {
    return object : PyCourseProjectGenerator(this, course) {
      override fun createAdditionalFiles(project: Project, baseDir: VirtualFile) {}
    }
  }
}
