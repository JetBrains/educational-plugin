package com.jetbrains.edu.commandLine

import com.github.ajalt.clikt.command.main
import com.github.ajalt.clikt.core.context
import com.intellij.openapi.application.ModernApplicationStarter
import com.intellij.openapi.diagnostic.logger
import kotlin.system.exitProcess

@Suppress("UnstableApiUsage")
abstract class EduAppStarterWrapper(protected val command: EduCommand) : ModernApplicationStarter() {
  override suspend fun start(args: List<String>) {
    try {
      command
        .context {
          echoMessage = LOG.toMessageEchoer()
          exitProcess = System::exit
        }
        .main(args.drop(1))
    }
    catch (e: Throwable) {
      LOG.error(e)
      exitProcess(1)
    }
  }

  companion object {
    @JvmStatic
    protected val LOG = logger<EduCourseCreatorAppStarter>()
  }
}
