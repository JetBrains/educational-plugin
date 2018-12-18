package com.jetbrains.edu.java

import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.projectRoots.JavaSdkVersion
import com.intellij.openapi.roots.ui.configuration.JdkComboBox
import com.intellij.openapi.roots.ui.configuration.projectRoot.ProjectSdksModel
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.gradle.JdkLanguageSettings

class JavaLanguageSettings : JdkLanguageSettings() {
  override fun getLanguageVersions(): List<String> = listOf(JavaSdkVersion.JDK_1_8, JavaSdkVersion.JDK_1_9,
                                                            JavaSdkVersion.JDK_10, JavaSdkVersion.JDK_11,
                                                            JavaSdkVersion.JDK_12).map { it.description }

  override fun validate(course: Course?): String? {
    if (course == null) {
      return null
    }
    val courseJavaVersionDescription = course.languageVersion ?: DEFAULT_JAVA.description
    val courseJavaVersion = courseJavaVersionDescription.toJavaSdkVersion()
                            ?: return "Unsupported Java versions: ${courseJavaVersionDescription}"

    val providedJavaVersion = myJdkSettings.jdkItem?.jdk?.versionString ?: return "No Java sdk"

    val javaSdkVersion = JavaSdkVersion.fromVersionString(providedJavaVersion)
                         ?: return "Failed to determine Java version"
    if (javaSdkVersion.isAtLeast(courseJavaVersion)) {
      return null
    }
    return "Java version should be at least ${courseJavaVersionDescription}"
  }

  override fun preselectJdk(course: Course, jdkComboBox: JdkComboBox, sdksModel: ProjectSdksModel) {
    val courseJavaVersion = (course.languageVersion ?: DEFAULT_JAVA.description).toJavaSdkVersion() ?: return
    val jdks = sdksModel.sdks.filter { it.sdkType == JavaSdk.getInstance() }
    val suitableJdk = jdks.find {
      JavaSdkVersion.fromVersionString(it.versionString ?: "")?.isAtLeast(courseJavaVersion) ?: false
    } ?: return
    jdkComboBox.selectedJdk = suitableJdk
  }

  private fun String.toJavaSdkVersion() = JavaSdkVersion.values().find { it.description == this }


  companion object {
    private val DEFAULT_JAVA = JavaSdkVersion.JDK_1_8
  }
}