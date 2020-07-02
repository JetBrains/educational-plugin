package com.jetbrains.edu.jvm.gradle

import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileCreateEvent
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.util.messages.MessageBusConnection
import com.jetbrains.edu.learning.gradle.GradleConstants.GRADLE_WRAPPER_UNIX

class GradleWrapperListener(private val connection: MessageBusConnection) : BulkFileListener {

  override fun after(events: List<VFileEvent>) {
    for (event in events) {
      if (event !is VFileCreateEvent) continue
      val file = event.file ?: continue
      if (file.name == GRADLE_WRAPPER_UNIX) {
        VfsUtil.virtualToIoFile(file).setExecutable(true)
        // We don't need this listener anymore
        connection.disconnect()
      }
    }
  }
}
