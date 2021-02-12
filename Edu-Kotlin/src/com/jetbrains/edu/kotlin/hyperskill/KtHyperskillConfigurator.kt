package com.jetbrains.edu.kotlin.hyperskill

import com.jetbrains.edu.jvm.JdkProjectSettings
import com.jetbrains.edu.kotlin.KtConfigurator
import com.jetbrains.edu.learning.stepik.hyperskill.HyperskillConfigurator

class KtHyperskillConfigurator : HyperskillConfigurator<JdkProjectSettings>(object : KtConfigurator() {
  override fun getMockFileName(text: String): String = MAIN_KT
})
