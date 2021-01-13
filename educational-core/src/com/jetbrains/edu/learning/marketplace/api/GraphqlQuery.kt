package com.jetbrains.edu.learning.marketplace.api


import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils

object GraphqlQuery {
  const val LOADING_STEP = 10

  fun search(offset: Int): String = GeneratorUtils.getInternalTemplateText("marketplace.qraphql.loadCourses.txt",
                                                                           mapOf("max" to LOADING_STEP, "offset" to offset))

  fun searchById(courseId: Int) = GeneratorUtils.getInternalTemplateText("marketplace.qraphql.loadCourseById.txt",
                                                                         mapOf("courseId" to courseId))

  /**
   * this query returns the List<UpdateBean>, which should contain a single value - latest update id bean, because in
   * marketplace.qraphql.courseUpdateId.txt.ft  parameter max is set to 1
   */
  fun lastUpdateId(courseId: Int) = GeneratorUtils.getInternalTemplateText("marketplace.qraphql.courseUpdateId.txt",
                                                                           mapOf("courseId" to courseId))

}