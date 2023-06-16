package com.jetbrains.edu.go.stepik.hyperskill

import com.jetbrains.edu.go.GoConfigurator
import com.jetbrains.edu.go.GoConfigurator.Companion.MAIN_GO
import com.jetbrains.edu.go.GoProjectSettings
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.stepik.hyperskill.HyperskillConfigurator

class GoHyperskillConfigurator : HyperskillConfigurator<GoProjectSettings>(GoConfigurator()) {
  override fun getMockFileName(course: Course, text: String): String = MAIN_GO
}