package com.jetbrains.edu.sql.jvm.gradle

import com.jetbrains.edu.jvm.JdkProjectSettings
import com.jetbrains.edu.jvm.gradle.GradleConfiguratorBase
import com.jetbrains.edu.jvm.gradle.checker.GradleTaskCheckerProvider
import com.jetbrains.edu.learning.checker.TaskCheckerProvider
import com.jetbrains.edu.sql.core.SqlConfiguratorBase

abstract class SqlGradleConfiguratorBase : GradleConfiguratorBase(), SqlConfiguratorBase<JdkProjectSettings> {
  override val taskCheckerProvider: TaskCheckerProvider
    get() = GradleTaskCheckerProvider()
}
