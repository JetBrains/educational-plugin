package com.jetbrains.edu.learning.checkio;

public class CheckioNames {
  public static final String CLIENT_ID = "clientId";
  public static final String CLIENT_SECRET = "clientSecret";

  public static final String CHECKIO_URL = "https://checkio.org";
  public static final String CHECKIO_OAUTH_SERVICE = "/oauth/authorize";
  public static final String CHECKIO_OAUTH_URL = CHECKIO_URL + CHECKIO_OAUTH_SERVICE;

  public static final String CHECKIO_OAUTH_SUCCEED_PAGE = "/oauthResponsePages/checkioOkPage.html";
  public static final String CHECKIO_OAUTH_FAILED_PAGE = "/oauthResponsePages/checkioErrorPage.html";

  public static final String CHECKIO_OAUTH_SUCCEED_DEFAULT_MESSAGE = "You're successfully logged in, you may return to IDE.";
  public static final String CHECKIO_OAUTH_FAILED_DEFAULT_MESSAGE = "Error occurred requesting user info. Please, try to log in again";

  public static class GRANT_TYPE {
    public static final String AUTHORIZATION_CODE = "authorization_code";
    public static final String REFRESH_TOKEN = "refresh_token";
  }
}
