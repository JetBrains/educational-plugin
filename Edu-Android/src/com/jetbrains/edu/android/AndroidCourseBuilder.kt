package com.jetbrains.edu.android

import com.android.tools.idea.sdk.IdeSdks
import com.intellij.ide.fileTemplates.FileTemplateManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VfsUtilCore
import com.jetbrains.edu.android.AndroidCourseBuilder.Type.*
import com.jetbrains.edu.coursecreator.actions.NewStudyItemInfo
import com.jetbrains.edu.learning.LanguageSettings
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.FrameworkLesson
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.gradle.GradleCourseBuilderBase
import com.jetbrains.edu.learning.gradle.JdkProjectSettings
import org.jetbrains.plugins.gradle.util.GradleConstants

class AndroidCourseBuilder : GradleCourseBuilderBase() {

  override val buildGradleTemplateName: String = "android-build.gradle"

  override val templates: Map<String, String>
    get() = super.templates + Pair("local.properties", "android-local.properties")

  override fun templateVariables(project: Project): Map<String, String> {
    // TODO: extract suitable android gradle plugin version from android plugin
    // TODO: use com.jetbrains.edu.kotlin.KtCourseBuilder.Companion#getKotlinPluginVersion
    return super.templateVariables(project) + mapOf("ANDROID_GRADLE_PLUGIN_VERSION" to "3.1.3",
                                                    "KOTLIN_VERSION" to "1.2.50",
                                                    "SDK_PATH" to (IdeSdks.getInstance().androidSdkPath?.absolutePath ?: ""))
  }

  override fun createInitialLesson(project: Project, course: Course): Lesson? = null

  override fun initNewTask(lesson: Lesson, task: Task, info: NewStudyItemInfo) {
    // TODO: show dialog and ask package name
    val itemName = if (lesson is FrameworkLesson) lesson.name else task.name
    val packageName = "com.edu.${FileUtil.sanitizeFileName(itemName)}"
    val attributes = mapOf("PACKAGE_NAME" to packageName)


    for ((templateName, fileInfo) in defaultAndroidCourseFiles(packageName)) {
      val template = FileTemplateManager.getDefaultInstance().findInternalTemplate(templateName)
      if (template == null) {
        LOG.warn("Failed to obtain internal template: `$templateName`")
        continue
      }
      val text = template.getText(attributes)
      when (fileInfo.type) {
        TASK_FILE -> {
          val taskFile = TaskFile()
          taskFile.name = fileInfo.path
          taskFile.setText(text)
          task.addTaskFile(taskFile)
        }
        TEST_FILE -> task.addTestsTexts(fileInfo.path, text)
        ADDITIONAL_FILE -> task.addAdditionalFile(fileInfo.path, text)
      }
    }
  }

  override fun getLanguageSettings(): LanguageSettings<JdkProjectSettings> = AndroidLanguageSettings()

  companion object {

    private val LOG: Logger = Logger.getInstance(AndroidCourseBuilder::class.java)

    // TODO: reuse AS project template to create all default files
    private fun defaultAndroidCourseFiles(packageName: String): Map<String, FileInfo> {
      val packagePath = packageName.replace('.', VfsUtilCore.VFS_SEPARATOR_CHAR)
      return mapOf(
        "android-task-build.gradle" to FileInfo(GradleConstants.DEFAULT_SCRIPT_NAME, ADDITIONAL_FILE),
        "android-MainActivity.kt" to FileInfo("java/$packagePath/MainActivity.kt", TASK_FILE),
        "android-AndroidManifest.xml" to FileInfo("AndroidManifest.xml", TASK_FILE),
        "android-activity_main.xml" to FileInfo("res/layout/activity_main.xml", TASK_FILE),
        "android-styles.xml" to FileInfo("res/values/styles.xml", TASK_FILE),
        "android-strings.xml" to FileInfo("res/values/strings.xml", TASK_FILE),
        "android-colors.xml" to FileInfo("res/values/colors.xml", TASK_FILE),
        "android-ExampleUnitTest.kt" to FileInfo("java/$packagePath/ExampleUnitTest.kt", TEST_FILE)
      )
    }
  }

  private data class FileInfo(val path: String, val type: Type)
  private enum class Type {
    TASK_FILE,
    TEST_FILE,
    ADDITIONAL_FILE
  }
}
