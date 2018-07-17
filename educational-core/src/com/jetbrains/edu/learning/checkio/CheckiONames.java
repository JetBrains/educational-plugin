package com.jetbrains.edu.learning.checkio;

import org.jetbrains.ide.RestService;

public class CheckiONames {
  public static final String CHECKIO_NAME = "CheckiO";
  public static final String CLIENT_ID = CheckiOOAuthBundle.message("checkioClientId");
  public static final String CLIENT_SECRET = CheckiOOAuthBundle.message("checkioClientSecret");

  public static final String CHECKIO_URL = "https://checkio.org";
  public static final String CHECKIO_OAUTH_SERVICE = "/oauth/authorize";
  public static final String CHECKIO_OAUTH_URL = CHECKIO_URL + CHECKIO_OAUTH_SERVICE;

  public static final String EDU_CHECKIO_SERVICE_NAME = "edu/checkio";

  public static final String EDU_CHECKIO_OAUTH_HOST = "http://localhost";
  public static final String EDU_CHECKIO_OAUTH_SERVICE = "/" + RestService.PREFIX + "/" + EDU_CHECKIO_SERVICE_NAME + "/oauth";

  public static final String CHECKIO_OAUTH_SUCCEED_PAGE = "/oauthResponsePages/checkioOkPage.html";
  public static final String CHECKIO_OAUTH_FAILED_PAGE = "/oauthResponsePages/checkioErrorPage.html";

  public static class GRANT_TYPE {
    public static final String AUTHORIZATION_CODE = "authorization_code";
    public static final String REFRESH_TOKEN = "refresh_token";
  }
}
