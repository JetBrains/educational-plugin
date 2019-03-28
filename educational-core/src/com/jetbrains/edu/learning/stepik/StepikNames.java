package com.jetbrains.edu.learning.stepik;

import com.intellij.openapi.application.ApplicationManager;

public class StepikNames {
  public static final String STEPIK = "Stepik";
  public static final String STEPIK_TYPE = STEPIK;
  public static final String ARE_SOLUTIONS_UPDATED_PROPERTY = "Educational.StepikSolutionUpdated";
  public static final String STEPIK_URL = ApplicationManager.getApplication().isUnitTestMode() ? "https://release.stepik.org" : "https://stepik.org" ;
  public static final String TOKEN_URL = STEPIK_URL + "/oauth2/token/";
  public static final String STEPIK_API_URL = STEPIK_URL + "/api/";

  public static final String PYCHARM_PREFIX = "pycharm";
  public static final String EDU_STEPIK_SERVICE_NAME = "edu/stepik";
  public static final String LINK = "link";
  public static final String CLIENT_ID = StepikOAuthBundle.INSTANCE.valueOrDefault("clientId", "");
  public static final String OAUTH_SERVICE_NAME = "edu/stepik/oauth";
  public static final String EXTERNAL_REDIRECT_URL = "https://example.com";
  public static final String PYCHARM_ADDITIONAL = "PyCharm additional materials";
  public static final String ADDITIONAL_FILES = "additional_files.json";

  public static final String PLUGIN_NAME = "EduTools";
}
