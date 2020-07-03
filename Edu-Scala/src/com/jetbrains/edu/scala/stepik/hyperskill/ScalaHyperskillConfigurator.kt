package com.jetbrains.edu.scala.stepik.hyperskill

import com.jetbrains.edu.jvm.JdkProjectSettings
import com.jetbrains.edu.learning.stepik.hyperskill.HyperskillConfigurator
import com.jetbrains.edu.scala.gradle.ScalaGradleConfigurator

class ScalaHyperskillConfigurator : HyperskillConfigurator<JdkProjectSettings>(ScalaGradleConfigurator())
