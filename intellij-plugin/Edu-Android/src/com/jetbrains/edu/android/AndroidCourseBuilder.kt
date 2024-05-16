package com.jetbrains.edu.android

import com.android.ide.common.gradle.Dependency
import com.android.ide.common.gradle.RichVersion
import com.android.ide.common.repository.GoogleMavenArtifactId
import com.android.sdklib.SdkVersionInfo
import com.android.tools.adtui.device.FormFactor
import com.android.tools.idea.gradle.plugin.AgpVersions
import com.android.tools.idea.gradle.repositories.RepositoryUrlManager
import com.android.tools.idea.sdk.AndroidSdks
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VfsUtilCore
import com.jetbrains.edu.android.AndroidCourseBuilder.Configuration.*
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

  override fun buildGradleTemplateName(course: Course): String = "android-build.gradle"
  override fun settingGradleTemplateName(course: Course): String = "android-settings.gradle"

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

  override fun createInitialLesson(holder: CourseInfoHolder<Course>, lessonProducer: () -> Lesson): Lesson {
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
      templates += TemplateFileInfo("android-strings.xml", "src/main/res/values/strings.xml", true)
      templates += TemplateFileInfo("android-colors.xml", "src/main/res/values/colors.xml", true)
      templates += TemplateFileInfo("android-themes.xml", "src/main/res/values/themes.xml", true)
      templates += TemplateFileInfo("android-themes-night.xml", "src/main/res/values-night/themes.xml", true)
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
      "DEPENDENCIES_BLOCK" to generateDependencyBlock()
    )
  }

  private fun getLibraryVersion(groupId: String, artifactId: String, defaultVersion: String): String {
    val dependency = Dependency(groupId, artifactId, RichVersion.parse("+"))
    val sdkHandler = AndroidSdks.getInstance().tryToChooseSdkHandler()
    return RepositoryUrlManager.get().resolveDependencyRichVersion(dependency, null, sdkHandler) ?: defaultVersion
  }

  private fun generateDependencyBlock(): String {
    return DEPENDENCIES.joinToString("\n", prefix = "dependencies {\n", postfix = "\n}") { dependency ->
      val (configuration, groupId, artifactId, defaultVersion) = dependency
      val version = getLibraryVersion(groupId, artifactId, defaultVersion)
      "    ${configuration.configurationName} '$groupId:$artifactId:$version'"
    }
  }

  private fun getLatestAndroidGradlePluginVersion(): String = AgpVersions.latestKnown.toString()

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
      putUserData(MIN_ANDROID_SDK, androidSdkVersion ?: FormFactor.MOBILE.defaultApi)
      putUserData(COMPILE_ANDROID_SDK, compileSdkVersion)
    }

    private val DEPENDENCIES: List<Dependency> = listOf(
      Dependency(IMPLEMENTATION, "androidx.core", "core-ktx", "1.7.0"),
      Dependency(IMPLEMENTATION, GoogleMavenArtifactId.ANDROIDX_APP_COMPAT_V7, "1.5.1"),
      Dependency(IMPLEMENTATION, GoogleMavenArtifactId.ANDROIDX_DESIGN, "1.7.0"),
      Dependency(IMPLEMENTATION, GoogleMavenArtifactId.CONSTRAINT_LAYOUT, "2.1.4"),
      Dependency(TEST_IMPLEMENTATION, "junit", "junit", "4.13.2"),
      Dependency(ANDROID_TEST_IMPLEMENTATION, GoogleMavenArtifactId.ANDROIDX_TEST_EXT_JUNIT, "1.1.4"),
      Dependency(ANDROID_TEST_IMPLEMENTATION, GoogleMavenArtifactId.ANDROIDX_ESPRESSO_CORE, "3.5.0"),
      Dependency(ANDROID_TEST_IMPLEMENTATION, GoogleMavenArtifactId.ANDROIDX_TEST_RULES, "1.1.0")
    )
  }

  private data class Dependency(
    val configuration: Configuration,
    val groupId: String,
    val artifactId: String,
    val defaultVersion: String
  ) {
    constructor(
      configuration: Configuration,
      mavenArtifactId: GoogleMavenArtifactId,
      defaultVersion: String
    ) : this(configuration, mavenArtifactId.mavenGroupId, mavenArtifactId.mavenArtifactId, defaultVersion)
  }

  private enum class Configuration(val configurationName: String) {
    IMPLEMENTATION("implementation"),
    TEST_IMPLEMENTATION("testImplementation"),
    ANDROID_TEST_IMPLEMENTATION("androidTestImplementation");
  }
}
