package com.jetbrains.edu.learning.taskDescription.ui.styleManagers

import com.google.gson.Gson
import com.jetbrains.edu.learning.EduSettings
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.tasks.VideoTask
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.stepik.getStepikLink
import kotlinx.css.*
import kotlinx.css.properties.lh

/**
 * Provides resources and stylesheet to stepikVideo.html.ft
 */
class VideoTaskResourcesManager {
  private val VIDEO_TEMPLATE = "stepikVideo.html"

  val videoResources = mapOf(
    "video_style" to videoStylesheet(),
    "videojs-resolution-switcher" to "https://cdnjs.cloudflare.com/ajax/libs/videojs-resolution-switcher/0.4.2/videojs-resolution-switcher.min.js",
    "videojs-resolution-switcher.css" to "https://cdnjs.cloudflare.com/ajax/libs/videojs-resolution-switcher/0.4.2/videojs-resolution-switcher.min.css",
    "video.js" to "http://vjs.zencdn.net/5.16.0/video.js",
    "video-js.css" to "http://vjs.zencdn.net/5.16.0/video-js.css"
  )

  private fun getResources(task: VideoTask, lesson: Lesson) = mapOf(
    "thumbnail" to task.thumbnail,
    "sources" to Gson().toJson(task.sources),
    "currentTime" to task.currentTime.toString(),
    "stepikLink" to getStepikLink(task, lesson)
  )

  fun getText(task: VideoTask, lesson: Lesson): String = if (task.sources.isNotEmpty()) {
    GeneratorUtils.getInternalTemplateText(VIDEO_TEMPLATE, getResources(task, lesson))
    }
    else {
      "View this video on <a href=" + getStepikLink(task, lesson) + ">Stepik</a>."
    }

  private fun videoStylesheet(): String {
    val styleManager = StyleManager()
    return CSSBuilder().apply {
      ".vjs-no-js" {
        fontFamily = styleManager.bodyFont
        fontSize = if (EduSettings.getInstance().shouldUseJavaFx()) styleManager.bodyFontSize.px else styleManager.bodyFontSize.pt
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
      ".vjs-resolution-button" {
        color = Color.white
      }
      ".vjs-nofull .vjs-fullscreen-control" {
        display = Display.none
      }
    }.toString()
  }
}