package com.jetbrains.edu.learning.network

import com.intellij.concurrency.ConcurrentCollectionFactory
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.util.Disposer
import com.intellij.util.io.isLocalHost
import com.jetbrains.edu.learning.EduTestAware
import com.jetbrains.edu.learning.isUnitTestMode
import org.jetbrains.annotations.TestOnly

@Service(Service.Level.APP)
class TestNetworkRequestManager : EduTestAware {
  private val allowedHosts: MutableSet<String> = ConcurrentCollectionFactory.createConcurrentSet()

  fun allowHosts(disposable: Disposable, vararg hosts: String) {
    require(isUnitTestMode) {
      "`allowHosts` should be called only in tests"
    }

    allowedHosts += hosts
    Disposer.register(disposable) {
      allowedHosts -= hosts
    }
  }

  fun checkHostIsAllowed(host: String) {
    require(isUnitTestMode) {
      "`checkHostIsAllowed` should be called only in tests"
    }

    check(isLocalHost(host) || host in allowedHosts) {
      val allowedHostsMessage = if (allowedHosts.isEmpty()) "localhost" else "localhost and $allowedHosts"

      "Only requests to $allowedHostsMessage are allowed in this test. Request to `$host` is denied.\n" +
      "Use `TestNetworkRequestManager.getInstance().allowHosts(...)` to allow this host in tests."
    }
  }

  @TestOnly
  override fun cleanUpState() {
    allowedHosts.clear()
  }

  companion object {
    fun getInstance(): TestNetworkRequestManager = service()
  }
}
