package com.jetbrains.edu.learning.checkio.options;

import com.intellij.ui.HoverHyperlinkLabel;
import com.intellij.ui.HyperlinkAdapter;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.components.JBLabel;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.jetbrains.edu.learning.checkio.account.CheckiOAccount;
import com.jetbrains.edu.learning.checkio.connectors.CheckiOOAuthConnector;
import com.jetbrains.edu.learning.settings.OptionsProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import java.util.Objects;

public abstract class CheckiOOptions implements OptionsProvider {
  @NotNull private JBLabel myLoginLabel = new JBLabel();
  @NotNull private HoverHyperlinkLabel myLoginLink = new HoverHyperlinkLabel("");
  @NotNull private JPanel myPanel = new JPanel();
  @Nullable private HyperlinkAdapter myLoginListener;

  @Nullable private CheckiOAccount myCurrentAccount;
  @Nullable private CheckiOAccount myLastSavedAccount;

  private final String myTitle;
  private final CheckiOOAuthConnector myOAuthConnector;


  protected CheckiOOptions(@NotNull String optionsPanelTitle, @NotNull CheckiOOAuthConnector oauthConnector) {
    myTitle = optionsPanelTitle;
    myOAuthConnector = oauthConnector;
    myCurrentAccount = oauthConnector.getAccount();

    initUI();
  }

  private void initUI() {
    myPanel = new JPanel(new GridLayoutManager(1, 2));
    addLoginLabel();
    addLoginLink();
    myPanel.setBorder(IdeBorderFactory.createTitledBorder(myTitle));
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

  @Nullable
  @Override
  public JComponent createComponent() {
    myLastSavedAccount = myOAuthConnector.getAccount();
    return myPanel;
  }

  @Override
  public boolean isModified() {
    return !Objects.equals(myCurrentAccount, myLastSavedAccount);
  }

  @Override
  public void reset() {
    myCurrentAccount = myLastSavedAccount;
    updateLoginLabels();
  }

  @Override
  public void apply() {
    if (isModified()) {
      myLastSavedAccount = myCurrentAccount;
    }

    reset();
  }

  protected void updateLoginLabels() {
    if (myLoginListener != null) {
      myLoginLink.removeHyperlinkListener(myLoginListener);
    }

    if (myCurrentAccount == null) {
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
          myCurrentAccount = myOAuthConnector.getAccount();
          updateLoginLabels();
        });
      }
    };
  }

  @Override
  public void disposeUIResources() {
    myOAuthConnector.setAccount(myLastSavedAccount);
  }

  @NotNull
  private HyperlinkAdapter createLogoutListener() {
    return new HyperlinkAdapter() {
      @Override
      protected void hyperlinkActivated(HyperlinkEvent event) {
        myOAuthConnector.setAccount(null);
        myCurrentAccount = null;
        updateLoginLabels();
      }
    };
  }
}
