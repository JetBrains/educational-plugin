package com.jetbrains.edu.java

import com.intellij.openapi.projectRoots.JavaSdkVersion
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

  private fun String.toJavaSdkVersion() = JavaSdkVersion.values().find { it.description == this }


  companion object {
    private val DEFAULT_JAVA = JavaSdkVersion.JDK_1_8
  }
}