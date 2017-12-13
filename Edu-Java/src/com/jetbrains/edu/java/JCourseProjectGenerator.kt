package com.jetbrains.edu.java

import com.jetbrains.edu.java.JConfigurator.TASK_JAVA
import com.jetbrains.edu.java.JConfigurator.TEST_JAVA
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.intellij.EduCourseBuilderBase
import com.jetbrains.edu.learning.intellij.generation.GradleCourseProjectGenerator

class JCourseProjectGenerator(courseBuilder: EduCourseBuilderBase, course: Course)
  : GradleCourseProjectGenerator(courseBuilder, course) {

  override fun initializeFirstTask(task: Task) {
    val taskFile = TaskFile()
    taskFile.task = task
    taskFile.name = TASK_JAVA
    taskFile.text = EduUtils.getTextFromInternalTemplate(TASK_JAVA)
    task.addTaskFile(taskFile)
    task.testsText.put(TEST_JAVA, EduUtils.getTextFromInternalTemplate(TEST_JAVA))
  }
}
