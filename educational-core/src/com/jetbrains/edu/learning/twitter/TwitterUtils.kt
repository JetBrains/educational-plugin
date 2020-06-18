package com.jetbrains.edu.learning.twitter;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.InputValidatorEx;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import com.jetbrains.edu.learning.messages.EduCoreBundle;
import org.apache.http.HttpStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import twitter4j.StatusUpdate;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;
import twitter4j.conf.ConfigurationBuilder;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;


public class TwitterUtils {
  private static final Logger LOG = Logger.getInstance(TwitterUtils.class);
  
  /**
   * Set consumer key and secret. 
   * @return Twitter instance with consumer key and secret set.
   */
  @NotNull
  public static Twitter getTwitter() {
    ConfigurationBuilder configurationBuilder = new ConfigurationBuilder();
    configurationBuilder.setOAuthConsumerKey(TwitterBundle.message("twitterConsumerKey"));
    configurationBuilder.setOAuthConsumerSecret(TwitterBundle.message("twitterConsumerSecret"));
    return new TwitterFactory(configurationBuilder.build()).getInstance();
  }

  /**
   * Set access token and token secret in Twitter instance
   */
  private static void setAuthInfoInTwitter(Twitter twitter, @NotNull String accessToken,
                                           @NotNull String tokenSecret) {
    AccessToken token = new AccessToken(accessToken, tokenSecret);
    twitter.setOAuthAccessToken(token);
  }

  public static void createTwitterDialogAndShow(@NotNull Project project, 
                                                @NotNull final TwitterPluginConfigurator configurator,
                                                @NotNull Task task) {
    ApplicationManager.getApplication().invokeLater(() -> {
      DialogWrapper.DoNotAskOption doNotAskOption = createDoNotAskOption();
      TwitterUtils.TwitterDialogPanel panel = configurator.getTweetDialogPanel(task);
      if (panel != null) {
        TwitterDialogWrapper wrapper = new TwitterDialogWrapper(project, panel, doNotAskOption);
        wrapper.setDoNotAskOption(doNotAskOption);

        if (wrapper.showAndGet()) {
          TwitterSettings settings = TwitterSettings.getInstance();
          try {
            boolean isAuthorized = !settings.getAccessToken().isEmpty();
            Twitter twitter = getTwitter();
            if (!isAuthorized) {
              authorizeAndUpdateStatus(twitter, panel);
            }
            else {
              setAuthInfoInTwitter(twitter, settings.getAccessToken(), settings.getTokenSecret());
              updateStatus(panel, twitter);
            }
          }
          catch (TwitterException | IOException e) {
            LOG.warn(e.getMessage());
            Messages.showErrorDialog("Status wasn't updated. Please, check internet connection and try again", "Twitter");
          }
        }
        else {
          LOG.warn("Panel is null");
        }
      }
    });
  }


  private static DialogWrapper.DoNotAskOption createDoNotAskOption() {
    return new DialogWrapper.DoNotAskOption() {
      @Override
      public boolean isToBeShown() {
        return true;
      }

      @Override
      public void setToBeShown(boolean toBeShown, int exitCode) {
        if (exitCode == DialogWrapper.CANCEL_EXIT_CODE || exitCode == DialogWrapper.OK_EXIT_CODE) {
          TwitterSettings.getInstance().setAskToTweet(toBeShown);
        }
      }

      @Override
      public boolean canBeHidden() {
        return true;
      }

      @Override
      public boolean shouldSaveOptionsOnCancel() {
        return true;
      }

      @NotNull
      @Override
      public String getDoNotShowMessage() {
        return EduCoreBundle.message("twitter.dialog.do.not.ask");
      }
    };
  }

  /**
   * Post on twitter media and text from panel
   * @param panel shown to user and used to provide data to post 
   */
  public static void updateStatus(TwitterUtils.TwitterDialogPanel panel, Twitter twitter) throws IOException, TwitterException {
    StatusUpdate update = new StatusUpdate(panel.getMessage());
    InputStream e = panel.getMediaSource();
    if (e != null) {
      File imageFile = FileUtil.createTempFile("twitter_media", panel.getMediaExtension());
      FileUtil.copy(e, new FileOutputStream(imageFile));
      update.media(imageFile);
    }

    twitter.updateStatus(update);
    BrowserUtil.browse("https://twitter.com/");
  }

  /**
   * Show twitter dialog, asking user to tweet about his achievements. Post tweet with provided by panel
   * media and text. 
   * As a result of succeeded tweet twitter website is opened in default browser.
   */
  public static void authorizeAndUpdateStatus(@NotNull final Twitter twitter,
                                              @NotNull final TwitterUtils.TwitterDialogPanel panel) throws TwitterException {
    RequestToken requestToken = twitter.getOAuthRequestToken();
    BrowserUtil.browse(requestToken.getAuthorizationURL());

    ApplicationManager.getApplication().invokeLater(() -> {
      String pin = createAndShowPinDialog();
      if (pin != null) {
        try {
          AccessToken token = twitter.getOAuthAccessToken(requestToken, pin);
          TwitterSettings settings = TwitterSettings.getInstance();
          settings.setAccessToken(token.getToken());
          settings.setTokenSecret(token.getTokenSecret());
          updateStatus(panel, twitter);
        }
        catch (TwitterException e) {
          if (e.getStatusCode() == HttpStatus.SC_UNAUTHORIZED) {
            LOG.warn("Unable to get the access token.");
            LOG.warn(e.getMessage());
          }
        }
        catch (IOException e) {
          LOG.warn(e.getMessage());
        }
      }
    });
  }

  public static String createAndShowPinDialog() {
    return Messages.showInputDialog("Twitter PIN:", "Twitter Authorization", null, "", new InputValidatorEx() {
      @Nullable
      @Override
      public String getErrorText(String inputString) {
        inputString = inputString.trim();
        if (inputString.isEmpty()) {
          return "PIN shouldn't be empty.";
        }
        if (!isNumeric(inputString)) {
          return "PIN should be numeric.";
        }
        return null;
      }

      @Override
      public boolean checkInput(String inputString) {
        return getErrorText(inputString) == null;
      }

      @Override
      public boolean canClose(String inputString) {
        return true;
      }
      
      private boolean isNumeric(@NotNull final String string) {
        for (char c: string.toCharArray()) {
          if (!StringUtil.isDecimalDigit(c)) {
            return false;
          }
        }
        return true;
      }
    });
  }


  /**
   * Dialog wrapper class with DoNotAsl option for asking user to tweet.
   * */
  private static class TwitterDialogWrapper extends DialogWrapper {
    private final TwitterUtils.TwitterDialogPanel myPanel;

    TwitterDialogWrapper(@Nullable Project project, @NotNull TwitterUtils.TwitterDialogPanel panel, DoNotAskOption doNotAskOption) {
      super(project);
      setTitle(EduCoreBundle.message("twitter.dialog.title"));
      setDoNotAskOption(doNotAskOption);
      setOKButtonText(EduCoreBundle.message("twitter.dialog.ok.action"));
      setResizable(true);
      Dimension preferredSize = panel.getPreferredSize();
      setSize((int) preferredSize.getHeight(), (int) preferredSize.getWidth());
      myPanel = panel;

      initValidation();
      init();
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
      return myPanel;
    }

    @Override
    protected @Nullable ValidationInfo doValidate() {
      return myPanel.doValidate();
    }
  }

  /**
   * Class provides structure for twitter dialog panel
   */
  public abstract static class TwitterDialogPanel extends JPanel {

    public TwitterDialogPanel(LayoutManager layout) {
      super(layout);
    }

    public TwitterDialogPanel() {
      super();
    }

    /**
     * Provides tweet text
     */
    @NotNull public abstract String getMessage();

    /**
     * @return Input stream of media should be posted or null if there's nothing to post 
     */
    @Nullable public abstract InputStream getMediaSource();
    
    @Nullable public abstract String getMediaExtension();

    @Nullable public ValidationInfo doValidate() {
      return null;
    }
    
  }
}
