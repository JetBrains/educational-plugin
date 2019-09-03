@file:Suppress("DEPRECATION")

package com.jetbrains.edu.learning

import com.intellij.featureStatistics.FeatureStatisticsBundleProvider
import com.intellij.openapi.Disposable
import org.hamcrest.CoreMatchers
import org.hamcrest.Matcher

private const val CIDR_FEATURE_STATISTICS_PROVIDER_FQNAME = "com.jetbrains.cidr.lang.OCFeatureStatisticsBundleProvider"

inline fun <reified T> nullValue(): Matcher<T> = CoreMatchers.nullValue(T::class.java)

// AS relies on a bundle provided by CIDR feature statistic provider, but it is not registered in tests for some reason.
// And it leads to fail of some tests.
// This hack tries to load this provider manually.
//
// Inspired by kotlin plugin
fun registerAdditionalResourceBundleProviders(disposable: Disposable) {
  val isAlreadyRegistered = FeatureStatisticsBundleProvider.EP_NAME.extensions.any { provider ->
    provider.javaClass.name == CIDR_FEATURE_STATISTICS_PROVIDER_FQNAME
  }
  if (isAlreadyRegistered) return

  val providerClass = try {
    Class.forName(CIDR_FEATURE_STATISTICS_PROVIDER_FQNAME)
  } catch (_: ClassNotFoundException) {
    return
  }

  val provider = providerClass.newInstance() as FeatureStatisticsBundleProvider
  FeatureStatisticsBundleProvider.EP_NAME.getPoint(null).registerExtension(provider, disposable)
}
