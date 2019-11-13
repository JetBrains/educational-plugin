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

  constructor(name: String, id: Int, position: Int, updateDate: Date, status: CheckStatus) : super(name, id, position, updateDate, status)

  override fun getItemType() = "video"

  override fun isPluginTaskType() = false
}

class VideoSource(var src: String, var res: String) {
  val type = "video/mp4"
  var label = "${res}p"

  //used for deserialization
  @Suppress("unused")
  constructor() : this("", "")

}