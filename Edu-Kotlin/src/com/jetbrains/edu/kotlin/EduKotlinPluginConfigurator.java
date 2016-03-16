package com.jetbrains.edu.kotlin;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.DefaultLogger;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.components.JBScrollPane;
import com.jetbrains.edu.learning.StudyBasePluginConfigurator;
import com.jetbrains.edu.learning.StudyPluginConfigurator;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.StudyUtils;
import com.jetbrains.edu.learning.actions.*;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.StudyStatus;
import com.jetbrains.edu.learning.courseFormat.Task;
import com.jetbrains.edu.learning.settings.ModifiableSettingsPanel;
import com.jetbrains.edu.learning.twitter.StudyTwitterUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import twitter4j.StatusUpdate;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class EduKotlinPluginConfigurator extends StudyBasePluginConfigurator {
    private  static final Logger LOG = DefaultLogger.getInstance(EduKotlinPluginConfigurator.class);

    @NotNull
    @Override
    public DefaultActionGroup getActionGroup(Project project) {
        final DefaultActionGroup group = new DefaultActionGroup();
        EduKotlinCheckAction checkAction = new EduKotlinCheckAction();
        checkAction.getTemplatePresentation().setIcon(EduKotlinIcons.CHECK_TASK);
        group.add(checkAction);
        group.add(new StudyPreviousStudyTaskAction());
        group.add(new StudyNextStudyTaskAction());
        StudyRefreshTaskFileAction resetTaskFile = new StudyRefreshTaskFileAction();
        resetTaskFile.getTemplatePresentation().setIcon(EduKotlinIcons.RESET_TASK_FILE);
        group.add(resetTaskFile);
        StudyFillPlaceholdersAction fillPlaceholdersAction = new StudyFillPlaceholdersAction();
        fillPlaceholdersAction.getTemplatePresentation().setIcon(EduKotlinIcons.FILL_PLACEHOLDERS_ICON);
        fillPlaceholdersAction.getTemplatePresentation().setText("Fill Answer Placeholders");
        group.add(fillPlaceholdersAction);
        return group;
    }

    @NotNull
    @Override
    public String getDefaultHighlightingMode() {
        return "text/x-java";
    }

    @Override
    public boolean accept(@NotNull Project project) {
        StudyTaskManager instance = StudyTaskManager.getInstance(project);
        if (instance == null) return false;
        Course course = instance.getCourse();
        return course != null && "PyCharm".equals(course.getCourseType()) && "kotlin".equals(course.getLanguage());
    }

    @NotNull
    @Override
    public String getLanguageScriptUrl() {
        return getClass().getResource("/code_mirror/clike.js").toExternalForm();
    }

    @Nullable
    @Override
    public ModifiableSettingsPanel getSettingsPanel() {
        Project[] openProjects = ProjectManager.getInstance().getOpenProjects();
        for (Project project : openProjects) {
            if (StudyTaskManager.getInstance(project).getCourse() != null) {
                return new KotlinSettingsPanel(project);
            }
        }
        return null;
    }

    @Nullable
    @Override
    public StudyAfterCheckAction[] getAfterCheckActions() {
        StudyAfterCheckAction studyAfterCheckAction = new StudyAfterCheckAction() {

            @Override
            public void run(@NotNull Project project, @NotNull Task task, StudyStatus studyStatusBeforeCheck) {
                if (askToTweet(project, task, studyStatusBeforeCheck)) {
                    createTwitterDialogAndShow(project, task);
                }
            }
        };
        return new StudyAfterCheckAction[]{studyAfterCheckAction};
    }

    private void createTwitterDialogAndShow(@NotNull Project project, @NotNull Task task) {
        ApplicationManager.getApplication().invokeLater(() -> {
            DialogWrapper.DoNotAskOption doNotAskOption = createDoNotAskOption(project);
            if (doNotAskOption.isToBeShown()) {
                StudyTwitterUtils.TwitterDialogPanel panel = getTweetDialogPanel(task);
                if (panel != null) {
                    TwitterDialogWrapper wrapper = new TwitterDialogWrapper(project, panel, doNotAskOption);
                    wrapper.setTitle("Twitter");
                    wrapper.setResizable(true);
                    wrapper.setDoNotAskOption(doNotAskOption);

                    panel.addTextFieldVerifier(createTextFieldLengthDocumentListener(wrapper, panel));
                    if (wrapper.showAndGet()) {
                        try {
                            boolean isAuthorized = !getTwitterAccessToken(project).isEmpty();
                            Twitter twitter = StudyTwitterUtils.getTwitter(getConsumerKey(project), getConsumerSecret(project));
                            if (!isAuthorized) {
                                authorizeAndUpdate(project, twitter, panel);
                            }
                            else {
                                twitter.setOAuthAccessToken(new AccessToken(getTwitterAccessToken(project),
                                        getTwitterTokenSecret(project)));
                                updateStatus(panel, twitter);
                            }
                        } catch (TwitterException | IOException var6) {
                            LOG.warn(var6.getMessage());
                            Messages.showErrorDialog("Status wasn\'t updated. Please, check internet connection and try again", "Twitter");
                        }
                    } 
                }
                else {
                    LOG.warn("Panel is null");
                }
            }

        });
    }

    private static void authorizeAndUpdate(@NotNull final Project project, @NotNull final Twitter twitter,
                                           @NotNull final StudyTwitterUtils.TwitterDialogPanel panel) throws TwitterException {
        RequestToken requestToken = twitter.getOAuthRequestToken();
        BrowserUtil.browse(requestToken.getAuthorizationURL());

        ApplicationManager.getApplication().invokeLater(() -> {
            String pin = Messages.showInputDialog("Twitter PIN:", "Twitter Authorization", null, "", null);
            try {
                AccessToken token;
                if (pin != null && pin.length() > 0) {
                    token = twitter.getOAuthAccessToken(requestToken, pin);
                }
                else {
                    token = twitter.getOAuthAccessToken();
                }
                StudyPluginConfigurator configurator = StudyUtils.getConfigurator(project);
                if (configurator != null) {
                    configurator.storeTwitterTokens(project, token.getToken(), token.getTokenSecret());
                    updateStatus(panel, twitter);
                    
                }
                else {
                    LOG.warn("Plugin configurator not found");
                }
            }
            catch (TwitterException e) {
                if (401 == e.getStatusCode()) {
                    LOG.warn("Unable to get the access token.");
                }
                else {
                    LOG.warn(e.getMessage());
                }
            } catch (IOException e) {
                LOG.warn(e.getMessage());
            }
        });
    }

    private static void updateStatus(StudyTwitterUtils.TwitterDialogPanel panel, Twitter twitter) throws IOException, TwitterException {
        StatusUpdate update = new StatusUpdate(panel.getMessage());
        InputStream e = panel.getMediaSource();
        if (e != null) {
            File imageFile = FileUtil.createTempFile("twitter_media", "gif");
            FileUtil.copy(e, new FileOutputStream(imageFile));
            update.media(imageFile);
        }

        twitter.updateStatus(update);
        BrowserUtil.browse("https://twitter.com/");
    }

    private class TwitterDialogWrapper extends DialogWrapper {
        private StudyTwitterUtils.TwitterDialogPanel myPanel;

        TwitterDialogWrapper(@Nullable Project project, @NotNull StudyTwitterUtils.TwitterDialogPanel panel, DoNotAskOption doNotAskOption) {
            super(project);
            setDoNotAskOption(doNotAskOption);
            setOKButtonText("Tweet");
            setCancelButtonText("No");
            Dimension preferredSize = panel.getPreferredSize();
            setSize((int) preferredSize.getHeight(), (int) preferredSize.getWidth());
            myPanel = panel;
            init();
        }

        public void setOKActionEnabled(boolean isEnabled) {
            super.setOKActionEnabled(isEnabled);
        }

        @Nullable
        @Override
        protected JComponent createCenterPanel() {
            return new JBScrollPane(myPanel);
        }
    }

    private DialogWrapper.DoNotAskOption createDoNotAskOption(@NotNull Project project) {
        return new DialogWrapper.DoNotAskOption() {
            @Override
            public boolean isToBeShown() {
                return KotlinStudyTwitterSettings.getInstance(project).askToTweet();
            }

            @Override
            public void setToBeShown(boolean toBeShown, int exitCode) {
                if (exitCode == DialogWrapper.CANCEL_EXIT_CODE || exitCode == DialogWrapper.OK_EXIT_CODE) {
                    KotlinStudyTwitterSettings.getInstance(project).setAskToTweet(toBeShown);
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
                return "Never ask me to tweet";
            }
        };
    }

    @NotNull
    @Override
    public String getConsumerKey(@NotNull Project project) {
        return KotlinTwitterBundle.message("consumerKey");
    }

    @NotNull
    @Override
    public String getConsumerSecret(@NotNull Project project) {
        return KotlinTwitterBundle.message("consumerSecret");
    }


    @Override
    public void storeTwitterTokens(@NotNull Project project, @NotNull String accessToken, @NotNull String tokenSecret) {
        KotlinStudyTwitterSettings kotlinStudyTwitterSettings = KotlinStudyTwitterSettings.getInstance(project);
        kotlinStudyTwitterSettings.setAccessToken(accessToken);
        kotlinStudyTwitterSettings.setTokenSecret(tokenSecret);
    }

    @NotNull
    @Override
    public String getTwitterTokenSecret(@NotNull Project project) {
        KotlinStudyTwitterSettings kotlinStudyTwitterSettings = KotlinStudyTwitterSettings.getInstance(project);
        return kotlinStudyTwitterSettings.getTokenSecret();
    }

    @NotNull
    @Override
    public String getTwitterAccessToken(@NotNull Project project) {
        KotlinStudyTwitterSettings kotlinStudyTwitterSettings = KotlinStudyTwitterSettings.getInstance(project);
        return kotlinStudyTwitterSettings.getAccessToken();
    }

    @Override
    public boolean askToTweet(@NotNull Project project, Task solvedTask, StudyStatus statusBeforeCheck) {
        StudyTaskManager taskManager = StudyTaskManager.getInstance(project);
        Course course = taskManager.getCourse();
        if (course != null && course.getName().equals("Kotlin Koans")) {
            KotlinStudyTwitterSettings kotlinStudyTwitterSettings = KotlinStudyTwitterSettings.getInstance(project);
            return kotlinStudyTwitterSettings.askToTweet()
                    && solvedTask.getStatus() == StudyStatus.Solved
                    && (statusBeforeCheck == StudyStatus.Unchecked || statusBeforeCheck == StudyStatus.Failed)
                    && KotlinUtils.calculateTaskNumber(solvedTask) % 8 == 0;
        }
        return false;
    }

    @Nullable
    @Override
    public StudyTwitterUtils.TwitterDialogPanel getTweetDialogPanel(@NotNull Task solvedTask) {
        return new KotlinTwitterDialogPanel(solvedTask);
    }

    private static DocumentListener createTextFieldLengthDocumentListener(@NotNull TwitterDialogWrapper builder, @NotNull final StudyTwitterUtils.TwitterDialogPanel panel) {
        return new DocumentAdapter() {
            @Override
            protected void textChanged(DocumentEvent e) {
                int length = e.getDocument().getLength();
                if (length > 140 || length == 0) {
                    builder.setOKActionEnabled(false);
                    panel.getRemainSymbolsLabel().setText("<html><font color='red'>" + String.valueOf(140 - length) + "</font></html>");
                } else {
                    builder.setOKActionEnabled(true);
                    panel.getRemainSymbolsLabel().setText(String.valueOf(140 - length));
                }

            }
        };
    }
}
