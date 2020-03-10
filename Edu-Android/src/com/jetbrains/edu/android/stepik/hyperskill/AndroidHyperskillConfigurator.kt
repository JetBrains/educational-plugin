package com.jetbrains.edu.android.stepik.hyperskill

import com.jetbrains.edu.android.AndroidConfigurator
import com.jetbrains.edu.jvm.JdkProjectSettings
import com.jetbrains.edu.learning.stepik.hyperskill.HyperskillConfigurator

class AndroidHyperskillConfigurator: HyperskillConfigurator<JdkProjectSettings>(AndroidConfigurator()) {
  override val testDirs: List<String>
    get() = listOf("src/stageTest", "src/androidTest")
}