package com.jetbrains.edu.learning.checkio;

import org.jetbrains.ide.RestService;

public class CheckiONames {
  private CheckiONames() {}

  public static final String CHECKIO = "CheckiO";

  public static final String CHECKIO_URL = "https://checkio.org";
  public static final String CHECKIO_OAUTH_SERVICE = "/oauth/authorize";
  public static final String CHECKIO_OAUTH_URL = CHECKIO_URL + CHECKIO_OAUTH_SERVICE;

  public static final String EDU_CHECKIO_SERVICE_NAME = "edu/checkio";

  public static final String EDU_CHECKIO_OAUTH_HOST = "http://localhost";
  public static final String EDU_CHECKIO_OAUTH_SERVICE = "/" + RestService.PREFIX + "/" + EDU_CHECKIO_SERVICE_NAME + "/oauth";

  public static class GRANT_TYPE {
    public static final String AUTHORIZATION_CODE = "authorization_code";
    public static final String REFRESH_TOKEN = "refresh_token";
  }
}
