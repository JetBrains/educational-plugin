package com.jetbrains.edu.learning.checkio.utils;

public final class CheckiONames {
  private CheckiONames() {}

  public static final String CHECKIO = "CheckiO";

  public static final String CHECKIO_URL = "checkio.org";

  public static final String CHECKIO_OAUTH_HOST = "https://" + CHECKIO_URL;
  private static final String CHECKIO_OAUTH_PATH = "/oauth/authorize";
  public static final String CHECKIO_OAUTH_URL = CHECKIO_OAUTH_HOST + CHECKIO_OAUTH_PATH;

  public static final String CHECKIO_OAUTH_REDIRECT_HOST = "http://localhost";
  public static final String CHECKIO_OAUTH_SERVICE_PREFIX = "/api";
  public static final String CHECKIO_OAUTH_SERVICE_NAME = "edu/checkio/oauth";
  public static final String CHECKIO_OAUTH_SERVICE_PATH = CHECKIO_OAUTH_SERVICE_PREFIX + "/" + CHECKIO_OAUTH_SERVICE_NAME;

  public static final String CHECKIO_TEST_FORM_URL = "/checkio/checkioTestForm.html";
}
