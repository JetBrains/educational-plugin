package com.jetbrains.edu.learning.checkio.utils;

import com.sun.istack.NotNull;
import org.jetbrains.annotations.NonNls;

import static com.jetbrains.edu.learning.marketplace.MarketplaceNamesKt.MARKETPLACE_PLUGIN_URL;

public final class CheckiONames {
  private CheckiONames() {}

  @NonNls public static final String CHECKIO = "CheckiO";
  @NonNls public static final String CHECKIO_TYPE = CHECKIO;

  @NonNls private static final String HTTPS = "https://";
  @NonNls public static final String CHECKIO_HOST = "checkio.org";
  @NonNls public static final String CHECKIO_URL = HTTPS + CHECKIO_HOST;

  @NonNls public static final String CHECKIO_USER = "/user/";

  @NonNls public static final String CHECKIO_TEST_FORM_TARGET_PATH = "/mission/check-html-output";

  @NonNls public static final String CHECKIO_HELP = MARKETPLACE_PLUGIN_URL + "/10081-edutools/docs/checkio-integration.html";

  @NonNls public static final String PY_CHECKIO_PREFIX = "Py";
  @NonNls public static final String PY_CHECKIO = PY_CHECKIO_PREFIX+ " " + CHECKIO;
  @NonNls public static final String JS_CHECKIO_PREFIX = "Js";
  @NonNls public static final String JS_CHECKIO = JS_CHECKIO_PREFIX + " " + CHECKIO;

  @NotNull
  @NonNls
  public static String getSolutionsLink(@NotNull String languageId, @NotNull String slug) {
    return HTTPS + languageId + "." + CHECKIO_HOST + "/mission/" + slug + "/publications/category/clear";
  }

  @NotNull
  @NonNls
  public static String getTaskLink(@NotNull String languageId, @NotNull String humanLanguageId, @NotNull String slug) {
    return HTTPS + languageId + "." + CHECKIO_HOST + "/" + humanLanguageId + "/mission/" + slug;
  }
}
