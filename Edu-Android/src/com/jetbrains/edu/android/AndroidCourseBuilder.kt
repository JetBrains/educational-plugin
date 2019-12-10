package com.jetbrains.edu.android

import com.android.ide.common.repository.GradleCoordinate
import com.android.tools.idea.projectsystem.GoogleMavenArtifactId
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VfsUtilCore
import com.jetbrains.edu.coursecreator.actions.NewStudyItemInfo
import com.jetbrains.edu.coursecreator.actions.NewStudyItemUiModel
import com.jetbrains.edu.coursecreator.actions.StudyItemType
import com.jetbrains.edu.coursecreator.actions.TemplateFileInfo
import com.jetbrains.edu.coursecreator.ui.AdditionalPanel
import com.jetbrains.edu.coursecreator.ui.showNewStudyItemDialog
import com.jetbrains.edu.jvm.JdkProjectSettings
import com.jetbrains.edu.jvm.gradle.GradleCourseBuilderBase
import com.jetbrains.edu.jvm.gradle.generation.GradleCourseProjectGenerator
import com.jetbrains.edu.learning.LanguageSettings
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.FrameworkLesson
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.gradle.GradleConstants.BUILD_GRADLE
import com.jetbrains.edu.learning.kotlinVersion

class AndroidCourseBuilder : GradleCourseBuilderBase() {

  override val buildGradleTemplateName: String = "android-build.gradle"

  override fun getCourseProjectGenerator(course: Course): GradleCourseProjectGenerator = AndroidCourseProjectGenerator(this, course)

  override fun templateVariables(project: Project): Map<String, Any> {
    val androidGradlePluginVersion = getLatestAndroidGradlePluginVersion()
    val kotlinVersion = kotlinVersion()
    return super.templateVariables(project) + mapOf(
      "ANDROID_GRADLE_PLUGIN_VERSION" to androidGradlePluginVersion,
      "KOTLIN_VERSION" to kotlinVersion.version,
      "NEED_KOTLIN_EAP_REPOSITORY" to !kotlinVersion.isRelease
    )
  }

  override fun showNewStudyItemUi(
    project: Project,
    course: Course,
    model: NewStudyItemUiModel,
    additionalPanels: List<AdditionalPanel>
  ): NewStudyItemInfo? {
    val parentItem = model.parent
    return if (model.itemType != StudyItemType.TASK || parentItem is FrameworkLesson && parentItem.taskList.isNotEmpty()) {
      super.showNewStudyItemUi(project, course, model, additionalPanels)
    } else {
      showNewStudyItemDialog(project, course, model, additionalPanels, ::AndroidNewTaskDialog)
    }
  }

  override fun createInitialLesson(project: Project, course: Course): Lesson? = null

  override fun initNewTask(project: Project, lesson: Lesson, task: Task, info: NewStudyItemInfo) {
    val packageName = info.getUserData(PACKAGE_NAME) ?: return
    val minAndroidSdk = info.getUserData(MIN_ANDROID_SDK) ?: return
    val compileAndroidSdk = info.getUserData(COMPILE_ANDROID_SDK) ?: return

    val attributes = mapOf(
      "PACKAGE_NAME" to packageName,
      "MIN_ANDROID_SDK" to minAndroidSdk.toString(),
      "COMPILE_ANDROID_SDK" to compileAndroidSdk.toString(),
      "TARGET_ANDROID_SDK" to compileAndroidSdk.toString(),
      "ANDROIDX_CORE_VERSION" to getLibraryVersion(project, "androidx.core", "core-ktx", "1.0.2"),
      "ANDROIDX_APP_COMPAT_VERSION" to getLibraryVersion(project, GoogleMavenArtifactId.ANDROIDX_APP_COMPAT_V7, "1.0.2"),
      "ANDROIDX_TEST_RUNNER_VERSION" to getLibraryVersion(project, "androidx.test.ext", "junit", "1.1.1"),
      "ANDROIDX_ESPRESSO_CORE_VERSION" to getLibraryVersion(project, GoogleMavenArtifactId.ANDROIDX_ESPRESSO_CORE, "3.2.0")
    )

    for (templateInfo in defaultAndroidCourseFiles(packageName)) {
      val taskFile = templateInfo.toTaskFile(attributes) ?: continue
      task.addTaskFile(taskFile)
    }
  }

  private fun getLibraryVersion(project: Project, mavenArtifactId: GoogleMavenArtifactId, defaultVersion: String): String {
    return getLibraryVersion(project, mavenArtifactId.mavenGroupId, mavenArtifactId.mavenArtifactId, defaultVersion)
  }

  private fun getLibraryVersion(project: Project, groupId: String, artifactId: String, defaultVersion: String): String {
    val gradleCoordinate = GradleCoordinate(groupId, artifactId, "+")
    return resolveDynamicCoordinateVersion(gradleCoordinate, project) ?: return defaultVersion
  }

  override fun getLanguageSettings(): LanguageSettings<JdkProjectSettings> = AndroidLanguageSettings()

  companion object {

    val PACKAGE_NAME: Key<String> = Key("PACKAGE_NAME")
    val MIN_ANDROID_SDK: Key<Int> = Key("MIN_ANDROID_SDK")
    val COMPILE_ANDROID_SDK: Key<Int> = Key("COMPILE_ANDROID_SDK")

    // TODO: reuse AS project template to create all default files
    private fun defaultAndroidCourseFiles(packageName: String): List<TemplateFileInfo> {
      val packagePath = packageName.replace('.', VfsUtilCore.VFS_SEPARATOR_CHAR)
      return listOf(
        TemplateFileInfo("android-task-build.gradle", BUILD_GRADLE, true),
        TemplateFileInfo("android-MainActivity.kt", "src/main/java/$packagePath/MainActivity.kt", true),
        TemplateFileInfo("android-AndroidManifest.xml", "src/main/AndroidManifest.xml", true),
        TemplateFileInfo("android-activity_main.xml", "src/main/res/layout/activity_main.xml", true),
        TemplateFileInfo("android-styles.xml", "src/main/res/values/styles.xml", true),
        TemplateFileInfo("android-strings.xml", "src/main/res/values/strings.xml", true),
        TemplateFileInfo("android-colors.xml", "src/main/res/values/colors.xml", true),
        TemplateFileInfo("android-ExampleUnitTest.kt", "src/test/java/$packagePath/ExampleUnitTest.kt", false),
        TemplateFileInfo("android-AndroidEduTestRunner.kt", "src/androidTest/java/$packagePath/AndroidEduTestRunner.kt", false)
      )
    }
  }
}
