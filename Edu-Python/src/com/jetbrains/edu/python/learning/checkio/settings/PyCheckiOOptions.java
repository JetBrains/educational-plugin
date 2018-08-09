package com.jetbrains.edu.python.learning.checkio.settings;

import com.intellij.ui.HyperlinkAdapter;
import com.jetbrains.edu.learning.checkio.options.CheckiOOptions;
import com.jetbrains.edu.python.learning.checkio.PyCheckiOAccountHolder;
import com.jetbrains.edu.python.learning.checkio.connectors.PyCheckiOOAuthConnector;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import javax.swing.event.HyperlinkEvent;

public class PyCheckiOOptions extends CheckiOOptions {
  protected PyCheckiOOptions() {
    super("CheckiO Python", PyCheckiOAccountHolder.getInstance(), PyCheckiOOAuthConnector.getInstance());
  }

  @Nls
  @Override
  public String getDisplayName() {
    return "Py CheckiO options";
  }

  @NotNull
  protected HyperlinkAdapter createAuthorizeListener() {
    return new HyperlinkAdapter() {
      @Override
      protected void hyperlinkActivated(HyperlinkEvent event) {
        PyCheckiOOAuthConnector.getInstance().doAuthorize(() -> {
          PyCheckiOAccountHolder.getInstance().setAccount(myCurrentAccount);
          myCurrentAccount = PyCheckiOAccountHolder.getInstance().getAccount();
          updateLoginLabels();
        });
      }
    };
  }
}
