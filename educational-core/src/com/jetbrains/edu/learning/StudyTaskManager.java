package com.jetbrains.edu.learning;

import com.intellij.ide.fileTemplates.FileTemplate;
import com.intellij.ide.fileTemplates.FileTemplateManager;
import com.intellij.ide.fileTemplates.FileTemplateUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.*;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiManager;
import com.intellij.util.containers.hash.HashMap;
import com.intellij.util.messages.Topic;
import com.intellij.util.xmlb.XmlSerializer;
import com.intellij.util.xmlb.annotations.Transient;
import com.jetbrains.edu.learning.courseFormat.*;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import com.jetbrains.edu.learning.serialization.SerializationUtils;
import com.jetbrains.edu.learning.serialization.StudyUnrecognizedFormatException;
import org.jdom.Element;
import org.jdom.output.XMLOutputter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.jetbrains.edu.learning.serialization.SerializationUtils.Xml.REMOTE_COURSE;

/**
 * Implementation of class which contains all the information
 * about study in context of current project
 */

@State(name = "StudySettings", storages = @Storage(value = "study_project.xml", roamingType = RoamingType.DISABLED))
public class StudyTaskManager implements PersistentStateComponent<Element>, DumbAware {
  public static final Topic<CourseSetListener> COURSE_SET = Topic.create("Edu.courseSet", CourseSetListener.class);
  private static final Logger LOG = Logger.getInstance(StudyTaskManager.class);

  private Course myCourse;
  public int VERSION = EduVersions.XML_FORMAT_VERSION;

  public final Map<Task, List<UserTest>> myUserTests = new HashMap<>();

  @Transient private final Project myProject;

  public StudyTaskManager(Project project) {
    myProject = project;
  }

  public StudyTaskManager() {
    this(null);
  }

  public void setCourse(Course course) {
    myCourse = course;
    myProject.getMessageBus().syncPublisher(COURSE_SET).courseSet(course);
  }

  @Nullable
  public Course getCourse() {
    return myCourse;
  }

  public void setStatus(AnswerPlaceholder placeholder, CheckStatus status) {
    placeholder.setStatus(status);
  }

  public void addUserTest(@NotNull final Task task, UserTest userTest) {
    List<UserTest> userTests = myUserTests.get(task);
    if (userTests == null) {
      userTests = new ArrayList<>();
      myUserTests.put(task, userTests);
    }
    userTests.add(userTest);
  }

  public void setUserTests(@NotNull final Task task, @NotNull final List<UserTest> userTests) {
    myUserTests.put(task, userTests);
  }

  @NotNull
  public List<UserTest> getUserTests(@NotNull final Task task) {
    final List<UserTest> userTests = myUserTests.get(task);
    return userTests != null ? userTests : Collections.emptyList();
  }

  public void removeUserTest(@NotNull final Task task, @NotNull final UserTest userTest) {
    final List<UserTest> userTests = myUserTests.get(task);
    if (userTests != null) {
      userTests.remove(userTest);
    }
  }

  public boolean hasFailedAnswerPlaceholders(@NotNull final TaskFile taskFile) {
    return taskFile.getAnswerPlaceholders().size() > 0 && taskFile.hasFailedPlaceholders();
  }

  @Nullable
  @Override
  public Element getState() {
    if (myCourse == null) {
      return null;
    }

    return serialize();
  }

  @NotNull
  private Element serialize() {
    Element el = new Element("taskManager");
    Element taskManagerElement = new Element(SerializationUtils.Xml.MAIN_ELEMENT);
    XmlSerializer.serializeInto(this, taskManagerElement);

    if (myCourse instanceof RemoteCourse) {
      try {
        Element course = new Element(REMOTE_COURSE);
        //noinspection RedundantCast
        XmlSerializer.serializeInto((RemoteCourse)myCourse, course);

        final Element xmlCourse = SerializationUtils.Xml.getChildWithName(taskManagerElement, SerializationUtils.COURSE);
        xmlCourse.removeContent();
        xmlCourse.addContent(course);
      }
      catch (StudyUnrecognizedFormatException e) {
        LOG.error("Failed to serialize remote course");
      }
    }

    el.addContent(taskManagerElement);
    return el;
  }

  @Override
  public void loadState(@NotNull Element state) {
    try {
      int version = SerializationUtils.Xml.getVersion(state);
      if (version == -1) {
        LOG.error("StudyTaskManager doesn't contain any version:\n" + state.getValue());
        return;
      }
      switch (version) {
        case 1:
          state = SerializationUtils.Xml.convertToSecondVersion(myProject, state);
        case 2:
          state = SerializationUtils.Xml.convertToThirdVersion(myProject, state);
        case 3:
          state = SerializationUtils.Xml.convertToFourthVersion(myProject, state);
        case 4:
          state = SerializationUtils.Xml.convertToFifthVersion(myProject, state);
          updateTestHelper();
        case 5:
          state = SerializationUtils.Xml.convertToSixthVersion(myProject, state);
        case 6:
          state = SerializationUtils.Xml.convertToSeventhVersion(myProject, state);
        case 7:
          state = SerializationUtils.Xml.convertToEighthVersion(myProject, state);
        case 8:
          state = SerializationUtils.Xml.convertToNinthVersion(myProject, state);
          // uncomment for future versions
          //case 9:
          // state = SerializationUtils.Xml.convertToTenthVersion(myProject, state);
      }
      deserialize(state);
      VERSION = EduVersions.XML_FORMAT_VERSION;
      if (myCourse != null) {
        myCourse.init(null, null, true);
      }
    }
    catch (StudyUnrecognizedFormatException e) {
      LOG.error("Unexpected course format:\n", new XMLOutputter().outputString(state));
    }
  }

  private void updateTestHelper() {
    StartupManager.getInstance(myProject).runWhenProjectIsInitialized(() -> ApplicationManager.getApplication().runWriteAction(() -> {
      final VirtualFile baseDir = myProject.getBaseDir();
      if (baseDir == null) {
        return;
      }
      final VirtualFile testHelper = baseDir.findChild(EduNames.TEST_HELPER);
      if (testHelper != null) {
        EduUtils.deleteFile(testHelper);
      }
      final FileTemplate template =
        FileTemplateManager.getInstance(myProject).getInternalTemplate(FileUtil.getNameWithoutExtension(EduNames.TEST_HELPER));
      try {
        final PsiDirectory projectDir = PsiManager.getInstance(myProject).findDirectory(baseDir);
        if (projectDir != null) {
          FileTemplateUtil.createFromTemplate(template, EduNames.TEST_HELPER, null, projectDir);
        }
      }
      catch (Exception e) {
        LOG.warn("Failed to create new test helper");
      }
    }));
  }

  private void deserialize(Element state) throws StudyUnrecognizedFormatException {
    final Element taskManagerElement = state.getChild(SerializationUtils.Xml.MAIN_ELEMENT);
    if (taskManagerElement == null) {
      throw new StudyUnrecognizedFormatException();
    }
    XmlSerializer.deserializeInto(this, taskManagerElement);
    final Element xmlCourse = SerializationUtils.Xml.getChildWithName(taskManagerElement, SerializationUtils.COURSE);
    final Element remoteCourseElement = xmlCourse.getChild(REMOTE_COURSE);
    if (remoteCourseElement != null) {
      final RemoteCourse remoteCourse = new RemoteCourse();
      XmlSerializer.deserializeInto(remoteCourse, remoteCourseElement);
      myCourse = remoteCourse;
    }
  }

  public static StudyTaskManager getInstance(@NotNull final Project project) {
    return ServiceManager.getService(project, StudyTaskManager.class);
  }
}
