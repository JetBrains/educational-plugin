package com.jetbrains.edu.learning.marketplace

import com.intellij.openapi.diagnostic.Logger
import com.jetbrains.edu.learning.checkIsBackgroundThread
import org.jetbrains.annotations.VisibleForTesting
import java.io.IOException

// link and file name should not be changed for the backwards compatibility
@VisibleForTesting
const val MARKETPLACE_LISTED_COURSES_LINK = "https://raw.githubusercontent.com/JetBrains/educational-plugin/master/marketplaceListedCourses.txt"

object MarketplaceListedCoursesIdsLoader {
  private val LOG = Logger.getInstance(MarketplaceListedCoursesIdsLoader::class.java)

  private var coursesIds: List<CourseIds>

  val featuredCoursesIds: List<Int>
    get() = coursesIds.map { it.marketplaceId }

  init {
    checkIsBackgroundThread()
    val url = java.net.URL(MARKETPLACE_LISTED_COURSES_LINK)
    val listedCoursesText = try {
      url.readText()
    }
    catch (e: IOException) {
      val message = "Failed to retrieve content of '$MARKETPLACE_LISTED_COURSES_LINK'"
      LOG.warn(message, e)
      null
    }
    coursesIds = listedCoursesText?.lines()?.filter { it.isNotEmpty() }?.map {
      val parts = it.split("#")
      val marketplaceId = parts[0].trim().toInt()

      val stepikPart = parts[1].trim()
      val stepikId = if (stepikPart.isNotEmpty()) {
        stepikPart.toInt()
      }
      else {
        null
      }

      CourseIds(marketplaceId, stepikId)
    } ?: emptyList()
  }

  fun getMarketplaceIdByStepikId(stepikCourseId: Int): Int? = coursesIds.find { it.stepikId == stepikCourseId }?.marketplaceId

  fun isUploadedToMarketplace(stepikCourseId: Int): Boolean = getMarketplaceIdByStepikId(stepikCourseId) != null

  // corresponding ids for courses uploaded both to marketplace and Stepik, needed to
  // avoid courses duplication between marketplace and stepik tabs
  private class CourseIds(val marketplaceId: Int, val stepikId: Int?)
}