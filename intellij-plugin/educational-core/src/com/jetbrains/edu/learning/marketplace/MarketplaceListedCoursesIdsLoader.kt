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

  val featuredCoursesIds: List<Int>

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
    featuredCoursesIds = listedCoursesText?.lines()?.filter { it.isNotEmpty() }?.map {
      it.split("#")[0].trim().toInt()
    } ?: emptyList()
  }
}
