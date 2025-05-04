package com.jetbrains.edu.socialMedia.x.api

import com.fasterxml.jackson.module.kotlin.readValue
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.socialMedia.x.XConnector
import org.intellij.lang.annotations.Language
import org.junit.Test

class XV2ApiSerializationTest : EduTestCase() {

  @Test
  fun `user lookup`() = doSerializationTest("""
    {
      "data": {
        "created_at": "2013-12-14T04:35:55Z",
        "id": "2244994945",
        "name": "X Dev",
        "protected": false,
        "username": "TwitterDev"
      }
    }
  """, XUserLookup(XUserData("TwitterDev", "X Dev")))

  @Test
  fun tweet() = doDeserializationTest(
    Tweet("Hello!", Media(listOf("1912475018862166016"))),
    """{"text":"Hello!","media":{"media_ids":["1912475018862166016"]}}"""
  )

  @Test
  fun `tweet response`() = doSerializationTest("""
    {
      "data" : {
        "edit_history_tweet_ids" : [
          "1912475036826448076"
        ],
        "id" : "1912475036826448076",
        "text" : "Hello!"
      }
    }    
  """, TweetResponse(TweetData("1912475036826448076", "Hello!")))

  @Test
  fun `media response after media upload initialization (INIT command)`() = doSerializationTest("""
    {
      "data" : {
        "id" : "1912475018862166016",
        "expires_after_secs" : 86400,
        "media_key" : "16_1912475018862166016"
      }
    }
  """, XMediaUploadResponse(
    XUploadData(
      id = "1912475018862166016",
      mediaKey = "16_1912475018862166016",
      expiresAfterSecs = 86400,
      processingInfo = null
    )
  ))

  @Test
  fun `media response after finishing media upload (FINALIZE command)`() = doSerializationTest("""
    {
      "data" : {
        "id" : "1912475018862166016",
        "media_key" : "16_1912475018862166016",
        "size" : 708679,
        "expires_after_secs" : 86400,
        "processing_info" : {
          "state" : "pending",
          "check_after_secs" : 1
        }
      }
    }
  """, XMediaUploadResponse(
    XUploadData(
      id = "1912475018862166016",
      mediaKey = "16_1912475018862166016",
      expiresAfterSecs = 86400,
      processingInfo = XProcessingInfo(
        state = PendingState.PENDING,
        checkAfterSecs = 1
      )
    )
  ))

  @Test
  fun `media response of checking media upload status`() = doSerializationTest("""
    {
      "data" : {
        "expires_after_secs" : 86398,
        "id" : "1912475018862166016",
        "media_key" : "16_1912475018862166016",
        "processing_info" : {
          "progress_percent" : 100,
          "state" : "succeeded"
        },
        "size" : 708679
      }
    }
  """, XMediaUploadResponse(
    XUploadData(
      id = "1912475018862166016",
      mediaKey = "16_1912475018862166016",
      expiresAfterSecs = 86398,
      processingInfo = XProcessingInfo(
        state = PendingState.SUCCEEDED,
        checkAfterSecs = 0
      )
    )
  ))

  private inline fun <reified T> doSerializationTest(@Language("JSON") rawData: String, expected: T) {
    val actual = XConnector.getInstance().objectMapper.readValue<T>(rawData)
    assertEquals(expected, actual)
  }

  private fun <T> doDeserializationTest(value: T, @Language("JSON") expected: String) {
    val actual = XConnector.getInstance().objectMapper.writeValueAsString(value)
    assertEquals(expected, actual)
  }
}
