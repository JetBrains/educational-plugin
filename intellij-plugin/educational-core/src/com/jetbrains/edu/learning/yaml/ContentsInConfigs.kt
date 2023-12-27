package com.jetbrains.edu.learning.yaml

import com.intellij.openapi.project.Project
import com.jetbrains.edu.coursecreator.persistentStorage.BinaryContentsFromCourseStorage
import com.jetbrains.edu.coursecreator.persistentStorage.ContentsFromCourseStorage
import com.jetbrains.edu.coursecreator.persistentStorage.PersistentCourseStorage
import com.jetbrains.edu.coursecreator.persistentStorage.TextualContentsFromCourseStorage
import com.jetbrains.edu.coursecreator.persistentStorage.UndeterminedContentsFromCourseStorage
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseFormat.BinaryContents
import com.jetbrains.edu.learning.courseFormat.EduFile
import com.jetbrains.edu.learning.courseFormat.FileContents
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.TextualContents
import com.jetbrains.edu.learning.courseFormat.UndeterminedContents
import com.jetbrains.edu.learning.courseFormat.ext.getPathInCourse
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.yaml.format.student.TakeFromStorageBinaryContents
import com.jetbrains.edu.learning.yaml.format.student.TakeFromStorageTextualContents

fun EduFile.store(storage: PersistentCourseStorage) {
  when (val currentContents = contents) {
    is ContentsFromCourseStorage -> {
      if (currentContents.storage == storage) return
    }
    TakeFromStorageBinaryContents -> {
      contents = BinaryContentsFromCourseStorage(storage, pathInStorage)
    }
    TakeFromStorageTextualContents -> {
      contents = TextualContentsFromCourseStorage(storage, pathInStorage)
    }
    else -> {
      contents = contents.store(storage, pathInStorage)
    }
  }
}

fun Task.storeEduFiles(project: Project) {
  val studyTaskManager = StudyTaskManager.getInstance(project)
  if (studyTaskManager.course?.isStudy != true) return

  for (taskFile in taskFiles.values) {
    taskFile.store(studyTaskManager.persistentCourseStorage)
  }
}

private val EduFile.pathInStorage: String
  get() = if (this is TaskFile) {
    task.getPathInCourse() + "/"
  }
  else {
    ""
  } + name

private fun FileContents.store(storage: PersistentCourseStorage, path: String): FileContents {
  return when (this) {
    is BinaryContents -> {
      storage.put(path, bytes)
      BinaryContentsFromCourseStorage(storage, path)
    }

    is TextualContents -> {
      storage.put(path, text)
      TextualContentsFromCourseStorage(storage, path)
    }

    is UndeterminedContents -> {
      storage.put(path, textualRepresentation)
      UndeterminedContentsFromCourseStorage(storage, path)
    }
  }
}