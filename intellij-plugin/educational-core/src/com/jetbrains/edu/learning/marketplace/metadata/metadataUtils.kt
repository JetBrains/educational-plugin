package com.jetbrains.edu.learning.marketplace.metadata

import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.stepik.hyperskill.HYPERSKILL_DEFAULT_HOST
import org.jetbrains.annotations.TestOnly
import java.net.URI
import java.net.URISyntaxException
import java.util.function.Supplier

/**
 * Contains a set of trusted hosts for metadata links.
 * Each host is associated with message on Task Description view of the corresponding link
 */
val TRUSTED_METADATA_HOSTS: Map<String, Supplier<String>> = mapOf(
  HYPERSKILL_DEFAULT_HOST to EduCoreBundle.lazyMessage("action.open.on.text", EduNames.JBA),
  "academy.jetbrains.com" to EduCoreBundle.lazyMessage("action.open.in.course.catalog"),
  "jetbrains-academy-staging-external.labs.jb.gg" to EduCoreBundle.lazyMessage("action.open.in.course.catalog"),
  "staging.academy.labs.jb.gg" to EduCoreBundle.lazyMessage("action.open.in.course.catalog"),
)

fun String.isValidAndAllowedUrl(): Boolean = try {
  val uri = URI(this)
  uri.scheme == "https" && uri.host in TRUSTED_METADATA_HOSTS
}
catch (_: URISyntaxException) {
  false
}
