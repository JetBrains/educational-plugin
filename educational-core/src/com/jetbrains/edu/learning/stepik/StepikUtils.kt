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
import com.intellij.openapi.project.Project
import com.jetbrains.edu.coursecreator.stepik.CCStepikConnector
import com.jetbrains.edu.learning.courseFormat.RemoteCourse
import com.jetbrains.edu.learning.courseFormat.StepikChangeStatus
import com.jetbrains.edu.learning.courseFormat.ext.course
import com.jetbrains.edu.learning.courseFormat.tasks.CodeTask
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseFormat.tasks.TheoryTask
import java.util.regex.Pattern

object StepikUtils {

  private val LOG = Logger.getInstance(StepikUtils::class.java)
  private val PYCHARM_COURSE_TYPE = Pattern.compile(String.format("%s(\\d*) (\\w+)", StepikNames.PYCHARM_PREFIX))

  @JvmStatic
  fun wrapStepikTasks(task: Task, text: String, adaptive: Boolean): String {
    var finalText = text
    val course = task.course
    if (task is TheoryTask && course != null && course.isAdaptive) {
      finalText += "<br/><br/><b>Note</b>: This theory task aims to help you solve difficult tasks. "
    }
    else if (task is CodeTask && adaptive) {
      finalText += "<br/><br/><b>Note</b>: Use standard input to obtain input for the task."
    }
    if (course != null && course.isStudy) {
      finalText += getFooterWithLink(task, adaptive)
    }

    return finalText
  }

  private fun getFooterWithLink(task: Task, adaptive: Boolean): String {
    val link = if (adaptive) getAdaptiveLink(task) else getLink(task, task.index)
    return "<div class=\"footer\"><a href=$link>Leave a comment</a></div>"
  }

  private fun getLink(task: Task?, stepNumber: Int): String? {
    if (task == null) {
      return null
    }
    val lesson = task.lesson
    return if (lesson == null || lesson.course !is RemoteCourse) {
      null
    }
    else String.format("%s/lesson/%d/step/%d", StepikNames.STEPIK_URL, lesson.id, stepNumber)

  }

  private fun getAdaptiveLink(task: Task?): String? {
    val link = getLink(task, 1)
    return if (link == null) null else "$link?adaptive=true"
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
  fun getTopLevelSectionId(project: Project, course: RemoteCourse): Int {
    return if (!course.sectionIds.isEmpty()) {
      course.sectionIds[0]
    }
    else {
      val topLevelLesson = course.lessons.first { it.stepikChangeStatus == StepikChangeStatus.UP_TO_DATE }
      val id = CCStepikConnector.findTopLevelLessonsSection(project, topLevelLesson)
      if (id != -1) {
        return id
      }
      else {
        return CCStepikConnector.postTopLevelLessonsSection(project, course)
      }
    }
  }
}
