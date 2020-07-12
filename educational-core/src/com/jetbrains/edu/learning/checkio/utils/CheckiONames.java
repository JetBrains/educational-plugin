package com.jetbrains.edu.learning.checkio.utils;

import com.sun.istack.NotNull;

public final class CheckiONames {
  private CheckiONames() {}

  public static final String CHECKIO = "CheckiO";
  public static final String CHECKIO_TYPE = CHECKIO;
  public static final String VIEW_SOLUTIONS = "View solutions";
  public static final String TASK_LINK = "See the task on " + CHECKIO;

  public static final String HTTPS = "https://";
  public static final String CHECKIO_URL = "checkio.org";

  public static final String CHECKIO_OAUTH_HOST = HTTPS + CHECKIO_URL;
  private static final String CHECKIO_OAUTH_PATH = "/oauth/authorize";
  public static final String CHECKIO_OAUTH_URL = CHECKIO_OAUTH_HOST + CHECKIO_OAUTH_PATH;

  public static final String CHECKIO_OAUTH_REDIRECT_HOST = "http://localhost";
  public static final String CHECKIO_OAUTH_SERVICE_PREFIX = "/api";
  public static final String CHECKIO_OAUTH_SERVICE_NAME = "edu/checkio/oauth";
  public static final String CHECKIO_OAUTH_SERVICE_PATH = CHECKIO_OAUTH_SERVICE_PREFIX + "/" + CHECKIO_OAUTH_SERVICE_NAME;
  public static final String CHECKIO_USER = "/user/";

  public static final String CHECKIO_TEST_FORM_TARGET_PATH = "/mission/check-html-output";

  public static final String PY_CHECKIO = "Py " + CHECKIO;
  public static final String JS_CHECKIO = "Js " + CHECKIO;

  @NotNull
  public static String getSolutionsLink(@NotNull String languageId, @NotNull String slug) {
    return HTTPS + languageId + "." + CHECKIO_URL + "/mission/" + slug + "/publications/category/clear";
  }

  @NotNull
  public static String getTaskLink(@NotNull String languageId, @NotNull String humanLanguageId, @NotNull String slug) {
    return HTTPS + languageId + "." + CHECKIO_URL + "/" + humanLanguageId + "/mission/" + slug;
  }
}
