package com.jetbrains.edu.learning.marketplace.api


import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils

object GraphqlQuery {
  const val LOADING_STEP = 10

  fun search(offset: Int, searchPrivate: Boolean): String {
    val templateName = if (searchPrivate) {
      "marketplace.qraphql.loadPrivateCourses.txt"
    }
    else {
      "marketplace.qraphql.loadPublicCourses.txt"
    }
    return GeneratorUtils.getInternalTemplateText(templateName, mapOf<String, Any>("max" to LOADING_STEP, "offset" to offset))
  }

  fun searchById(courseId: Int, searchPrivate: Boolean) =
    if (searchPrivate) {
      GeneratorUtils.getInternalTemplateText("marketplace.qraphql.loadPrivateCourseById.txt",
                                             mapOf("courseId" to courseId))
    }
    else {
      GeneratorUtils.getInternalTemplateText("marketplace.qraphql.loadPublicCourseById.txt",
                                             mapOf("courseId" to courseId))
    }

  /**
   * this query returns the List<UpdateBean>, which should contain one value for each courseId in courseIds - the latest update id bean,
   * because in marketplace.qraphql.courseUpdatesById.txt.ft `collapseField: PLUGIN_ID` parameter is passed
   */
  fun lastUpdatesList(courseIds: List<Int>) = GeneratorUtils.getInternalTemplateText("marketplace.qraphql.courseUpdatesById.txt",
                                                                                     mapOf("courseIds" to courseIds))

}