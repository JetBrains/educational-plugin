package com.jetbrains.edu.learning

import com.intellij.openapi.extensions.ExtensionPointName

/**
 * This extension must be implemented only in one place
 * @see [com.jetbrains.edu.remote.RemoteEnvDefaultHelper]
 */
interface RemoteEnvHelper {
  fun isRemoteServer(): Boolean

  fun getUidToken(): String?

  companion object {
    val EP_NAME: ExtensionPointName<RemoteEnvHelper> = ExtensionPointName.create("Educational.remoteEnvHelper")

    @JvmStatic
    fun isRemoteDevServer(): Boolean = EP_NAME.computeSafeIfAny { it.isRemoteServer() } == true

    @JvmStatic
    fun getUserUidToken(): String? = EP_NAME.computeSafeIfAny { it.getUidToken() }
  }
}