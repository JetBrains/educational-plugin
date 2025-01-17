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
    override val serviceURL = "https://lti-tool-staging.labs.jb.gg/"
  },

  /**
   * Implementation inside the Submission Service. Was historically the first implementation.
   * Key differences from [STANDALONE] implementation:
   *  - request to open some course has the parameter "task id" instead of "study item id".
   *  - when a task is solved, the results only for successful attempts should be sent, not to failed attempts.
   */
  ALPHA_TEST_2024 {
    override val serviceURL get() = SubmissionsServiceHost.getSelectedUrl()
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