package com.jetbrains.edu.java

import com.jetbrains.edu.jvm.JavaVersionNotProvided
import com.jetbrains.edu.jvm.JavaVersionParseSuccess
import com.jetbrains.edu.jvm.ParsedJavaVersion
import com.jetbrains.edu.jvm.gradle.GradleBuildSystemSupport
import com.jetbrains.edu.jvm.environment.JdkLanguageEnvironmentCatalogProvider
import com.jetbrains.edu.jvm.minCCJdkVersion
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.CourseMode

class JLanguageEnvironmentCatalogProvider : JdkLanguageEnvironmentCatalogProvider(GradleBuildSystemSupport) {

  override fun Course.minJvmSdkVersion(): ParsedJavaVersion {
    if (courseMode == CourseMode.EDUCATOR) return JavaVersionParseSuccess(minCCJdkVersion)
    val javaVersionDescription = languageVersion ?: return JavaVersionNotProvided
    return ParsedJavaVersion.fromJavaSdkDescriptionString(javaVersionDescription)
  }
}
