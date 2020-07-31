package com.jetbrains.edu.scala.stepik.hyperskill

import com.jetbrains.edu.jvm.JdkProjectSettings
import com.jetbrains.edu.jvm.gradle.GradleHyperskillConfigurator
import com.jetbrains.edu.scala.gradle.ScalaGradleConfigurator

class ScalaHyperskillConfigurator : GradleHyperskillConfigurator<JdkProjectSettings>(ScalaGradleConfigurator())
