package com.jetbrains.edu.android

import com.android.ide.common.repository.GradleCoordinate
import com.android.sdklib.SdkVersionInfo
import com.android.tools.adtui.device.FormFactor
import com.android.tools.idea.gradle.repositories.RepositoryUrlManager
import com.android.tools.idea.projectsystem.GoogleMavenArtifactId
import com.android.tools.idea.sdk.AndroidSdks
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VfsUtilCore
import com.jetbrains.edu.coursecreator.StudyItemType.TASK_TYPE
import com.jetbrains.edu.coursecreator.actions.TemplateFileInfo
import com.jetbrains.edu.coursecreator.actions.studyItem.CCCreateLesson
import com.jetbrains.edu.coursecreator.actions.studyItem.CCCreateTask
import com.jetbrains.edu.coursecreator.actions.studyItem.NewStudyItemInfo
import com.jetbrains.edu.coursecreator.actions.studyItem.NewStudyItemUiModel
import com.jetbrains.edu.jvm.JdkProjectSettings
import com.jetbrains.edu.jvm.gradle.GradleCourseBuilderBase
import com.jetbrains.edu.jvm.gradle.generation.GradleCourseProjectGenerator
import com.jetbrains.edu.learning.CourseInfoHolder
import com.jetbrains.edu.learning.LanguageSettings
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.LESSON
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.TASK
import com.jetbrains.edu.learning.courseFormat.FrameworkLesson
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.gradle.GradleConstants.BUILD_GRADLE
import com.jetbrains.edu.learning.isUnitTestMode
import com.jetbrains.edu.learning.kotlinVersion

class AndroidCourseBuilder : GradleCourseBuilderBase() {

  override val buildGradleTemplateName: String = "android-build.gradle"

  override fun getCourseProjectGenerator(course: Course): GradleCourseProjectGenerator = AndroidCourseProjectGenerator(this, course)

  override fun templateVariables(projectName: String): Map<String, Any> {
    val androidGradlePluginVersion = getLatestAndroidGradlePluginVersion()
    val kotlinVersion = kotlinVersion()
    return super.templateVariables(projectName) + mapOf(
      "ANDROID_GRADLE_PLUGIN_VERSION" to androidGradlePluginVersion,
      "KOTLIN_VERSION" to kotlinVersion.version,
    )
  }

  override fun showNewStudyItemUi(
    project: Project,
    course: Course,
    model: NewStudyItemUiModel,
    studyItemCreator: (NewStudyItemInfo) -> Unit
  ) {
    val parentItem = model.parent
    if (model.itemType != TASK_TYPE || parentItem is FrameworkLesson && parentItem.taskList.isNotEmpty()) {
      super.showNewStudyItemUi(project, course, model, studyItemCreator)
    }
    else {
      val studyItemCreatorWrapper = if (isUnitTestMode) {
        studyItemCreator
      }
      else {
        val androidStudyItemCreator: (NewStudyItemInfo) -> Unit = { info ->
          val fullInfo = AndroidNewTaskAfterPopupDialog(project, course, model, info).showAndGetResult()
          if (fullInfo != null) {
            studyItemCreator(fullInfo)
          }
        }
        androidStudyItemCreator
      }
      super.showNewStudyItemUi(project, course, model, studyItemCreatorWrapper)
    }
  }

  override fun createInitialLesson(holder: CourseInfoHolder<Course>): Lesson {
    val lessonInfo = NewStudyItemInfo(LESSON + 1, 1, ::FrameworkLesson)
    val lesson = CCCreateLesson().createAndInitItem(holder, null, lessonInfo)

    val taskInfo = NewStudyItemInfo(TASK + 1, 1, ::EduTask).apply {
      initAndroidProperties(SdkVersionInfo.HIGHEST_KNOWN_STABLE_API)
    }
    val task = CCCreateTask().createAndInitItem(holder, lesson, taskInfo)
    lesson.addTask(task)

    return lesson
  }

  // TODO: reuse AS project template to create all default files
  override fun getDefaultTaskTemplates(
    course: Course,
    info: NewStudyItemInfo,
    withSources: Boolean,
    withTests: Boolean
  ): List<TemplateFileInfo> {
    val packageName = info.getUserData(PACKAGE_NAME) ?: return emptyList()
    val packagePath = packageName.replace('.', VfsUtilCore.VFS_SEPARATOR_CHAR)

    val templates = mutableListOf<TemplateFileInfo>()

    if (withSources) {
      templates += TemplateFileInfo("android-task-build.gradle", BUILD_GRADLE, true)
      templates += TemplateFileInfo("android-MainActivity.kt", "src/main/java/$packagePath/MainActivity.kt", true)
      templates += TemplateFileInfo("android-AndroidManifest.xml", "src/main/AndroidManifest.xml", true)
      templates += TemplateFileInfo("android-activity_main.xml", "src/main/res/layout/activity_main.xml", true)
      templates += TemplateFileInfo("android-styles.xml", "src/main/res/values/styles.xml", true)
      templates += TemplateFileInfo("android-strings.xml", "src/main/res/values/strings.xml", true)
      templates += TemplateFileInfo("android-colors.xml", "src/main/res/values/colors.xml", true)
    }
    if (withTests) {
      templates += TemplateFileInfo("android-ExampleUnitTest.kt", "src/test/java/$packagePath/ExampleUnitTest.kt", false)
      templates += TemplateFileInfo("android-AndroidEduTestRunner.kt", "src/androidTest/java/$packagePath/AndroidEduTestRunner.kt", false)
      templates += TemplateFileInfo("android-ExampleInstrumentedTest.kt", "src/androidTest/java/$packagePath/ExampleInstrumentedTest.kt",
                                    false)
    }

    return templates
  }

  override fun extractInitializationParams(info: NewStudyItemInfo): Map<String, String> {
    val packageName = info.getUserData(PACKAGE_NAME) ?: return emptyMap()
    val minAndroidSdk = info.getUserData(MIN_ANDROID_SDK) ?: return emptyMap()
    val compileAndroidSdk = info.getUserData(COMPILE_ANDROID_SDK) ?: return emptyMap()

    return mapOf(
      "PACKAGE_NAME" to packageName,
      "MIN_ANDROID_SDK" to minAndroidSdk.toString(),
      "COMPILE_ANDROID_SDK" to compileAndroidSdk.toString(),
      "TARGET_ANDROID_SDK" to compileAndroidSdk.toString(),
      "ANDROIDX_CORE_VERSION" to getLibraryVersion("androidx.core", "core-ktx", "1.0.2"),
      "ANDROIDX_APP_COMPAT_VERSION" to getLibraryVersion(GoogleMavenArtifactId.ANDROIDX_APP_COMPAT_V7, "1.0.2"),
      "ANDROIDX_TEST_RUNNER_VERSION" to getLibraryVersion("androidx.test.ext", "junit", "1.1.1"),
      "ANDROIDX_ESPRESSO_CORE_VERSION" to getLibraryVersion(GoogleMavenArtifactId.ANDROIDX_ESPRESSO_CORE, "3.2.0"),
      "ANDROIDX_RULES_VERSION" to getLibraryVersion(GoogleMavenArtifactId.ANDROIDX_TEST_RULES, "1.1.0")
    )
  }

  private fun getLibraryVersion(mavenArtifactId: GoogleMavenArtifactId, defaultVersion: String): String {
    return getLibraryVersion(mavenArtifactId.mavenGroupId, mavenArtifactId.mavenArtifactId, defaultVersion)
  }

  private fun getLibraryVersion(groupId: String, artifactId: String, defaultVersion: String): String {
    val gradleCoordinate = GradleCoordinate(groupId, artifactId, "+")
    val sdkHandler = AndroidSdks.getInstance().tryToChooseSdkHandler()
    return RepositoryUrlManager.get().resolveDynamicCoordinateVersion(gradleCoordinate, null, sdkHandler) ?: defaultVersion
  }

  override fun getLanguageSettings(): LanguageSettings<JdkProjectSettings> = AndroidLanguageSettings()

  companion object {
    private val PACKAGE_NAME: Key<String> = Key("PACKAGE_NAME")
    private val MIN_ANDROID_SDK: Key<Int> = Key("MIN_ANDROID_SDK")
    private val COMPILE_ANDROID_SDK: Key<Int> = Key("COMPILE_ANDROID_SDK")

    const val DEFAULT_PACKAGE_NAME = "com.example.android.course"

    fun NewStudyItemInfo.initAndroidProperties(compileSdkVersion: Int,
                                               packageName: String = DEFAULT_PACKAGE_NAME,
                                               androidSdkVersion: Int? = null) {
      putUserData(PACKAGE_NAME, packageName)
      putUserData(MIN_ANDROID_SDK, androidSdkVersion ?: FormFactor.MOBILE.minOfflineApiLevel)
      putUserData(COMPILE_ANDROID_SDK, compileSdkVersion)
    }
  }
}
