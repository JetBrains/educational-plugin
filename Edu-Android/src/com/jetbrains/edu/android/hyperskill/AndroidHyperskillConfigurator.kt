package com.jetbrains.edu.android.hyperskill

import com.jetbrains.edu.android.AndroidConfigurator
import com.jetbrains.edu.jvm.JdkProjectSettings
import com.jetbrains.edu.learning.stepik.hyperskill.HyperskillConfigurator

class AndroidHyperskillConfigurator: HyperskillConfigurator<JdkProjectSettings>(AndroidConfigurator()) {
  override val testDirs: List<String>
    get() = listOf("src/test", "src/androidTest")

  override val isCourseCreatorEnabled: Boolean
    get() = true

  override fun getMockFileName(text: String): String = "Main.kt"
}