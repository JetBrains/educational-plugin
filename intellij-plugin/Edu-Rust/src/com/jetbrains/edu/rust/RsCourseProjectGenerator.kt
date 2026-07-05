package com.jetbrains.edu.rust

import com.jetbrains.edu.learning.CourseInfoHolder
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.EduFile
import com.jetbrains.edu.learning.courseFormat.ext.getDir
import com.jetbrains.edu.learning.courseFormat.ext.pathInCourse
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator
import com.jetbrains.edu.rust.environment.RsLanguageEnvironment

class RsCourseProjectGenerator(builder: RsCourseBuilder, course: Course) :
  CourseProjectGenerator<RsLanguageEnvironment>(builder, course) {

  override fun autoCreatedAdditionalFiles(holder: CourseInfoHolder<Course>): List<EduFile> {
    val members = mutableListOf<String>()
    holder.course.visitLessons { lesson ->
      val lessonDir = lesson.getDir(holder.courseDir) ?: return@visitLessons
      val lessonDirPath = lessonDir.pathInCourse(holder) ?: return@visitLessons
      members += "    \"${lessonDirPath}/*/\""
    }

    val initialMembers = members.joinToString(",\n", postfix = if (members.isEmpty()) "" else ",")

    return listOf(
      GeneratorUtils.createFromInternalTemplateOrFromDisk(
        holder.courseDir,
        "Cargo.toml",
        "workspaceCargo.toml",
        mapOf(INITIAL_MEMBERS to initialMembers)
      )
    )
  }

  companion object {
    private const val INITIAL_MEMBERS = "INITIAL_MEMBERS"
  }
}
