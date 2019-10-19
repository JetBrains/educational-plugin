package com.jetbrains.edu.learning.builtInServer

import com.intellij.ide.RecentProjectsManagerBase
import io.netty.bootstrap.ServerBootstrap
import org.jetbrains.ide.BuiltInServerManager
import org.jetbrains.ide.BuiltInServerManagerImpl

val recentProjectsManagerInstance: RecentProjectsManagerBase get() = RecentProjectsManagerBase.getInstanceEx()

fun createServerBootstrap() : ServerBootstrap = (BuiltInServerManager.getInstance() as BuiltInServerManagerImpl).createServerBootstrap()