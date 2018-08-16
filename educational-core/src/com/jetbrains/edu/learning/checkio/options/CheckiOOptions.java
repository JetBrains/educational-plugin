package com.jetbrains.edu.learning.checkio.options;

import com.intellij.ui.HoverHyperlinkLabel;
import com.intellij.ui.HyperlinkAdapter;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.components.JBLabel;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.jetbrains.edu.learning.checkio.connectors.CheckiOOAuthConnector;
import com.jetbrains.edu.learning.checkio.model.CheckiOAccount;
import com.jetbrains.edu.learning.settings.OptionsProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import java.util.Objects;

public abstract class CheckiOOptions implements OptionsProvider {
  private JBLabel myLoginLabel;
  private HoverHyperlinkLabel myLoginLink;
  private JPanel myPanel;
  private HyperlinkAdapter myLoginListener;

  private CheckiOAccount myCurrentAccount;

  private final String myTitle;
  private final CheckiOOAuthConnector myOAuthConnector;


  protected CheckiOOptions(
    @NotNull String optionsPanelTitle,
    @NotNull CheckiOOAuthConnector oauthConnector
  ) {
    myTitle = optionsPanelTitle;
    myOAuthConnector = oauthConnector;
  }

  @Nullable
  @Override
  public JComponent createComponent() {
    myPanel = new JPanel(new GridLayoutManager(1, 2));
    addLoginLabel();
    addLoginLink();
    myPanel.setBorder(IdeBorderFactory.createTitledBorder(myTitle));
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
    return !Objects.equals(myCurrentAccount, myOAuthConnector.getAccount());
  }

  @Override
  public void reset() {
    myCurrentAccount = myOAuthConnector.getAccount();
    updateLoginLabels();
  }

  @Override
  public void apply() {
    if (isModified()) {
      myOAuthConnector.setAccount(myCurrentAccount);
    }

    reset();
  }

  protected void updateLoginLabels() {
    if (myLoginListener != null) {
      myLoginLink.removeHyperlinkListener(myLoginListener);
    }

    if (!myCurrentAccount.isLoggedIn()) {
      myLoginLabel.setText("You're not logged in");
      myLoginLink.setText("Log in to CheckiO");

      myLoginListener = createAuthorizeListener();
    }
    else {
      myLoginLabel.setText("You're logged in as " + myCurrentAccount.getUserInfo().getUsername());
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
        myOAuthConnector.doAuthorize(() -> {
          final CheckiOAccount newAccount = myOAuthConnector.getAccount();
          myOAuthConnector.setAccount(myCurrentAccount);
          myCurrentAccount = newAccount;
          updateLoginLabels();
        });
      }
    };
  }

  @NotNull
  private HyperlinkAdapter createLogoutListener() {
    return new HyperlinkAdapter() {
      @Override
      protected void hyperlinkActivated(HyperlinkEvent event) {
        myCurrentAccount.logOut();
        updateLoginLabels();
      }
    };
  }
}
