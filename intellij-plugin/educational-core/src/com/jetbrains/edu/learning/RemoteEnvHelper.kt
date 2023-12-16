package com.jetbrains.edu.learning

import com.intellij.openapi.extensions.ExtensionPointName

/**
 * This extension must be implemented only in one place
 * @see [com.jetbrains.edu.remote.RemoteEnvDefaultHelper]
 */
interface RemoteEnvHelper {
  fun isRemoteServer(): Boolean

  /**
   * This token identifies the user logged into JBA and is used for authorization in the Submission service
   */
  fun getUserUidToken(): String?

  companion object {
    val EP_NAME: ExtensionPointName<RemoteEnvHelper> = ExtensionPointName.create("Educational.remoteEnvHelper")

    fun isRemoteDevServer(): Boolean = EP_NAME.computeSafeIfAny { it.isRemoteServer() } == true

    fun getUserUidToken(): String? = EP_NAME.computeSafeIfAny { it.getUserUidToken() }
  }
}