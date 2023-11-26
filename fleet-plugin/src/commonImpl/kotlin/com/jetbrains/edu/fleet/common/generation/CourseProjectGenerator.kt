package com.jetbrains.edu.fleet.common.generation

import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.TASK_MD
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.Section
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import fleet.api.*
import fleet.common.fs.fsService


class CourseProjectGenerator {

  suspend fun createCourse(baseDir: FileAddress, course: Course): Boolean {
    course.init(false)
    // TODO: store course somewhere

    val path = baseDir.path
    val fsApi = requireNotNull(fsService(baseDir)) { "There must be fs service for file $baseDir" }
    if (fsApi.exists(path)) {
      // TODO: show error
      return false
    }
    fsApi.createDirectoryAll(path).unwrap()  // TODO: show error if failed

    for (item in course.items) {
      if (item is Lesson) {
        createLesson(item, baseDir)
      }
      else if (item is Section) {
        createSection(item, baseDir)
      }
    }
    return true
  }

  suspend fun createSection(item: Section, baseDir: FileAddress): FileAddress {
    val sectionDir = createUniqueDir(baseDir, item.name)

    for (lesson in item.lessons) {
      createLesson(lesson, sectionDir)
    }
    return sectionDir
  }

  private suspend fun createUniqueDir(baseDir: FileAddress, itemName: String): FileAddress {
    val dir = baseDir.resolveChild(itemName)
    val fsApi = requireNotNull(fsService(baseDir)) { "There must be fs service for file $baseDir" }

    if (!fsApi.exists(dir.path)) {
      fsApi.createDirectoryAll(dir.path).unwrap()
    }
    return dir
  }

  private suspend fun createLesson(lesson: Lesson, baseDir: FileAddress) {
    val lessonDir = createUniqueDir(baseDir, lesson.name)
    for (task in lesson.taskList) {
      createTask(task, lessonDir)
    }
  }

  private suspend fun createTask(task: Task, baseDir: FileAddress) {
    val taskDir = createUniqueDir(baseDir, task.name)

    createTaskContent(task, taskDir)
    createDescriptionFile(taskDir, task)
  }

  private suspend fun createTaskContent(task: Task, taskDir: FileAddress) {
    for (file in task.taskFiles.values) {
      createChildFile(taskDir.resolveChild(file.name), file.text)
    }
  }

  private suspend fun createDescriptionFile(taskDir: FileAddress, task: Task): Boolean {
    return createChildFile(taskDir.child(TASK_MD), task.descriptionText)
  }

  suspend fun createChildFile(fileAddress: FileAddress, text: String): Boolean {
    val path = fileAddress.path
    val fsApi = requireNotNull(fsService(fileAddress)) { "There must be fs service for file $fileAddress" }

    if (!fsApi.exists(path)) {
      fsApi.createDirectoryAll(path.parent()!!).unwrap()
      fsApi.createFile(path, text.toByteArray())
    }
    return fsApi.exists(path)
  }
}
