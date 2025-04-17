package com.jetbrains.edu.learning.socialMedia.x.api

import com.fasterxml.jackson.module.kotlin.readValue
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.socialMedia.x.XConnector
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

  private inline fun <reified T> doSerializationTest(@Language("JSON") rawData: String, expected: T) {
    val actual = XConnector.getInstance().objectMapper.readValue<T>(rawData)
    assertEquals(expected, actual)
  }

  private fun <T> doDeserializationTest(value: T, @Language("JSON") expected: String) {
    val actual = XConnector.getInstance().objectMapper.writeValueAsString(value)
    assertEquals(expected, actual)
  }
}
