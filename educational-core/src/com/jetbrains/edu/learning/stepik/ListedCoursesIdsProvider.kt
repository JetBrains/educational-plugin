package com.jetbrains.edu.learning.stepik

import com.intellij.openapi.diagnostic.Logger
import java.io.IOException


private const val LISTED_COURSES_LINK ="https://raw.githubusercontent.com/JetBrains/educational-plugin/master/listedCourses.txt"

object ListedCoursesIdsProvider {
  private val LOG = Logger.getInstance(ListedCoursesIdsProvider::class.java)
  val featuredCommunityCourses: List<Int>
  val inProgressCourses: List<Int>
  val featuredStepikCourses: Map<Int, MutableList<String>>

  init {
    val url = java.net.URL(LISTED_COURSES_LINK)
    val listedCoursesTexts: Map<CoursesListName, String> = try {
      val text = url.readText()
      val coursesListsChunks = text.split("##").filter { it.isNotEmpty() }
      coursesListsChunks.associate {
        val name = CoursesListName.valueOf(it.lines().first())
        name to it.substringAfter("$name\n").trimEnd()
      }
    }
    catch (e: IOException) {
      LOG.warn("Failed to retrieve content of '$LISTED_COURSES_LINK'", e)
      emptyMap()
    }

    featuredCommunityCourses = getCoursesIds(listedCoursesTexts[CoursesListName.Community])
    inProgressCourses = getCoursesIds(listedCoursesTexts[CoursesListName.InProgress])
    featuredStepikCourses = getCourseIdsWithLanguage(listedCoursesTexts[CoursesListName.Stepik])
  }

  private fun getCoursesIds(text: String?): List<Int> {
    text ?: return emptyList()
    return text.lines().map { it.split("#")[0].trim().toInt() }
  }

  private fun getCourseIdsWithLanguage(text: String?): Map<Int, MutableList<String>> {
    text ?: return emptyMap()
    val result = mutableMapOf<Int, MutableList<String>>()
    text.lines().forEach {
      val parts = it.split("#")
      val id = parts[0].trim().toInt()
      val language = parts[1].trim()

      if (result.containsKey(id)) {
        result[id]?.add(language)
      }
      else {
        result[id] = mutableListOf(language)
      }
    }
    return result
  }

  private enum class CoursesListName {
    Community, InProgress, Stepik
  }
}