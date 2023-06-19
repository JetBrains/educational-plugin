package com.jetbrains.edu.remote

import com.jetbrains.rdserver.unattendedHost.UnattendedStatusUtil

@Suppress("UnstableApiUsage")
fun isRemoteDevServer(): Boolean = UnattendedStatusUtil.getStatus().unattendedMode