package com.jetbrains.edu.learning.storage

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.CourseSetListener
import com.jetbrains.edu.learning.courseFormat.Course

class LearningObjectsPersister(private val project: Project) : CourseSetListener {
  override fun courseSet(course: Course) {
    if (!course.isStudy) return

    val storageManager = LearningObjectsStorageManager.getInstance(project)

    storageManager.persistAllEduFiles(course)

    course.needWriteYamlText = storageManager.writeTextInYaml
  }
}