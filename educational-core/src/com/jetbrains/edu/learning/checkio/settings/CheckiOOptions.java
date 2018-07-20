package com.jetbrains.edu.learning.checkio.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.ui.HoverHyperlinkLabel;
import com.intellij.ui.HyperlinkAdapter;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.components.JBLabel;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.jetbrains.edu.learning.checkio.CheckiONames;
import com.jetbrains.edu.learning.checkio.connectors.CheckiOConnector;
import com.jetbrains.edu.learning.checkio.model.CheckiOUser;
import com.jetbrains.edu.learning.checkio.model.Tokens;
import com.jetbrains.edu.learning.settings.OptionsProvider;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import java.util.Objects;

public class CheckiOOptions implements OptionsProvider {
  private JBLabel myLoginLabel;
  private HoverHyperlinkLabel myLoginLink;
  private JPanel myPanel;
  private HyperlinkAdapter myLoginListener;

  private CheckiOUser myUser;
  private Tokens myTokens;

  @Nls
  @Override
  public String getDisplayName() {
    return "CheckiO options";
  }

  @Nullable
  @Override
  public JComponent createComponent() {
    myPanel = new JPanel(new GridLayoutManager(1, 2));
    addLoginLabel();
    addLoginLink();
    myPanel.setBorder(IdeBorderFactory.createTitledBorder(CheckiONames.CHECKIO));
    return myPanel;
  }

  private void addLoginLabel() {
    myLoginLabel = new JBLabel();
    GridConstraints constraints = new GridConstraints();
    constraints.setRow(0);
    constraints.setColumn(0);
    constraints.setAnchor(GridConstraints.ANCHOR_WEST);
    constraints.setHSizePolicy(GridConstraints.SIZEPOLICY_FIXED);
    myPanel.add(myLoginLabel, constraints);
  }

  private void addLoginLink() {
    myLoginLink = new HoverHyperlinkLabel("");
    GridConstraints constraints = new GridConstraints();
    constraints.setRow(0);
    constraints.setColumn(1);
    constraints.setAnchor(GridConstraints.ANCHOR_WEST);
    myPanel.add(myLoginLink, constraints);
  }

  @Override
  public boolean isModified() {
    return !Objects.equals(myUser, CheckiOSettings.getInstance().getUser()) ||
           !Objects.equals(myTokens, CheckiOSettings.getInstance().getTokens());
  }

  @Override
  public void reset() {
    myUser = CheckiOSettings.getInstance().getUser();
    myTokens = CheckiOSettings.getInstance().getTokens();
    updateLoginLabels(myUser);
  }

  @Override
  public void apply() throws ConfigurationException {
    if (isModified()) {
      CheckiOSettings.getInstance().setUser(myUser);
      CheckiOSettings.getInstance().setTokens(myTokens);
    }

    reset();
  }

  private void updateLoginLabels(@Nullable CheckiOUser user) {
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

  @NotNull
  private HyperlinkAdapter createAuthorizeListener() {
    return new HyperlinkAdapter() {
      @Override
      protected void hyperlinkActivated(HyperlinkEvent event) {
        ApplicationManager.getApplication().getMessageBus().connect().subscribe(CheckiOConnector.LOGGED_IN, (newTokens, newUser)-> {
          if (!Objects.equals(myUser, newUser)) {
            CheckiOSettings.getInstance().setUser(myUser);
            CheckiOSettings.getInstance().setTokens(myTokens);

            myUser = newUser;
            myTokens = newTokens;

            updateLoginLabels(myUser);
          }
        });

        CheckiOConnector.doAuthorize();
      }
    };
  }

  @NotNull
  private HyperlinkAdapter createLogoutListener() {
    return new HyperlinkAdapter() {
      @Override
      protected void hyperlinkActivated(HyperlinkEvent event) {
        myTokens = null;
        myUser = null;
        updateLoginLabels(null);
      }
    };
  }
}
