package com.jetbrains.edu.learning.courseFormat.tasks

class VideoTask : TheoryTask {
  var thumbnail: String = ""
  var sources = listOf<VideoSource>()
  var currentTime = 0

  //used for deserialization
  @Suppress("unused")
  constructor()

  constructor(name: String) : super(name)

  override fun getItemType() = "video"

}

class VideoSource(var src: String, var res: String) {
  val type = "video/mp4"
  var label = "${res}p"

  //used for deserialization
  @Suppress("unused")
  constructor() : this("", "")

}