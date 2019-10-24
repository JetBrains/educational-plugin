package com.jetbrains.edu.python.learning

import com.intellij.openapi.project.Project
import com.jetbrains.edu.coursecreator.actions.NewStudyItemInfo
import com.jetbrains.edu.learning.EduCourseBuilder
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.python.learning.PyConfiguratorBase.TASK_PY
import com.jetbrains.edu.python.learning.PyNewConfiguratorBase.Companion.TEST_FILE_NAME
import com.jetbrains.edu.python.learning.PyNewConfiguratorBase.Companion.TEST_FOLDER
import com.jetbrains.python.PyNames
import com.jetbrains.python.newProject.PyNewProjectSettings

abstract class PyNewCourseBuilderBase : EduCourseBuilder<PyNewProjectSettings> {
  override val taskTemplateName: String = TASK_PY
  override val testTemplateName: String = TEST_FILE_NAME

  override fun initNewTask(project: Project, lesson: Lesson, task: Task, info: NewStudyItemInfo) {
    if (task.taskFiles.isEmpty()) {
      super.initNewTask(project, lesson, task, info)
      task.addTaskFile(PyNames.INIT_DOT_PY)
      val testInitPy = TaskFile("$TEST_FOLDER/${PyNames.INIT_DOT_PY}", "")
      testInitPy.isVisible = false
      task.addTaskFile(testInitPy)
    }
  }
}
