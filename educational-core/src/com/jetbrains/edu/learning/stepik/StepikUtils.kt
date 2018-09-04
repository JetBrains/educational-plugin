/*
 * Copyright 2000-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
@file:JvmName("StepikUtils")

package com.jetbrains.edu.learning.stepik

import com.intellij.openapi.diagnostic.Logger
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.RemoteCourse
import com.jetbrains.edu.learning.courseFormat.StepikChangeStatus
import com.jetbrains.edu.learning.courseFormat.tasks.CodeTask
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseFormat.tasks.TheoryTask
import java.util.regex.Pattern

object StepikUtils {
  private val LOG = Logger.getInstance(StepikUtils::class.java)
  private val PYCHARM_COURSE_TYPE = Pattern.compile(String.format("%s(\\d*) (\\w+)", StepikNames.PYCHARM_PREFIX))

  @JvmStatic
  fun wrapStepikTasks(task: Task, text: String): String {
    var finalText = text
    val course = task.course
    if (course.isAdaptive) {
      when (task) {
        is TheoryTask -> finalText += "<br/><br/><b>Note</b>: This theory task aims to help you solve difficult tasks. "
        is CodeTask ->  finalText += "<br/><br/><b>Note</b>: Use standard input to obtain input for the task."
      }
    }
    return finalText
  }

  @JvmStatic
  fun setCourseLanguage(info: RemoteCourse) {
    val courseType = info.type
    val matcher = PYCHARM_COURSE_TYPE.matcher(courseType)
    if (matcher.matches()) {
      val language = matcher.group(2)
      info.language = language
    }
    else {
      LOG.info(String.format("Language for course `%s` with `%s` type can't be set because it isn't \"pycharm\" course",
                             info.name, courseType))
    }
  }

  @JvmStatic
  fun setStatusRecursively(course: Course, status: StepikChangeStatus) {
    course.visitLessons { lesson ->
      setLessonStatus(lesson, status)
      true
    }
    for (section in course.sections) {
      section.stepikChangeStatus = status
    }
  }

  private fun setLessonStatus(lesson: Lesson, status: StepikChangeStatus) {
    lesson.stepikChangeStatus = status
    for (task in lesson.taskList) {
      task.stepikChangeStatus = status
    }
  }
}
