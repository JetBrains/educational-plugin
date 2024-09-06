package com.jetbrains.edu.learning.command

import com.intellij.diagnostic.ThreadDumper
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


@Service(Service.Level.APP)
class EduThreadDumpService(private val scope: CoroutineScope) {

  @Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
  fun startThreadDumping() {
    LOG.warn("Default core pool size: ${kotlinx.coroutines.scheduling.CORE_POOL_SIZE}")

    scope.launch {
      while (true) {
        delay(5 * 60 * 1000)
        LOG.warn("thread dump\n" + ThreadDumper.getThreadDumpInfo(ThreadDumper.getThreadInfos(), false).rawDump)
      }
    }
  }

  companion object {
    private val LOG = logger<EduThreadDumpService>()

    fun getInstance(): EduThreadDumpService = service()
  }
}