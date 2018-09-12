package com.jetbrains.edu.learning.courseFormat.remote

interface RemoteInfo

class LocalInfo : RemoteInfo

class StepikRemoteInfo : RemoteInfo {
  var isPublic: Boolean = false
}
