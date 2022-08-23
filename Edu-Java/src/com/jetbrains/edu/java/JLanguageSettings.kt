package com.jetbrains.edu.java

import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.projectRoots.JavaSdkVersion
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.ui.configuration.JdkComboBox
import com.intellij.openapi.roots.ui.configuration.projectRoot.ProjectSdksModel
import com.jetbrains.edu.java.messages.EduJavaBundle
import com.jetbrains.edu.jvm.JdkLanguageSettings
import com.jetbrains.edu.learning.EduNames.ENVIRONMENT_CONFIGURATION_LINK_JAVA
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.newproject.ui.ValidationMessage

class JLanguageSettings : JdkLanguageSettings() {

  override fun setupProjectSdksModel(model: ProjectSdksModel) {
    val (jdkPath, sdk) = findBundledJdk(model) ?: return
    if (sdk == null) {
      model.addSdk(JavaSdk.getInstance(), jdkPath, null)
    }
  }

  override fun validate(course: Course?, courseLocation: String?): ValidationMessage? {
    return if (course != null) {
      val courseJavaVersionDescription = course.languageVersion ?: DEFAULT_JAVA.description
      val courseJavaVersion = courseJavaVersionDescription.toJavaSdkVersion()
                              ?: return ValidationMessage(
                                EduJavaBundle.message("error.unsupported.java.version", courseJavaVersionDescription),
                                ENVIRONMENT_CONFIGURATION_LINK_JAVA)

      val providedJavaVersion = jdk?.versionString ?: return ValidationMessage(EduJavaBundle.message("error.no.jdk"),
                                                                                    ENVIRONMENT_CONFIGURATION_LINK_JAVA)

      val javaSdkVersion = JavaSdkVersion.fromVersionString(providedJavaVersion)
                           ?: return ValidationMessage(EduJavaBundle.message("failed.determine.java.version"),
                                                       ENVIRONMENT_CONFIGURATION_LINK_JAVA)
      if (javaSdkVersion.isAtLeast(courseJavaVersion)) {
        return null
      }
      return ValidationMessage(
        EduJavaBundle.message("error.old.java", courseJavaVersionDescription),
        "https://www.oracle.com/technetwork/java/javase/downloads/index.html"
      )
    }
    else super.validate(null, courseLocation)
  }

  override fun preselectJdk(course: Course, jdkComboBox: JdkComboBox, sdksModel: ProjectSdksModel) {
    val suitableJdk = findSuitableJdk(course, sdksModel) ?: return
    jdkComboBox.selectedJdk = suitableJdk
  }

  companion object {
    val DEFAULT_JAVA = JavaSdkVersion.JDK_1_8

    @JvmStatic
    fun findSuitableJdk(course: Course, sdkModel: ProjectSdksModel): Sdk? {
      val courseJavaVersion = (course.languageVersion ?: DEFAULT_JAVA.description).toJavaSdkVersion() ?: return null
      val jdks = sdkModel.sdks.filter { it.sdkType == JavaSdk.getInstance() }
      return jdks.find { JavaSdkVersion.fromVersionString(it.versionString ?: "")?.isAtLeast(courseJavaVersion) ?: false }
    }

    private fun String.toJavaSdkVersion() = JavaSdkVersion.values().find { it.description == this }
  }
}
