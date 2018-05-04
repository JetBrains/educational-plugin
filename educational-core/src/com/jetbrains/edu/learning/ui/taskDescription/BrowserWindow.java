package com.jetbrains.edu.learning.ui.taskDescription;

import com.intellij.icons.AllIcons;
import com.intellij.ide.BrowserUtil;
import com.intellij.ide.ui.LafManager;
import com.intellij.ide.ui.LafManagerListener;
import com.intellij.ide.ui.laf.darcula.DarculaLookAndFeelInfo;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.colors.FontPreferences;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.io.StreamUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.jetbrains.edu.learning.EduLanguageDecorator;
import com.jetbrains.edu.learning.EduUtils;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import com.jetbrains.edu.learning.navigation.NavigationUtils;
import com.jetbrains.edu.learning.statistics.EduUsagesCollector;
import com.sun.webkit.dom.ElementImpl;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebHistory;
import javafx.scene.web.WebView;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;
import org.jsoup.Jsoup;
import org.jsoup.select.Elements;
import org.w3c.dom.*;
import org.w3c.dom.events.Event;
import org.w3c.dom.events.EventListener;
import org.w3c.dom.events.EventTarget;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.jetbrains.edu.learning.stepik.StepikNames.STEPIK_URL;

public class BrowserWindow extends JFrame {
  private static final Logger LOG = Logger.getInstance(TaskDescriptionToolWindow.class);
  private static final String EVENT_TYPE_CLICK = "click";
  private static final Pattern IN_COURSE_LINK = Pattern.compile("#(\\w+)#(\\w+)#((\\w+)#)?");

  private static final double BODY_FONT_SIZE_MAC = 13.0;
  private static final double BODY_FONT_SIZE = 14.0;
  private static final double FONT_SCALE_MAC = BODY_FONT_SIZE_MAC / FontPreferences.DEFAULT_FONT_SIZE;
  private static final double FONT_SCALE = BODY_FONT_SIZE / FontPreferences.DEFAULT_FONT_SIZE;

  private static final double CODE_FONT_SIZE_MAC = 13.0;
  private static final double CODE_FONT_SIZE = 14.0;
  private static final double CODE_FONT_SCALE_MAC = CODE_FONT_SIZE_MAC / FontPreferences.DEFAULT_FONT_SIZE;
  private static final double CODE_FONT_SCALE = CODE_FONT_SIZE / FontPreferences.DEFAULT_FONT_SIZE;

  private static final double LINE_HEIGHT_MAC = 20.0;
  private static final double LINE_HEIGHT = 24.0;
  private static final double LINE_HEIGHT_SCALE_MAC = LINE_HEIGHT_MAC / BODY_FONT_SIZE_MAC;
  private static final double LINE_HEIGHT_SCALE = LINE_HEIGHT/ BODY_FONT_SIZE;

  private static final double CODE_LINE_HEIGHT_MAC = 16.0;
  private static final double CODE_LINE_HEIGHT = 20.0;
  private static final double CODE_LINE_HEIGHT_SCALE_MAC = CODE_LINE_HEIGHT_MAC / CODE_FONT_SIZE_MAC;
  private static final double CODE_LINE_HEIGHT_SCALE = CODE_LINE_HEIGHT / CODE_FONT_SIZE;

  public static final String SRC_ATTRIBUTE = "src";
  private JFXPanel myPanel;
  private WebView myWebComponent;
  private StackPane myPane;

  private WebEngine myEngine;
  private ProgressBar myProgressBar;
  private final Project myProject;
  private boolean myLinkInNewBrowser;
  private boolean myShowProgress;

  public BrowserWindow(@NotNull final Project project, final boolean linkInNewWindow, final boolean showProgress) {
    myProject = project;
    myLinkInNewBrowser = linkInNewWindow;
    myShowProgress = showProgress;
    setSize(new Dimension(900, 800));
    setLayout(new BorderLayout());
    setPanel(new JFXPanel());
    setTitle("Study Browser");
    LafManager.getInstance().addLafManagerListener(new StudyLafManagerListener());
    initComponents();
  }

  private void updateLaf(boolean isDarcula) {
    if (isDarcula) {
      updateLafDarcula();
    }
    else {
      updateIntellijAndGTKLaf();
    }
  }

  private void updateIntellijAndGTKLaf() {
    Platform.runLater(() -> {
      final URL scrollBarStyleUrl = getClass().getResource("/style/javaFXBrowserScrollBar.css");
      final URL engineStyleUrl = getClass().getResource(getBrowserStylesheet(false));
      myPane.getStylesheets().add(scrollBarStyleUrl.toExternalForm());
      myEngine.setUserStyleSheetLocation(engineStyleUrl.toExternalForm());
      myPanel.getScene().getStylesheets().add(engineStyleUrl.toExternalForm());
      myEngine.reload();
    });
  }

  private void updateLafDarcula() {
    Platform.runLater(() -> {
      final URL engineStyleUrl = getClass().getResource(getBrowserStylesheet(true));
      final URL scrollBarStyleUrl = getClass().getResource("/style/javaFXBrowserDarculaScrollBar.css");
      myEngine.setUserStyleSheetLocation(engineStyleUrl.toExternalForm());
      myPane.getStylesheets().add(scrollBarStyleUrl.toExternalForm());
      myPane.setStyle("-fx-background-color: #3c3f41");
      myPanel.getScene().getStylesheets().add(engineStyleUrl.toExternalForm());
      myEngine.reload();
    });
  }

  @NotNull
  private static String getBrowserStylesheet(boolean isDarcula) {
    if (SystemInfo.isMac) {
      return isDarcula ? "style/javaFXBrowserDarcula_mac.css" : "/style/javaFXBrowser_mac.css";
    }

    if (SystemInfo.isWindows) {
      return isDarcula ? "style/javaFXBrowserDarcula_win.css" : "/style/javaFXBrowser_win.css";
    }

    return isDarcula ? "style/javaFXBrowserDarcula_linux.css" : "/style/javaFXBrowser_linux.css";
  }

  private void initComponents() {
    Platform.runLater(() -> {
      Platform.setImplicitExit(false);
      myPane = new StackPane();
      myWebComponent = new WebView();
      myWebComponent.setOnDragDetected(event -> {});
      myEngine = myWebComponent.getEngine();


      if (myShowProgress) {
        myProgressBar = makeProgressBarWithListener();
        myWebComponent.setVisible(false);
        myPane.getChildren().addAll(myWebComponent, myProgressBar);
      }
      else {
        myPane.getChildren().add(myWebComponent);
      }
      if (myLinkInNewBrowser) {
        initHyperlinkListener();
      }
      Scene scene = new Scene(myPane);
      myPanel.setScene(scene);
      myPanel.setVisible(true);
      updateLaf(LafManager.getInstance().getCurrentLookAndFeel() instanceof DarculaLookAndFeelInfo);
    });

    add(myPanel, BorderLayout.CENTER);
    setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
  }

  public void loadContent(@NotNull final String content) {
    Course course = StudyTaskManager.getInstance(myProject).getCourse();
    if (course == null) {
      return;
    }

    Task task = EduUtils.getCurrentTask(myProject);
    if (task == null) {
      Platform.runLater(() -> myEngine.loadContent(content));
      return;
    }

    VirtualFile taskDir = task.getTaskDir(myProject);
    if (taskDir == null) {
      Platform.runLater(() -> {
        updateLookWithProgressBarIfNeeded();
        myEngine.loadContent(content);
      });
      return;
    }

    Platform.runLater(() -> {
      updateLookWithProgressBarIfNeeded();
      myEngine.loadContent(doProcessContent(content, taskDir, myProject));
    });
  }

  @TestOnly
  public static String processContent(@NotNull String content, @NotNull VirtualFile taskDir, Project project) {
    return doProcessContent(content, taskDir, project);
  }

  private static String doProcessContent(@NotNull String content, @NotNull VirtualFile taskDir, Project project) {
    Course course = StudyTaskManager.getInstance(project).getCourse();
    if (course == null) {
      return content;
    }

    String text = content;
    EduLanguageDecorator decorator = EduLanguageDecorator.INSTANCE.forLanguage(course.getLanguageById());
    if (decorator != null) {
      text = createHtmlWithCodeHighlighting(content, decorator.getLanguageScriptUrl(), decorator.getDefaultHighlightingMode());
    }

    return absolutizeImgPaths(text, taskDir);
  }

  @NotNull
  private static String absolutizeImgPaths(@NotNull String withCodeHighlighting, @NotNull VirtualFile taskDir) {
    org.jsoup.nodes.Document document = Jsoup.parse(withCodeHighlighting);
    Elements imageElements = document.getElementsByTag("img");
    for (org.jsoup.nodes.Element imageElement : imageElements) {
      String imagePath = imageElement.attr(SRC_ATTRIBUTE);
      if (!BrowserUtil.isAbsoluteURL(imagePath)) {
        File file = new File(imagePath);
        String absolutePath = new File(taskDir.getPath(), file.getPath()).toURI().toString();
        imageElement.attr("src", absolutePath);
      }
    }
    return document.outerHtml();
  }
  @NotNull
  private static String createHtmlWithCodeHighlighting(@NotNull final String content,
                                                @NotNull String languageScriptUrl,
                                                @NotNull String defaultHighlightingMode) {
    String template = null;
    ClassLoader classLoader = BrowserWindow.class.getClassLoader();
    InputStream stream = classLoader.getResourceAsStream("/code-mirror/template.html");
    try {
      template = StreamUtil.readText(stream, "utf-8");
    }
    catch (IOException e) {
      LOG.warn(e.getMessage());
    }
    finally {
      try {
        stream.close();
      }
      catch (IOException e) {
        LOG.warn(e.getMessage());
      }
    }

    if (template == null) {
      LOG.warn("Code mirror template is null");
      return content;
    }

    final EditorColorsScheme editorColorsScheme = EditorColorsManager.getInstance().getGlobalScheme();
    int bodyFontSize = (int)(editorColorsScheme.getEditorFontSize() * (SystemInfo.isMac ? FONT_SCALE_MAC : FONT_SCALE));
    int codeFontSize = (int)(editorColorsScheme.getEditorFontSize() * (SystemInfo.isMac ? CODE_FONT_SCALE_MAC : CODE_FONT_SCALE));

    int bodyLineHeight = (int)(editorColorsScheme.getEditorFontSize() * (SystemInfo.isMac ? LINE_HEIGHT_SCALE_MAC : LINE_HEIGHT_SCALE));
    int codeLineHeight = (int)(editorColorsScheme.getEditorFontSize() * (SystemInfo.isMac ? CODE_LINE_HEIGHT_SCALE_MAC : CODE_LINE_HEIGHT_SCALE));

    template = template.replace("${body_font_size}", String.valueOf(bodyFontSize));
    template = template.replace("${code_font_size}", String.valueOf(codeFontSize));
    template = template.replace("${body_line_height}", String.valueOf(bodyLineHeight));
    template = template.replace("${code_line_height}", String.valueOf(codeLineHeight));
    template = template.replace("${codemirror}", classLoader.getResource("/code-mirror/codemirror.js").toExternalForm());
    template = template.replace("${language_script}", languageScriptUrl);
    template = template.replace("${default_mode}", defaultHighlightingMode);
    template = template.replace("${runmode}", classLoader.getResource("/code-mirror/runmode.js").toExternalForm());
    template = template.replace("${colorize}", classLoader.getResource("/code-mirror/colorize.js").toExternalForm());
    template = template.replace("${javascript}", classLoader.getResource("/code-mirror/javascript.js").toExternalForm());
    if (LafManager.getInstance().getCurrentLookAndFeel() instanceof DarculaLookAndFeelInfo) {
      template = template.replace("${css_oldcodemirror}", classLoader.getResource("/code-mirror/codemirror-old-darcula.css").toExternalForm());
      template = template.replace("${css_codemirror}", classLoader.getResource("/code-mirror/codemirror-darcula.css").toExternalForm());
    }
    else {
      template = template.replace("${css_oldcodemirror}", classLoader.getResource("/code-mirror/codemirror-old.css").toExternalForm());
      template = template.replace("${css_codemirror}", classLoader.getResource("/code-mirror/codemirror.css").toExternalForm());
    }
    template = template.replace("${code}", content);

    return template;
  }

  private void updateLookWithProgressBarIfNeeded() {
    if (myShowProgress) {
      myProgressBar.setVisible(true);
      myWebComponent.setVisible(false);
    }
  }

  private void initHyperlinkListener() {
    myEngine.getLoadWorker().stateProperty().addListener((ov, oldState, newState) -> {
      if (newState == Worker.State.SUCCEEDED) {
        final EventListener listener = makeHyperLinkListener();

        addListenerToAllHyperlinkItems(listener);
      }
    });
  }

  private void addListenerToAllHyperlinkItems(EventListener listener) {
    final Document doc = myEngine.getDocument();
    if (doc != null) {
      final NodeList nodeList = doc.getElementsByTagName("a");
      for (int i = 0; i < nodeList.getLength(); i++) {
        ((EventTarget)nodeList.item(i)).addEventListener(EVENT_TYPE_CLICK, listener, false);
      }
    }
  }

  @NotNull
  private EventListener makeHyperLinkListener() {
    return new EventListener() {
      @Override
      public void handleEvent(Event ev) {
        String domEventType = ev.getType();
        if (domEventType.equals(EVENT_TYPE_CLICK)) {
          ev.preventDefault();
          Element target = (Element)ev.getTarget();
          String hrefAttribute = getElementWithATag(target).getAttribute("href");

          if (hrefAttribute != null) {
            final Matcher matcher = IN_COURSE_LINK.matcher(hrefAttribute);
            if (matcher.matches()) {
              EduUsagesCollector.inCourseLinkClicked();
              String sectionName = null;
              String lessonName;
              String taskName;
              if (matcher.group(3) != null) {
                sectionName = matcher.group(1);
                lessonName = matcher.group(2);
                taskName = matcher.group(4);
              }
              else {
                lessonName = matcher.group(1);
                taskName = matcher.group(2);
              }
              NavigationUtils.navigateToTask(myProject, sectionName, lessonName, taskName);
            }
            else {
              EduUsagesCollector.externalLinkClicked();
              myEngine.setJavaScriptEnabled(true);
              myEngine.getLoadWorker().cancel();
              String href = getLink(target);
              if (href == null) return;
              if (isRelativeLink(href)) {
                href = STEPIK_URL + href;
              }
              BrowserUtil.browse(href);
              if (href.startsWith(STEPIK_URL)) {
                EduUsagesCollector.stepikLinkClicked();
              }

            }
          }
        }
      }

      private boolean isRelativeLink(@NotNull String href) {
        return !href.startsWith("http");
      }

      private Element getElementWithATag(Element element) {
        Element currentElement = element;
        while (!currentElement.getTagName().toLowerCase(Locale.ENGLISH).equals("a")) {
          currentElement = ((ElementImpl)currentElement).getParentElement();
        }
        return currentElement;
      }

      @Nullable
      private String getLink(@NotNull Element element) {
        final String href = element.getAttribute("href");
        return href == null ? getLinkFromNodeWithCodeTag(element) : href;
      }

      @Nullable
      private String getLinkFromNodeWithCodeTag(@NotNull Element element) {
        Node parentNode = element.getParentNode();
        NamedNodeMap attributes = parentNode.getAttributes();
        while (attributes.getLength() > 0 && attributes.getNamedItem("class") != null) {
          parentNode = parentNode.getParentNode();
          attributes = parentNode.getAttributes();
        }
        return attributes.getNamedItem("href").getNodeValue();
      }
    };
  }

  public void addBackAndOpenButtons() {
    ApplicationManager.getApplication().invokeLater(() -> {
      final JPanel panel = new JPanel();
      panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));

      final JButton backButton = makeGoButton("Click to go back", AllIcons.Actions.Back, -1);
      final JButton forwardButton = makeGoButton("Click to go forward", AllIcons.Actions.Forward, 1);
      final JButton openInBrowser = new JButton(AllIcons.Actions.Browser_externalJavaDoc);
      openInBrowser.addActionListener(e -> BrowserUtil.browse(myEngine.getLocation()));
      openInBrowser.setToolTipText("Click to open link in browser");
      addButtonsAvailabilityListeners(backButton, forwardButton);

      panel.setMaximumSize(new Dimension(40, getPanel().getHeight()));
      panel.add(backButton);
      panel.add(forwardButton);
      panel.add(openInBrowser);

      add(panel, BorderLayout.PAGE_START);
    });
  }

  private void addButtonsAvailabilityListeners(JButton backButton, JButton forwardButton) {
    Platform.runLater(() -> myEngine.getLoadWorker().stateProperty().addListener((ov, oldState, newState) -> {
      if (newState == Worker.State.SUCCEEDED) {
        final WebHistory history = myEngine.getHistory();
        boolean isGoBackAvailable = history.getCurrentIndex() > 0;
        boolean isGoForwardAvailable = history.getCurrentIndex() < history.getEntries().size() - 1;
        ApplicationManager.getApplication().invokeLater(() -> {
          backButton.setEnabled(isGoBackAvailable);
          forwardButton.setEnabled(isGoForwardAvailable);
        });
      }
    }));
  }

  private JButton makeGoButton(@NotNull final String toolTipText, @NotNull final Icon icon, final int direction) {
    final JButton button = new JButton(icon);
    button.setEnabled(false);
    button.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() == 1) {
          Platform.runLater(() -> myEngine.getHistory().go(direction));
        }
      }
    });
    button.setToolTipText(toolTipText);
    return button;
  }


  private ProgressBar makeProgressBarWithListener() {
    final ProgressBar progress = new ProgressBar();
    progress.progressProperty().bind(myWebComponent.getEngine().getLoadWorker().progressProperty());

    myWebComponent.getEngine().getLoadWorker().stateProperty().addListener(
      (ov, oldState, newState) -> {
        if (myWebComponent.getEngine().getLocation().contains("http") && newState == Worker.State.SUCCEEDED) {
          myProgressBar.setVisible(false);
          myWebComponent.setVisible(true);
        }
      });

    return progress;
  }

  public JFXPanel getPanel() {
    return myPanel;
  }

  private void setPanel(JFXPanel panel) {
    myPanel = panel;
  }

  private class StudyLafManagerListener implements LafManagerListener {
    @Override
    public void lookAndFeelChanged(LafManager manager) {
      updateLaf(manager.getCurrentLookAndFeel() instanceof DarculaLookAndFeelInfo);
    }
  }
}
