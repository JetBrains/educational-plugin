package com.jetbrains.edu.java.hyperskill

import com.jetbrains.edu.java.JLanguageSettings
import com.jetbrains.edu.jvm.JavaVersionParseSuccess
import com.jetbrains.edu.jvm.ParsedJavaVersion
import com.jetbrains.edu.jvm.hyperskillJdkVersion
import com.jetbrains.edu.learning.courseFormat.Course

class JHyperskillLanguageSettings : JLanguageSettings() {
  override fun minJvmSdkVersion(course: Course): ParsedJavaVersion {
    return JavaVersionParseSuccess(hyperskillJdkVersion)
  }
}