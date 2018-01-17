package com.jetbrains.edu.coursecreator.configuration

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.ext.course
import com.jetbrains.edu.learning.courseFormat.tasks.Task

object CourseChangeHandler {
  fun placeholderChanged(placeholder: AnswerPlaceholder) {
    val taskFile = placeholder.taskFile ?: return
    taskFileChanged(taskFile)
  }

  fun taskFileChanged(taskFile: TaskFile) {
    val task = taskFile.task
    val project = taskFile.task.lesson.course.getProject()
    project ?: return
    ApplicationManager.getApplication().runWriteAction {
      CourseInfoSynchronizer.saveTask(task.getTaskDir(project), task)
    }
  }

  fun taskChanged(task: Task) {
    val project = task.course?.getProject()
    project ?: return
    CourseInfoSynchronizer.saveTask(task.getTaskDir(project), task)
  }

  fun lessonChanged(lesson: Lesson) {
    val project = lesson.course.getProject() ?: return
    val lessonDir = project.baseDir.findChild(EduNames.LESSON + lesson.index) ?: return
    CourseInfoSynchronizer.saveLesson(lessonDir, lesson)
  }

  fun courseChanged(course: Course) {
    CourseInfoSynchronizer.saveCourse(course.getProject()?:return)
  }

  private fun Course.getProject(): Project? {
    return ProjectManager.getInstance().openProjects.firstOrNull { StudyTaskManager.getInstance(it).course == this }
  }
}