package com.jetbrains.edu.android

import com.intellij.ide.fileTemplates.FileTemplateManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VfsUtilCore
import com.jetbrains.edu.android.AndroidCourseBuilder.Type.*
import com.jetbrains.edu.coursecreator.actions.NewStudyItemInfo
import com.jetbrains.edu.coursecreator.actions.NewStudyItemUiModel
import com.jetbrains.edu.coursecreator.actions.StudyItemType
import com.jetbrains.edu.coursecreator.ui.CCItemPositionPanel
import com.jetbrains.edu.coursecreator.ui.showNewStudyItemDialog
import com.jetbrains.edu.learning.LanguageSettings
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.FrameworkLesson
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.gradle.GradleCourseBuilderBase
import com.jetbrains.edu.learning.gradle.JdkProjectSettings
import com.jetbrains.edu.learning.gradle.generation.GradleCourseProjectGenerator
import com.jetbrains.edu.learning.kotlinPluginVersion
import org.jetbrains.plugins.gradle.util.GradleConstants

class AndroidCourseBuilder : GradleCourseBuilderBase() {

  override val buildGradleTemplateName: String = "android-build.gradle"

  override fun getCourseProjectGenerator(course: Course): GradleCourseProjectGenerator = AndroidCourseProjectGenerator(this, course)

  override fun templateVariables(project: Project): Map<String, String> {
    // TODO: extract suitable android gradle plugin version from android plugin
    return super.templateVariables(project) + mapOf("ANDROID_GRADLE_PLUGIN_VERSION" to "3.1.3",
                                                    "KOTLIN_VERSION" to kotlinPluginVersion())
  }

  override fun showNewStudyItemUi(
    project: Project,
    model: NewStudyItemUiModel,
    positionPanel: CCItemPositionPanel?
  ): NewStudyItemInfo? {
    val parentItem = model.parent
    return if (model.itemType != StudyItemType.TASK || parentItem is FrameworkLesson && parentItem.taskList.isNotEmpty()) {
      super.showNewStudyItemUi(project, model, positionPanel)
    } else {
      showNewStudyItemDialog(project, model, positionPanel, ::AndroidNewTaskDialog)
    }
  }

  override fun createInitialLesson(project: Project, course: Course): Lesson? = null

  override fun initNewTask(lesson: Lesson, task: Task, info: NewStudyItemInfo) {
    val packageName = info.getUserData(PACKAGE_NAME) ?: return
    val minAndroidSdk = info.getUserData(MIN_ANDROID_SDK) ?: return
    val compileAndroidSdk = info.getUserData(COMPILE_ANDROID_SDK) ?: return
    val attributes = mapOf(
      "PACKAGE_NAME" to packageName,
      "MIN_ANDROID_SDK" to minAndroidSdk.toString(),
      "COMPILE_ANDROID_SDK" to compileAndroidSdk.toString(),
      "TARGET_ANDROID_SDK" to compileAndroidSdk.toString()
    )

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

    val PACKAGE_NAME: Key<String> = Key("PACKAGE_NAME")
    val MIN_ANDROID_SDK: Key<Int> = Key("MIN_ANDROID_SDK")
    val COMPILE_ANDROID_SDK: Key<Int> = Key("COMPILE_ANDROID_SDK")

    private val LOG: Logger = Logger.getInstance(AndroidCourseBuilder::class.java)

    // TODO: reuse AS project template to create all default files
    private fun defaultAndroidCourseFiles(packageName: String): Map<String, FileInfo> {
      val packagePath = packageName.replace('.', VfsUtilCore.VFS_SEPARATOR_CHAR)
      return mapOf(
        "android-task-build.gradle" to FileInfo(GradleConstants.DEFAULT_SCRIPT_NAME, ADDITIONAL_FILE),
        "android-MainActivity.kt" to FileInfo("src/main/java/$packagePath/MainActivity.kt", TASK_FILE),
        "android-AndroidManifest.xml" to FileInfo("src/main/AndroidManifest.xml", TASK_FILE),
        "android-activity_main.xml" to FileInfo("src/main/res/layout/activity_main.xml", TASK_FILE),
        "android-styles.xml" to FileInfo("src/main/res/values/styles.xml", TASK_FILE),
        "android-strings.xml" to FileInfo("src/main/res/values/strings.xml", TASK_FILE),
        "android-colors.xml" to FileInfo("src/main/res/values/colors.xml", TASK_FILE),
        "android-ExampleUnitTest.kt" to FileInfo("src/test/java/$packagePath/ExampleUnitTest.kt", TEST_FILE),
        "android-AndroidEduTestRunner.kt" to FileInfo("src/androidTest/java/$packageName/AndroidEduTestRunner.kt", TEST_FILE)
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
