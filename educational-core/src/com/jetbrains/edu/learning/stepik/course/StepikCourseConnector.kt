package com.jetbrains.edu.learning.stepik.course

import com.intellij.lang.Language
import com.intellij.openapi.diagnostic.Logger
import com.jetbrains.edu.learning.stepik.StepikConnector.getLessons
import com.jetbrains.edu.learning.stepik.StepikConnector.getStepSources
import com.jetbrains.edu.learning.stepik.StepikLanguages
import com.jetbrains.edu.learning.stepik.api.StepikNewConnector
import com.jetbrains.edu.learning.stepik.api.StepikNewConnector.getCourseInfo
import java.io.IOException
import java.net.MalformedURLException
import java.net.URL
import java.util.*

object StepikCourseConnector {
  private val LOG = Logger.getInstance(StepikCourseConnector::class.java.name)

  @JvmStatic
  fun getCourseIdFromLink(link: String): Int {
    try {
      val url = URL(link)
      val pathParts = url.path.split("/").dropLastWhile { it.isEmpty() }
      for (i in pathParts.indices) {
        val part = pathParts[i]
        if (part == "course" && i + 1 < pathParts.size) {
          return Integer.parseInt(pathParts[i + 1])
        }
      }
    }
    catch (e: MalformedURLException) {
      LOG.warn(e.message)
    }

    return -1
  }

  fun getCourseInfoByLink(link: String): StepikCourse? {
    val courseId: Int = try {
      Integer.parseInt(link)
    }
    catch (e: NumberFormatException) {
      getCourseIdFromLink(link)
    }

    if (courseId != -1) {
      val info = getCourseInfo(courseId, false)
      return stepikCourseFromRemote(info)
    }
    return null
  }

  fun getSupportedLanguages(remoteCourse: StepikCourse): List<Language> {
    val languages = ArrayList<Language>()
    try {
      val codeTemplates = getFirstCodeTemplates(remoteCourse)
      for (languageName in codeTemplates.keys) {
        val id = StepikLanguages.langOfName(languageName).id
        val language = Language.findLanguageByID(id)
        if (language != null) {
          languages.add(language)
        }
      }
    }
    catch (e: IOException) {
      LOG.warn(e.message)
    }

    return languages
  }

  private fun getFirstCodeTemplates(remoteCourse: StepikCourse): Map<String, String> {
    val unitsIds = StepikNewConnector.getUnitsIds(remoteCourse)
    val lessons = getLessons(unitsIds)
    for (lesson in lessons) {
      val allStepSources = getStepSources(lesson.steps, remoteCourse.languageID)

      for (stepSource in allStepSources) {
        val step = stepSource.block
        if (step != null && step.name == "code" && step.options != null) {
          val codeTemplates = step.options.codeTemplates
          if (codeTemplates != null) {
            return codeTemplates
          }
        }
      }
    }
    return emptyMap()
  }
}
