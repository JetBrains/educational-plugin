package com.jetbrains.edu.go

import com.jetbrains.edu.learning.EduCourseBuilder
import com.jetbrains.edu.learning.checker.TaskCheckerProvider
import com.jetbrains.edu.learning.configuration.EduConfiguratorWithSubmissions

class GoConfigurator : EduConfiguratorWithSubmissions<GoProjectSettings>() {
  override fun getCourseBuilder(): EduCourseBuilder<GoProjectSettings> {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun getTestFileName(): String {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun getTaskCheckerProvider(): TaskCheckerProvider {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }
}
