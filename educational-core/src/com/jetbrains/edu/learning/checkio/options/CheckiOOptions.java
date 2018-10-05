package com.jetbrains.edu.learning.checkio.options;

import com.intellij.ui.HyperlinkAdapter;
import com.jetbrains.edu.learning.checkio.account.CheckiOAccount;
import com.jetbrains.edu.learning.checkio.connectors.CheckiOOAuthConnector;
import com.jetbrains.edu.learning.settings.OauthOptions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.event.HyperlinkEvent;

public abstract class CheckiOOptions extends OauthOptions<CheckiOAccount> {
  private final CheckiOOAuthConnector myOAuthConnector;

  protected CheckiOOptions(@NotNull CheckiOOAuthConnector oauthConnector) {
    super();
    myOAuthConnector = oauthConnector;
    initAccounts();
  }

  @Nullable
  @Override
  public CheckiOAccount getCurrentAccount() {
    return myOAuthConnector.getAccount();
  }

  @Override
  public void setCurrentAccount(@Nullable CheckiOAccount lastSavedAccount) {
    myOAuthConnector.setAccount(lastSavedAccount);
  }

  @NotNull
  protected HyperlinkAdapter createAuthorizeListener() {
    return new HyperlinkAdapter() {
      @Override
      protected void hyperlinkActivated(HyperlinkEvent event) {
        myOAuthConnector.doAuthorize(() -> {
          setLastSavedAccount(getCurrentAccount());
          updateLoginLabels();
        });
      }
    };
  }
}
