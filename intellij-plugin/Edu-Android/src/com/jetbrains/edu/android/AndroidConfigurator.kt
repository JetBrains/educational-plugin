package com.jetbrains.edu.android

import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.util.BuildNumber
import com.jetbrains.edu.EducationalCoreIcons
import com.jetbrains.edu.android.checker.AndroidTaskCheckerProvider
import com.jetbrains.edu.jvm.gradle.GradleConfiguratorBase
import com.jetbrains.edu.jvm.gradle.GradleCourseBuilderBase
import com.jetbrains.edu.learning.EduExperimentalFeatures
import com.jetbrains.edu.learning.checker.TaskCheckerProvider
import com.jetbrains.edu.learning.isFeatureEnabled
import com.jetbrains.edu.learning.isUnitTestMode
import javax.swing.Icon

// BACKCOMPAT: 2024.3. Drop it
private val BUILD_243_25659 = BuildNumber.fromString("243.25659")!!

class AndroidConfigurator : GradleConfiguratorBase() {
  override val courseBuilder: GradleCourseBuilderBase
    get() = AndroidCourseBuilder()

  override val taskCheckerProvider: TaskCheckerProvider
    get() = AndroidTaskCheckerProvider()

  override val sourceDir: String
    get() = "src/main"

  override val testDirs: List<String>
    get() = listOf("src/test", "src/androidTest")

  override val testFileName: String
    get() = "ExampleUnitTest.kt"

  // There are binary incompatibilities with AS before 2024.3.2 (243.25659)
  override val isEnabled: Boolean
    get() = ApplicationInfo.getInstance().build >= BUILD_243_25659

  override val isCourseCreatorEnabled: Boolean
    get() = isFeatureEnabled(EduExperimentalFeatures.ANDROID_COURSES) || isUnitTestMode

  override val logo: Icon
    get() = EducationalCoreIcons.Language.Android
}
