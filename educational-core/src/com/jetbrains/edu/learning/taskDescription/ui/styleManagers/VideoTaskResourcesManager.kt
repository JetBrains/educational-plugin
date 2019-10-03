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
 * Is used to provide resources and stylesheet to stepikVideo.html.ft
 */
class VideoTaskResourcesManager(private val task: VideoTask, private val lesson: Lesson) {
  private val VIDEO_TEMPLATE = "stepikVideo.html"

  private val resources = mapOf(
    "thumbnail" to task.thumbnail,
    "sources" to Gson().toJson(task.sources),
    "currentTime" to task.currentTime.toString(),
    "stepikLink" to getStepikLink(task, lesson),
    "video_style" to videoStylesheet(),
    "typography_color_style" to StyleResourcesManager().typographyAndColorStylesheet()
  )

  val text: String
    get() = if (task.sources.isNotEmpty()) {
      GeneratorUtils.getInternalTemplateText(VIDEO_TEMPLATE, resources)
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
      }
      ".video-js" {
        display = Display.block
        height = 90.pct
        width = 100.pct
        backgroundColor = styleManager.bodyBackground
        position = Position.relative
        overflow = Overflow.hidden
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