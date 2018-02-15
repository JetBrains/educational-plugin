package com.jetbrains.edu.learning.courseGeneration

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.ThrowableComputable
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.ThrowableRunnable
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.coursecreator.settings.CCSettings
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.RemoteCourse
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.ext.course
import com.jetbrains.edu.learning.courseFormat.ext.testTextMap
import com.jetbrains.edu.learning.courseFormat.tasks.CodeTask
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.intellij.EduIntellijUtils
import org.apache.commons.codec.binary.Base64
import java.io.IOException
import java.util.*
import java.util.concurrent.atomic.AtomicReference

object GeneratorUtils {

  @Throws(IOException::class)
  @JvmStatic
  fun createCourse(course: Course,
                   baseDir: VirtualFile,
                   indicator: ProgressIndicator) {
    indicator.isIndeterminate = false
    indicator.fraction = 0.0

    val lessons = course.getLessons(true)
    for ((i, lesson) in lessons.withIndex()) {
      indicator.fraction = (i + 1).toDouble() / lessons.size
      if (!lesson.isAdditional) {
        indicator.text = "Generating lesson ${i + 1} from ${lessons.size}"
      } else {
        indicator.text = "Generating additional files"
      }
      lesson.index = i + 1
      createLesson(lesson, baseDir)
    }
    course.removeAdditionalLesson()
  }

  @Throws(IOException::class)
  @JvmStatic
  fun createLesson(lesson: Lesson, courseDir: VirtualFile) {
    if (EduNames.ADDITIONAL_MATERIALS == lesson.name) {
      createAdditionalFiles(lesson, courseDir)
    } else {
      val lessonDirName = "${EduNames.LESSON}${lesson.index}"
      val lessonDir = runInWriteActionAndWait(ThrowableComputable {
        VfsUtil.createDirectoryIfMissing(courseDir, lessonDirName)
      })
      val taskList = lesson.getTaskList()
      for ((i, task) in taskList.withIndex()) {
        task.index = i + 1
        createTask(task, lessonDir)
      }
    }
  }

  @Throws(IOException::class)
  @JvmStatic
  fun createTask(task: Task, lessonDir: VirtualFile) {
    val name = "${EduNames.TASK}${task.index}"
    val taskDir = runInWriteActionAndWait(ThrowableComputable {
      VfsUtil.createDirectoryIfMissing(lessonDir, name)
    })
    createTaskContent(task, taskDir)
  }

  @Throws(IOException::class)
  private fun createTaskContent(task: Task, taskDir: VirtualFile) {
    var i = 0
    for ((_, taskFileContent) in task.getTaskFiles()) {
      taskFileContent.index = i
      i++
      createTaskFile(taskDir, taskFileContent)
    }
    createTestFiles(taskDir, task)
    val course = task.course
    if (course != null && CCUtils.COURSE_MODE == course.courseMode) {
      createDescriptionFiles(taskDir, task)
    }
  }

  @Throws(IOException::class)
  @JvmStatic
  fun createTaskFile(taskDir: VirtualFile, taskFile: TaskFile) {
    createChildFile(taskDir, taskFile.pathInTask, taskFile.text)
  }

  @Throws(IOException::class)
  @JvmStatic
  fun createTestFiles(taskDir: VirtualFile, task: Task) {
    val tests = task.testTextMap
    createFiles(taskDir, tests)
  }

  @Throws(IOException::class)
  @JvmStatic
  fun createDescriptionFiles(taskDir: VirtualFile, task: Task) {
    val taskTexts = task.taskTexts
    val renamedTaskTexts = taskTexts.mapKeys { entry ->
      entry.key + "." + FileUtilRt.getExtension(EduUtils.getTaskDescriptionFileName(CCSettings.getInstance().useHtmlAsDefaultTaskFormat()))
    }
    createFiles(taskDir, renamedTaskTexts)
  }

  @Throws(IOException::class)
  private fun createFiles(taskDir: VirtualFile, texts: Map<String, String>) {
    for ((name, value) in texts) {
      val virtualTaskFile = taskDir.findChild(name)
      if (virtualTaskFile == null) {
        createChildFile(taskDir, name, value)
      }
    }
  }

  @Throws(IOException::class)
  private fun createAdditionalFiles(lesson: Lesson, courseDir: VirtualFile) {
    val taskList = lesson.getTaskList()
    if (taskList.size != 1) return
    val task = taskList[0]

    val filesToCreate = HashMap(task.testsText)
    task.getTaskFiles().mapValuesTo(filesToCreate) { entry -> entry.value.text }
    filesToCreate.putAll(task.additionalFiles)

    for ((key, value) in filesToCreate) {
      createChildFile(courseDir, key, value)
    }
  }

  @Throws(IOException::class)
  @JvmStatic
  fun createChildFile(parentDir: VirtualFile, path: String, text: String) {
    runInWriteActionAndWait(ThrowableRunnable {
      var newDirectories: String? = null
      var fileName = path
      var dir: VirtualFile? = parentDir
      if (path.contains("/")) {
        val pos = path.lastIndexOf("/")
        fileName = path.substring(pos + 1)
        newDirectories = path.substring(0, pos)
      }
      if (newDirectories != null) {
        dir = VfsUtil.createDirectoryIfMissing(parentDir, newDirectories)
      }
      if (dir != null) {
        var virtualTaskFile = dir.findChild(fileName)
        if (virtualTaskFile == null) {
          virtualTaskFile = dir.createChildData(parentDir, fileName)
        }
        if (EduUtils.isImage(path)) {
          virtualTaskFile.setBinaryContent(Base64.decodeBase64(text))
        } else {
          VfsUtil.saveText(virtualTaskFile, text)
        }
      }
    })
  }

  @Throws(IOException::class)
  private fun runInWriteActionAndWait(action: ThrowableRunnable<IOException>) {
    runInWriteActionAndWait(ThrowableComputable {
      action.run()
    })
  }

  @Throws(IOException::class)
  private fun <T> runInWriteActionAndWait(action: ThrowableComputable<T, IOException>): T {
    val application = ApplicationManager.getApplication()
    val resultRef = AtomicReference<T>()
    val exceptionRef = AtomicReference<IOException>()
    application.invokeAndWait {
      application.runWriteAction {
        try {
          resultRef.set(action.compute())
        } catch (e: IOException) {
          exceptionRef.set(e)
        }
      }
    }
    return if (exceptionRef.get() != null) {
      throw IOException(exceptionRef.get())
    } else {
      resultRef.get()
    }
  }

  @JvmStatic
  fun initializeCourse(project: Project, course: Course) {
    course.initCourse(false)

    if (course.isAdaptive && !EduUtils.isCourseValid(course)) {
      Messages.showWarningDialog("There is no recommended tasks for this adaptive course",
                                 "Error in Course Creation")
    }
    if (updateTaskFilesNeeded(course)) {
      updateJavaCodeTaskFileNames(project, course)
    }
    StudyTaskManager.getInstance(project).course = course
  }

  private fun updateTaskFilesNeeded(course: Course): Boolean {
    return course is RemoteCourse && course.isStudy() && EduNames.JAVA == course.getLanguageID()
  }

  private fun updateJavaCodeTaskFileNames(project: Project, course: Course) {
    for (lesson in course.lessons) {
      for (task in lesson.getTaskList()) {
        if (task is CodeTask) {
          for (taskFile in task.getTaskFiles().values) {
            EduIntellijUtils.nameTaskFileAfterContainingClass(task, taskFile, project)
          }
        }
      }
    }
  }
}
