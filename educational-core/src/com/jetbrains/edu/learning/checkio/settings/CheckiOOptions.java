package com.jetbrains.edu.learning.checkio.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.ui.HoverHyperlinkLabel;
import com.intellij.ui.HyperlinkAdapter;
import com.intellij.ui.components.JBLabel;
import com.jetbrains.edu.learning.checkio.controllers.CheckiOAuthorizationController;
import com.jetbrains.edu.learning.checkio.model.CheckiOUser;
import com.jetbrains.edu.learning.checkio.ui.CheckiOOptionsUIProvider;
import com.jetbrains.edu.learning.settings.OptionsProvider;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import java.util.Objects;

public class CheckiOOptions implements OptionsProvider {
  private CheckiOOptions() {}

  private final CheckiOOptionsUIProvider UIProvider = new CheckiOOptionsUIProvider();

  private JBLabel myLoginLabel = UIProvider.getLoginLabel();
  private HoverHyperlinkLabel myLoginLink = UIProvider.getLoginLink();
  private JPanel myPanel = UIProvider.getPanel();
  private HyperlinkAdapter myLoginListener;

  private CheckiOUser myUser;

  @Nls
  @Override
  public String getDisplayName() {
    return "CheckiO options";
  }

  @Nullable
  @Override
  public JComponent createComponent() {
    return myPanel;
  }

  @Override
  public boolean isModified() {
    return !Objects.equals(myUser, CheckiOSettings.getInstance().getUser());
  }

  @Override
  public void reset() {
    myUser = CheckiOSettings.getInstance().getUser();
    updateLoginLabels(myUser);
  }

  @Override
  public void apply() throws ConfigurationException {
    if (isModified()) {
      CheckiOSettings.getInstance().setUser(myUser);
    }

    reset();
  }

  private void updateLoginLabels(CheckiOUser user) {
    if (myLoginListener != null) {
      myLoginLink.removeHyperlinkListener(myLoginListener);
    }

    if (user == null) {
      myLoginLabel.setText("You're not logged in");
      myLoginLink.setText("Log in to CheckiO");

      myLoginListener = createAuthorizeListener();
    } else {
      myLoginLabel.setText("You're logged in as " + user.getUsername());
      myLoginLink.setText("Log out");

      myLoginListener = createLogoutListener();
    }

    myLoginLink.addHyperlinkListener(myLoginListener);
  }

  private HyperlinkAdapter createAuthorizeListener() {
    return new HyperlinkAdapter() {
      @Override
      protected void hyperlinkActivated(HyperlinkEvent event) {
        ApplicationManager.getApplication().getMessageBus().connect().subscribe(CheckiOAuthorizationController.LOGGED_IN, (newUser) -> {
          if (!Objects.equals(myUser, newUser)) {
            CheckiOSettings.getInstance().setUser(myUser);
            myUser = newUser;
            updateLoginLabels(myUser);
          }
        });
        CheckiOAuthorizationController.doAuthorize();
      }
    };
  }

  private HyperlinkAdapter createLogoutListener() {
    return new HyperlinkAdapter() {
      @Override
      protected void hyperlinkActivated(HyperlinkEvent event) {
        myUser = null;
        updateLoginLabels(null);
      }
    };
  }
}
