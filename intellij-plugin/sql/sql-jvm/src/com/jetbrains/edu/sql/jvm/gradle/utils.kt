package com.jetbrains.edu.sql.jvm.gradle

import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.ext.configurator

val Course.isSqlCourse: Boolean get() = configurator is SqlGradleConfigurator
