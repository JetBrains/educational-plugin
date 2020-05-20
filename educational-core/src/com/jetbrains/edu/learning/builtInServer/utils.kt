package com.jetbrains.edu.learning.builtInServer

import io.netty.bootstrap.ServerBootstrap
import org.jetbrains.ide.BuiltInServerManager
import org.jetbrains.ide.BuiltInServerManagerImpl

fun createServerBootstrap(): ServerBootstrap = (BuiltInServerManager.getInstance() as BuiltInServerManagerImpl).createServerBootstrap()