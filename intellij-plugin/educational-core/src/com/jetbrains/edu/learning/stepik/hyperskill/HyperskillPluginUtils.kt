package com.jetbrains.edu.learning.stepik.hyperskill

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.project.Project

var Project.isHyperskillProject: Boolean
  get() = PropertiesComponent.getInstance(this).getBoolean(IS_HYPERSKILL_COURSE_PROPERTY)
  set(value) {
    PropertiesComponent.getInstance(this).setValue(IS_HYPERSKILL_COURSE_PROPERTY, value)
  }

private const val IS_HYPERSKILL_COURSE_PROPERTY: String = "edu.course.is.hyperskill"