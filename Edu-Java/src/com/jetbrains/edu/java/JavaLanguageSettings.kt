package com.jetbrains.edu.java

import com.intellij.openapi.projectRoots.JavaSdkVersion
import com.jetbrains.edu.learning.gradle.JdkLanguageSettings

class JavaLanguageSettings : JdkLanguageSettings() {
  override fun getLanguageVersions(): List<String> = listOf(JavaSdkVersion.JDK_1_8, JavaSdkVersion.JDK_1_9,
                                                            JavaSdkVersion.JDK_10, JavaSdkVersion.JDK_11,
                                                            JavaSdkVersion.JDK_12).map { it.description }
}