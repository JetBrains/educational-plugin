package com.jetbrains.edu.learning.builtInServer

import com.intellij.ide.RecentProjectsManagerBase
import com.intellij.idea.StartupUtil
import com.intellij.util.io.serverBootstrap
import io.netty.bootstrap.ServerBootstrap

val recentProjectsManagerInstance: RecentProjectsManagerBase get() = RecentProjectsManagerBase.getInstanceEx()

fun createServerBootstrap() : ServerBootstrap = serverBootstrap(StartupUtil.getServer()!!.eventLoopGroup)