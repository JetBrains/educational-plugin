package com.jetbrains.edu.learning.checkio.utils

import com.jetbrains.edu.learning.marketplace.MARKETPLACE_PLUGIN_URL
import com.sun.istack.NotNull
import org.jetbrains.annotations.NonNls

object CheckiONames {
  const val CHECKIO: @NonNls String = "CheckiO"
  const val CHECKIO_TYPE: @NonNls String = CHECKIO
  private const val HTTPS: @NonNls String = "https://"
  const val CHECKIO_HOST: @NonNls String = "checkio.org"
  const val CHECKIO_URL: @NonNls String = HTTPS + CHECKIO_HOST
  const val CHECKIO_USER: @NonNls String = "/user/"
  const val CHECKIO_TEST_FORM_TARGET_PATH: @NonNls String = "/mission/check-html-output"
  const val CHECKIO_HELP: @NonNls String = "$MARKETPLACE_PLUGIN_URL/10081-jetbrains-academy/docs/checkio-integration.html"
  const val PY_CHECKIO_PREFIX: @NonNls String = "Py"
  const val PY_CHECKIO: @NonNls String = "$PY_CHECKIO_PREFIX $CHECKIO"
  const val JS_CHECKIO_PREFIX: @NonNls String = "Js"
  const val JS_CHECKIO: @NonNls String = "$JS_CHECKIO_PREFIX $CHECKIO"
  @NotNull
  fun getSolutionsLink(@NotNull languageId: String, @NotNull slug: String): @NonNls String =
    "$HTTPS$languageId.$CHECKIO_HOST/mission/$slug/publications/category/clear"

  @NotNull
  fun getTaskLink(@NotNull languageId: String, @NotNull humanLanguageId: String, @NotNull slug: String): @NonNls String =
    "$HTTPS$languageId.$CHECKIO_HOST/$humanLanguageId/mission/$slug"
}
