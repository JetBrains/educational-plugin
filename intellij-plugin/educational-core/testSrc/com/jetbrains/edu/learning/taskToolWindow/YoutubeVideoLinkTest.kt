package com.jetbrains.edu.learning.taskToolWindow

import com.jetbrains.edu.learning.EduTestCase
import org.junit.Test

class YoutubeVideoLinkTest : EduTestCase() {
  private val videoId = "0zM3nApSvMg"

  @Test
  fun `test get video id watch v=${videoId}`() = doTestGetYoutubeVideoId("https://www.youtube.com/watch?v=${videoId}")

  @Test
  fun `test get video id watch v=${videoId}&start`() = doTestGetYoutubeVideoId("https://www.youtube.com/watch?v=${videoId}&start=10")

  @Test
  fun `test get video id watch v=${videoId}&feature`() = doTestGetYoutubeVideoId(
    "http://www.youtube.com/watch?v=${videoId}&feature=feedrec_grec_index")

  @Test
  fun `test get video id youtu_be${videoId}`() = doTestGetYoutubeVideoId("https://youtu.be/${videoId}")

  @Test
  fun `test get video id embed${videoId}`() = doTestGetYoutubeVideoId("https://www.youtube.com/embed/${videoId}")

  @Test
  fun `test get video id embed${videoId}&start`() = doTestGetYoutubeVideoId("https://www.youtube.com/embed/${videoId}&start=10")

  @Test
  fun `test get video id embed${videoId}&feature`() = doTestGetYoutubeVideoId("https://www.youtube.com/embed/${videoId}&feature")

  @Test
  fun `test get video id v=${videoId}&feature`() = doTestGetYoutubeVideoId("http://youtube.com/?v=${videoId}&feature")

  @Test
  fun `test video tag substituted`() {
    val taskDescriptionWithVideoTag = """
      Some text
      <video width="320" height="240" controls>
        <source src="https://youtu.be/${videoId}" type="video/ogg">
      </video>
    """.trimIndent()
    doTest(taskDescriptionWithVideoTag)
  }

  @Test
  fun `test iframe tag substituted`() {
    val taskDescriptionWithIframeTag = """
      Some text
      <iframe width="560" height="315" src="https://www.youtube.com/embed/${videoId}" frameborder="0"></iframe>
    """.trimIndent()
    doTest(taskDescriptionWithIframeTag)
  }

  private fun doTestGetYoutubeVideoId(link: String) {
    val id = link.getYoutubeVideoId()
    assertTrue("Check containsYoutubeLink failed for link ${link}", link.containsYoutubeLink())
    assertEquals("Failed to get video id for link ${link}", videoId, id)
  }

  private fun doTest(taskDescriptionWithVideo: String) {
    val expectedTaskDescription = """
      <html>
       <head></head>
       <body>
        Some text <a href="http://www.youtube.com/watch?v=${videoId}"><img src="http://img.youtube.com/vi/${videoId}/0.jpg"></a>
       </body>
      </html>
    """.trimIndent()
    val substitutedText = processYoutubeLink(taskDescriptionWithVideo, 1)
    assertEquals(expectedTaskDescription, substitutedText)
  }
}