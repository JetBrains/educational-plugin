package com.jetbrains.edu.csharp.hyperskill

import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillProject
import com.jetbrains.edu.learning.stepik.hyperskill.courseGeneration.HyperskillCourseCreator

class UnityHyperskillCourseCreator : HyperskillCourseCreator {
  override fun createHyperskillCourse(
    hyperskillProject: HyperskillProject,
    languageId: String,
    languageVersion: String?,
    eduEnvironment: String
  ): HyperskillCourse {
    val customContentPath = if (hyperskillProject.language == "unity") "Assets/Scripts/Editor" else ""
    return HyperskillCourse(hyperskillProject, languageId, languageVersion, eduEnvironment).apply {
      this.customContentPath = customContentPath
    }
  }
}