package com.jetbrains.edu.java

import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.projectRoots.JavaSdkVersion
import com.intellij.openapi.roots.ui.configuration.projectRoot.ProjectSdksModel
import com.jetbrains.edu.jvm.JavaVersionNotProvided
import com.jetbrains.edu.jvm.JavaVersionParseSuccess
import com.jetbrains.edu.jvm.JdkLanguageSettings
import com.jetbrains.edu.jvm.ParsedJavaVersion
import com.jetbrains.edu.jvm.minCCJdkVersion
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.CourseMode

open class JLanguageSettings : JdkLanguageSettings() {

  override fun setupProjectSdksModel(model: ProjectSdksModel) {
    val (jdkPath, sdk) = findBundledJdk(model) ?: return
    if (sdk == null) {
      model.addSdk(JavaSdk.getInstance(), jdkPath, null)
    }
  }

  override fun minJvmSdkVersion(course: Course): ParsedJavaVersion {
    if (course.courseMode == CourseMode.EDUCATOR) return JavaVersionParseSuccess(minCCJdkVersion)
    val javaVersionDescription = course.languageVersion ?: return JavaVersionNotProvided
    return ParsedJavaVersion.fromJavaSdkDescriptionString(javaVersionDescription)
  }

  companion object {
    val DEFAULT_JAVA = JavaSdkVersion.JDK_1_8
  }
}
