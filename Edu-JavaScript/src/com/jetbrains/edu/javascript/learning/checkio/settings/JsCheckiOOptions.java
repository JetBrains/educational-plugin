package com.jetbrains.edu.javascript.learning.checkio.settings;

import com.jetbrains.edu.javascript.learning.checkio.connectors.JsCheckiOOAuthConnector;
import com.jetbrains.edu.javascript.learning.checkio.utils.JsCheckiOUtils;
import com.jetbrains.edu.learning.checkio.account.CheckiOAccount;
import com.jetbrains.edu.learning.checkio.options.CheckiOOptions;
import com.jetbrains.edu.learning.checkio.utils.CheckiONames;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

public class JsCheckiOOptions extends CheckiOOptions {
  protected JsCheckiOOptions() {
    super(JsCheckiOOAuthConnector.getInstance());
  }

  @Nls
  @Override
  public String getDisplayName() {
    return CheckiONames.JS_CHECKIO;
  }

  @NotNull
  @Override
  protected String profileUrl(@NotNull CheckiOAccount account) {
    return JsCheckiOUtils.getProfileUrl(account);
  }
}
