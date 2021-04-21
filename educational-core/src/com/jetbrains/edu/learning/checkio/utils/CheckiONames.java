package com.jetbrains.edu.learning.checkio.utils;

import com.sun.istack.NotNull;
import org.jetbrains.annotations.NonNls;

import static com.jetbrains.edu.learning.marketplace.MarketplaceNamesKt.MARKETPLACE_PLUGIN_URL;

public final class CheckiONames {
  private CheckiONames() {}

  @NonNls public static final String CHECKIO = "CheckiO";
  @NonNls public static final String CHECKIO_TYPE = CHECKIO;

  @NonNls public static final String HTTPS = "https://";
  @NonNls public static final String CHECKIO_URL = "checkio.org";

  @NonNls public static final String CHECKIO_OAUTH_HOST = HTTPS + CHECKIO_URL;
  @NonNls private static final String CHECKIO_OAUTH_PATH = "/oauth/authorize";
  @NonNls public static final String CHECKIO_OAUTH_URL = CHECKIO_OAUTH_HOST + CHECKIO_OAUTH_PATH;

  @NonNls public static final String CHECKIO_OAUTH_REDIRECT_HOST = "http://localhost";
  @NonNls public static final String CHECKIO_OAUTH_SERVICE_PREFIX = "/api";
  @NonNls public static final String CHECKIO_OAUTH_SERVICE_NAME = "edu/checkio/oauth";
  @NonNls public static final String CHECKIO_OAUTH_SERVICE_PATH = CHECKIO_OAUTH_SERVICE_PREFIX + "/" + CHECKIO_OAUTH_SERVICE_NAME;
  @NonNls public static final String CHECKIO_USER = "/user/";

  @NonNls public static final String CHECKIO_TEST_FORM_TARGET_PATH = "/mission/check-html-output";

  @NonNls public static final String CHECKIO_HELP = MARKETPLACE_PLUGIN_URL + "/10081-edutools/docs/checkio-integration.html";

  @NonNls public static final String PY_CHECKIO = "Py " + CHECKIO;
  @NonNls public static final String JS_CHECKIO = "Js " + CHECKIO;

  @NotNull
  @NonNls
  public static String getSolutionsLink(@NotNull String languageId, @NotNull String slug) {
    return HTTPS + languageId + "." + CHECKIO_URL + "/mission/" + slug + "/publications/category/clear";
  }

  @NotNull
  @NonNls
  public static String getTaskLink(@NotNull String languageId, @NotNull String humanLanguageId, @NotNull String slug) {
    return HTTPS + languageId + "." + CHECKIO_URL + "/" + humanLanguageId + "/mission/" + slug;
  }
}
