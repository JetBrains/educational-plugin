package com.jetbrains.edu.learning.courseFormat.tasks

import com.jetbrains.edu.learning.courseFormat.CheckStatus
import java.util.*

class VideoTask : TheoryTask {
  var thumbnail: String = ""
  var sources = listOf<VideoSource>()
  var currentTime = 0

  //used for deserialization
  @Suppress("unused")
  constructor()

  constructor(name: String) : super(name)

  constructor(name: String, id: Int, position: Int, updateDate: Date, status: CheckStatus) : super(name, id, position, updateDate, status)

  override fun getItemType() = VIDEO_TASK_TYPE

  override fun isPluginTaskType() = false

  companion object {
    const val VIDEO_TASK_TYPE: String = "video"
  }
}

class VideoSource(var src: String, var res: String) {
  val type = "video/mp4"
  var label = "${res}p"

  //used for deserialization
  @Suppress("unused")
  constructor() : this("", "")

}