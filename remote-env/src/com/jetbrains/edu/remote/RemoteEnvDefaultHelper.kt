package com.jetbrains.edu.remote

import com.intellij.openapi.components.service
import com.jetbrains.edu.learning.RemoteEnvHelper
import com.jetbrains.rdserver.unattendedHost.UnattendedHostManager

class RemoteEnvDefaultHelper : RemoteEnvHelper {
  override fun isRemoteServer(): Boolean = UnattendedHostManager.getInstance().isUnattendedMode

  override fun getUidToken(): String? = service<EduRemoteUidHolderService>().userUid
}