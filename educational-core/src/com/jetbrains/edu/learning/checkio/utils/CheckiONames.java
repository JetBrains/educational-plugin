package com.jetbrains.edu.learning.checkio.utils;

public class CheckiONames {
  private CheckiONames() {}

  public static final String CHECKIO = "CheckiO";
  public static final String PY_CHECKIO = "Py " + CHECKIO;

  public static final String PY_CHECKIO_API_HOST = "https://py.checkio.org";
  public static final String CHECKIO_OAUTH_HOST = "https://checkio.org";
  private static final String CHECKIO_OAUTH_PATH = "/oauth/authorize";
  public static final String CHECKIO_OAUTH_URL = CHECKIO_OAUTH_HOST + CHECKIO_OAUTH_PATH;

  public static final String CHECKIO_OAUTH_REDIRECT_HOST = "http://localhost";
  public static final String CHECKIO_OAUTH_SERVICE_PREFIX = "/api/";

  public static final String PY_CHECKIO_OAUTH_SERVICE_NAME = "edu/checkio/oauth/py";
  public static final String JS_CHECKIO_OAUTH_SERVICE_NAME = "edu/checkio/oauth/js";

  public static final String PY_CHECKIO_OAUTH_SERVICE_PATH = CHECKIO_OAUTH_SERVICE_PREFIX + "edu/checkio/oauth/py";
  public static final String JS_CHECKIO_OAUTH_SERVICE_PATH = CHECKIO_OAUTH_SERVICE_PREFIX + "edu/checkio/oauth/js";

  public static final String CHECKIO_TEST_FORM_URL = "/checkio/checkioTestForm.html";

  public static class GRANT_TYPE {
    public static final String AUTHORIZATION_CODE = "authorization_code";
    public static final String REFRESH_TOKEN = "refresh_token";
  }
}
