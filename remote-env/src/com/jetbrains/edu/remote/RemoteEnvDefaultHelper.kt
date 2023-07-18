package com.jetbrains.edu.remote

import com.jetbrains.edu.learning.RemoteEnvHelper
import com.jetbrains.rdserver.unattendedHost.UnattendedHostManager

class RemoteEnvDefaultHelper : RemoteEnvHelper {
  override fun isRemoteServer(): Boolean = UnattendedHostManager.getInstance().isUnattendedMode
}