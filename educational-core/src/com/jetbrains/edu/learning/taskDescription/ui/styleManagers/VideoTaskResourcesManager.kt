package com.jetbrains.edu.learning.taskDescription.ui.styleManagers

import com.google.gson.Gson
import com.jetbrains.edu.learning.JavaUILibrary.Companion.isJCEF
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.tasks.VideoTask
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.stepik.getStepikLink
import kotlinx.css.*
import kotlinx.css.properties.lh

/**
 * Provides resources and stylesheet to stepikVideo.html.ft
 */
class VideoTaskResourcesManager : TaskResourcesManager<VideoTask> {
  override val resources = mapOf(
    "video_style" to videoStylesheet(),
    "videojs-resolution-switcher" to "https://cdnjs.cloudflare.com/ajax/libs/videojs-resolution-switcher/0.4.2/videojs-resolution-switcher.min.js",
    "video.js" to "http://vjs.zencdn.net/7.6.5/video.js",
    "video-js.css" to "http://vjs.zencdn.net/7.6.5/video-js.css"
  )

  private fun getTaskResources(task: VideoTask, lesson: Lesson) = mapOf(
    "thumbnail" to task.thumbnail,
    "sources" to Gson().toJson(task.sources),
    "currentTime" to task.currentTime.toString(),
    "stepikLink" to getStepikLink(task, lesson)
  )

  override fun getText(task: VideoTask): String {
    val lesson = task.parent as Lesson
    return if (task.sources.isNotEmpty()) {
      GeneratorUtils.getInternalTemplateText(VIDEO_TEMPLATE, getTaskResources(task, lesson))
    }
    else {
      EduCoreBundle.message("stepik.view.video", getStepikLink(task, lesson))
    }
  }

  private fun videoStylesheet(): String {
    val styleManager = StyleManager()
    return CSSBuilder().apply {
      ".vjs-no-js" {
        fontFamily = styleManager.bodyFont
        fontSize = if (isJCEF()) styleManager.bodyFontSize.px else styleManager.bodyFontSize.pt
        lineHeight = styleManager.bodyLineHeight.px.lh
        color = styleManager.bodyColor
        backgroundColor = styleManager.bodyBackground
        textAlign = TextAlign.left
        paddingLeft = 0.px
        paddingTop = 0.px
      }
      ".video-cell" {
        display = Display.tableCell
        verticalAlign = VerticalAlign.middle
        overflow = Overflow.hidden
      }
      ".container" {
        width = 100.pct
        height = 100.pct
        display = Display.table
        backgroundColor = styleManager.bodyBackground
      }
      ".vjs-resolution-button .vjs-menu-button .vjs-icon-placeholder::before" {
        //insert gear character as content to resolution button
        content = QuotedString("\\f110")
        //without fontFamily specification gear is not rendered
        fontFamily = "VideoJS"
        //line height specification is needed for correct vertical position
        lineHeight = 1.67.em.lh
      }
      ".vjs-picture-in-picture-control" {
        display = Display.none
      }
      ".vjs-nofull .vjs-fullscreen-control" {
        display = Display.none
      }
    }.toString()
  }

  companion object {
    private const val VIDEO_TEMPLATE = "stepikVideo.html"
  }
}