package com.jetbrains.edu.learning.marketplace.lti

import com.jetbrains.edu.learning.marketplace.BaseMarketplaceRestService.Companion.STUDY_ITEM_ID
import com.jetbrains.edu.learning.marketplace.changeHost.SubmissionsServiceHost
import io.netty.handler.codec.http.QueryStringDecoder

/**
 * Items correspond to different implementations of the LTI server.
 * These implementations have different APIs.
 *
 */
enum class LTIOnlineService {

  /**
   * Implementation as a standalone service.
   */
  STANDALONE {
    override val serviceURL = getHost()
  },

  /**
   * Implementation inside the Submission Service. Was historically the first implementation.
   * Key differences from [STANDALONE] implementation:
   *  - request to open some course has the parameter "task id" instead of "study item id".
   *  - when a task is solved, the results only for successful attempts should be sent, not to failed attempts.
   */
  ALPHA_TEST_2024 {
    override val serviceURL get() = SubmissionsServiceHost.selectedHost.url
  };

  abstract val serviceURL: String

  companion object {
    /**
     * Detects which service made an LTI request.
     * Uses 'edu_study_item_id' parameter: it is absent in the implementation of alpha test.
     */
    fun detect(urlDecoder: QueryStringDecoder): LTIOnlineService = if (urlDecoder.parameters()[STUDY_ITEM_ID].isNullOrEmpty()) {
      ALPHA_TEST_2024
    }
    else {
      STANDALONE
    }
  }
}

private const val LTI_HOST_SYSTEM_PROPERTY = "edu.lti.service.host"
private const val LTI_PRODUCTION_HOST = "https://lti-tool-production.labs.jb.gg/"
private const val LTI_STAGING_HOST = "https://lti-tool-staging.labs.jb.gg/"

// TODO In EDU-7851 the logic of getting host will be rewritten.
// A user will be able to change it with UI the same way it is done for Submission Service URL
private fun getHost(): String {
  val urlString = System.getProperty(
    LTI_HOST_SYSTEM_PROPERTY,
    LTI_PRODUCTION_HOST
  )

  return when (urlString) {
    "production" -> LTI_PRODUCTION_HOST
    "staging" -> LTI_STAGING_HOST
    else -> urlString
  }
}