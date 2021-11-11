@file:JvmName("TrustedProjectUtils")

package com.jetbrains.edu.learning.newproject

import com.intellij.openapi.diagnostic.Logger
import java.nio.file.Path

private val LOG = Logger.getInstance(":com.jetbrains.edu.learning.newproject.TrustedProjectUtils")

// Starting with 2021.3.1 and 2021.2.4 some project trusted API was changed,
// so let's check if new API is available not to used outdated API and produce runtime errors
//
// BACKCOMPAT: 2021.3. Drop it since new API should be always available
val isNewTrustedProjectApiAvailable: Boolean get() {
  return setProjectPathTrustedMethod != null
}

// BACKCOMPAT: 2021.3. Use `TrustedPaths.getInstance().setProjectPathTrusted(path, true)` directly
fun setProjectPathTrusted(path: Path) {
  if (setProjectPathTrustedMethod == null) {
    LOG.error("`com.intellij.ide.impl.TrustedPaths` API is not available")
  }
  setProjectPathTrustedMethod?.invoke(path, true)
}

// Reflection version of `TrustedPaths.getInstance()::setProjectPathTrusted`
private val setProjectPathTrustedMethod: ((Path, Boolean) -> Unit)? by lazy {
  try {
    val trustedPathClass = Class.forName("com.intellij.ide.impl.TrustedPaths")
    val getInstance = trustedPathClass.getMethod("getInstance")
    val setProjectPathTrusted = trustedPathClass.getMethod("setProjectPathTrusted", Path::class.java, Boolean::class.java)
    val serviceInstance = getInstance.invoke(null);
    { path, trusted ->
      try {
        setProjectPathTrusted.invoke(serviceInstance, path, trusted)
      }
      catch (e: Throwable) {
        LOG.error(e)
      }
    }
  }
  catch (ignore: Throwable) {
    null
  }
}
