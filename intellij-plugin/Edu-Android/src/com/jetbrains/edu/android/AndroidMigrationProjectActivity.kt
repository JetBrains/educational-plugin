package com.jetbrains.edu.android

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.writeAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.FrameworkLesson
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.ext.allTasks
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.courseFormat.ext.getDocument
import com.jetbrains.edu.learning.courseFormat.ext.getVirtualFile
import com.jetbrains.edu.learning.framework.FrameworkLessonManager
import com.jetbrains.edu.learning.gradle.GradleConstants.BUILD_GRADLE
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val TEST_RUNNER_REGEX: Regex = """testInstrumentationRunner ".*?.AndroidEduTestRunner"""".toRegex()

class AndroidMigrationProjectActivity : ProjectActivity {
  override suspend fun execute(project: Project) {
    val course = project.course ?: return
    if (course.configurator !is AndroidConfigurator) return

    if (!PropertiesComponent.getInstance(project).getBoolean(INSTRUMENTED_TEST_RUNNER_MIGRATION)) {
      migrateToDefaultTestRunner(project, course)
      PropertiesComponent.getInstance(project).setValue(INSTRUMENTED_TEST_RUNNER_MIGRATION, true)
    }
  }

  private suspend fun migrateToDefaultTestRunner(project: Project, course: Course) {
    for (task in course.allTasks) {
      task.taskFiles[BUILD_GRADLE]?.migrateBuildGradle(project)

      task.getTaskFileValues()
        .find { file -> file.name.endsWith("/AndroidEduTestRunner.kt") }
        ?.delete(project)
    }
  }

  private suspend fun TaskFile.migrateBuildGradle(project: Project) {
    performIfOnDisk {
      val document = getDocument(project) ?: return@performIfOnDisk
      val oldText = document.text
      val newText = oldText.withDefaultTestRunner()
      if (oldText != newText) {
        writeAction {
          document.setText(newText)
        }
        // Done in this way, not to have a conflict between in memory and VFS states,
        // i.e., we modify the memory state first and propagate the changes to VFS
        withContext(Dispatchers.EDT) {
          FileDocumentManager.getInstance().saveDocument(document)
        }
      }
    }

    performIfInFrameworkStorage(project) { state ->
      val oldText = state[name] ?: return@performIfInFrameworkStorage
      state[name] = oldText.withDefaultTestRunner()
    }
  }

  private suspend fun TaskFile.delete(project: Project) {
    performIfOnDisk {
      writeAction {
        getVirtualFile(project)?.delete(this)
      }
    }

    performIfInFrameworkStorage(project) { state ->
      state.remove(name)
    }

    writeAction {
      task.removeTaskFile(name)
    }
  }

  private suspend fun TaskFile.performIfOnDisk(action: suspend () -> Unit) {
    val lesson = task.lesson
    if (!task.course.isStudy || lesson !is FrameworkLesson || lesson.currentTask() == task) {
      action()
    }
  }

  private suspend fun TaskFile.performIfInFrameworkStorage(project: Project, action: suspend (MutableMap<String, String>) -> Unit) {
    val lesson = task.lesson
    // There isn't any reason to update the task state in Framework storage for the current task since
    // it will be overridden after navigation from this task
    if (task.course.isStudy && lesson is FrameworkLesson && lesson.currentTask() != task) {
      val frameworkLessonManager = FrameworkLessonManager.getInstance(project)
      val state = frameworkLessonManager.getTaskState(lesson, task).toMutableMap()
      action(state)
      frameworkLessonManager.saveExternalChanges(task, state)
    }
  }

  private fun String.withDefaultTestRunner(): String =
    replace(TEST_RUNNER_REGEX, """testInstrumentationRunner "androidx.test.runner.AndroidJUnitRunner"""")

  companion object {
    const val INSTRUMENTED_TEST_RUNNER_MIGRATION = "INSTRUMENTED_TEST_RUNNER_MIGRATION"
  }
}
