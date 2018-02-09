package com.jetbrains.edu.coursecreator.actions;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.intellij.ide.projectView.ProjectView;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.util.containers.hash.HashMap;
import com.intellij.util.io.ZipUtil;
import com.jetbrains.edu.coursecreator.CCUtils;
import com.jetbrains.edu.coursecreator.ui.CCCreateCourseArchiveDialog;
import com.jetbrains.edu.learning.EduNames;
import com.jetbrains.edu.learning.EduUtils;
import com.jetbrains.edu.learning.serialization.SerializationUtils;
import com.jetbrains.edu.learning.StudyTaskManager;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.Lesson;
import com.jetbrains.edu.learning.courseFormat.TaskFile;
import com.jetbrains.edu.learning.courseFormat.ext.TaskExt;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import com.jetbrains.edu.learning.statistics.EduUsagesCollector;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.ZipOutputStream;

public class CCCreateCourseArchive extends DumbAwareAction {
  private static final Logger LOG = Logger.getInstance(CCCreateCourseArchive.class.getName());

  public static final String GENERATE_COURSE_ARCHIVE = "&Generate Course Archive";
  public static final String LAST_ARCHIVE_LOCATION = "Edu.CourseCreator.LastArchiveLocation";

  private String myZipName;
  private String myLocationDir;

  public void setZipName(String zipName) {
    myZipName = zipName;
  }

  public void setLocationDir(String locationDir) {
    myLocationDir = locationDir;
  }

  public CCCreateCourseArchive() {
    super(GENERATE_COURSE_ARCHIVE, GENERATE_COURSE_ARCHIVE, null);
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    Presentation presentation = e.getPresentation();
    Project project = e.getProject();
    presentation.setEnabledAndVisible(project != null && CCUtils.isCourseCreator(project));
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    final Project project = e.getData(CommonDataKeys.PROJECT);
    if (project == null) return;
    final VirtualFile baseDir = project.getBaseDir();
    if (baseDir == null) return;
    Module module = ModuleUtilCore.findModuleForFile(baseDir, project);
    if (module == null) return;

    CCCreateCourseArchiveDialog dlg = new CCCreateCourseArchiveDialog(project, this);
    dlg.show();
    if (dlg.getExitCode() != DialogWrapper.OK_EXIT_CODE) {
      return;
    }
    boolean isSuccessful = createCourseArchive(project, module, myZipName, myLocationDir, true);
    if (isSuccessful) {
      PropertiesComponent.getInstance(project).setValue(LAST_ARCHIVE_LOCATION, myLocationDir);
      EduUsagesCollector.createdCourseArchive();
    } else {
      Messages.showErrorDialog("Can not create archive for current course", "Failed to Create Course Archive");
    }
  }

  /**
   * @return true if course archive was created successfully, false otherwise
   */
  public static boolean createCourseArchive(final Project project, Module module, String zipName, String locationDir, boolean showMessage) {
    final Course course = StudyTaskManager.getInstance(project).getCourse();
    if (course == null) return false;
    final VirtualFile baseDir = project.getBaseDir();
    VirtualFile archiveFolder = CCUtils.generateFolder(project, module, zipName);
    if (archiveFolder == null) {
      return false;
    }
    FileDocumentManager.getInstance().saveAllDocuments();

    final Ref<Boolean> isCreationSuccessful = Ref.create();
    ApplicationManager.getApplication().runWriteAction(new Runnable() {
      @Override
      public void run() {
        archiveFolder.refresh(false, true);
        Course courseCopy = course.copy();
        replaceAnswerFilesWithTaskFiles(courseCopy);
        courseCopy.sortLessons();
        createAdditionalFiles(courseCopy);
        try {
          generateJson(archiveFolder, courseCopy);
          VirtualFileManager.getInstance().refreshWithoutFileWatcher(false);
          packCourse(archiveFolder, locationDir, zipName, showMessage);
          synchronize(project);
          isCreationSuccessful.set(true);
        } catch (IOException e) {
          LOG.error("Failed to create course archive", e);
          isCreationSuccessful.set(false);
        }
      }

      private void createAdditionalFiles(Course course) {
        final Lesson lesson = CCUtils.createAdditionalLesson(course, project, EduNames.ADDITIONAL_MATERIALS);
        if (lesson != null) {
          course.addLesson(lesson);
        }
      }

      private void replaceAnswerFilesWithTaskFiles(Course courseCopy) {
        for (Lesson lesson : courseCopy.getLessons()) {
          String lessonDirName = EduNames.LESSON + String.valueOf(lesson.getIndex());
          final VirtualFile lessonDir = baseDir.findChild(lessonDirName);
          if (lessonDir == null) continue;
          for (Task task : lesson.getTaskList()) {
            final VirtualFile taskDir = task.getTaskDir(project);
            if (taskDir == null) continue;
            convertToStudentTaskFiles(task, taskDir);
            addTestsToTask(task);
            addDescriptions(task);
          }
        }
      }

      private void convertToStudentTaskFiles(Task task, VirtualFile taskDir) {
        final HashMap<String, TaskFile> studentTaskFiles = new HashMap<>();
        for (Map.Entry<String, TaskFile> entry : task.getTaskFiles().entrySet()) {
          VirtualFile answerFile = EduUtils.findTaskFileInDir(entry.getValue(), taskDir);
          if (answerFile == null) {
            continue;
          }
          final TaskFile studentFile = EduUtils.createStudentFile(project, answerFile, task, 0);
          if (studentFile != null) {
            studentTaskFiles.put(entry.getKey(), studentFile);
          }
        }
        task.taskFiles = studentTaskFiles;
      }

      private void addDescriptions(@NotNull final Task task) {
        final List<VirtualFile> descriptions = getDescriptionFiles(task, project);
        for (VirtualFile file : descriptions) {
          try {
            task.addTaskText(FileUtilRt.getNameWithoutExtension(file.getName()), VfsUtilCore.loadText(file));
          }
          catch (IOException e) {
            LOG.warn("Failed to load text " + file.getName());
          }
        }
      }

      private void addTestsToTask(Task task) {
        task.getTestsText().clear();
        final List<VirtualFile> testFiles = getTestFiles(task, project);
        for (VirtualFile file : testFiles) {
          try {
            task.addTestsTexts(file.getName(), VfsUtilCore.loadText(file));
          }
          catch (IOException e) {
            LOG.warn("Failed to load text " + file.getName());
          }
        }
      }

      private List<VirtualFile> getTestFiles(@NotNull Task task, @NotNull Project project) {
        List<VirtualFile> testFiles = new ArrayList<>();
        VirtualFile taskDir = task.getTaskDir(project);
        if (taskDir == null) {
          return testFiles;
        }
        String testDirPath = TaskExt.getTestDir(task);
        if (StringUtil.isNotEmpty(testDirPath)) {
          VirtualFile testDir = taskDir.findFileByRelativePath(testDirPath);
          if (testDir == null) {
            return testFiles;
          }
          testFiles.addAll(Arrays.asList(testDir.getChildren()));
        } else {
          testFiles.addAll(Arrays.stream(taskDir.getChildren())
            .filter(file -> EduUtils.isTestsFile(project, file))
            .collect(Collectors.toList()));
        }
        return testFiles;
      }

      private List<VirtualFile> getDescriptionFiles(@NotNull Task task, @NotNull Project project) {
        List<VirtualFile> testFiles = new ArrayList<>();
        VirtualFile taskDir = task.getTaskDir(project);
        if (taskDir == null) {
          return testFiles;
        }
        testFiles.addAll(Arrays.stream(taskDir.getChildren())
                           .filter(file -> EduUtils.isTaskDescriptionFile(file.getName()))
                           .collect(Collectors.toList()));
        return testFiles;
      }
    });

    return isCreationSuccessful.get();
  }

  private static void synchronize(@NotNull final Project project) {
    VirtualFileManager.getInstance().refreshWithoutFileWatcher(true);
    ProjectView.getInstance(project).refresh();
  }

  private static void packCourse(@NotNull final VirtualFile baseDir, String locationDir,
                                    String zipName, boolean showMessage) throws IOException {
    final File zipFile = new File(locationDir, zipName + ".zip");
    VirtualFile[] courseFiles = baseDir.getChildren();
    try (ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(zipFile)))) {
      for (VirtualFile file : courseFiles) {
        ZipUtil.addFileOrDirRecursively(zos, null, new File(file.getPath()), file.getName(), null, null);
      }
      if (showMessage) {
        ApplicationManager.getApplication().invokeLater(
          () -> Messages.showInfoMessage("Course archive was saved to " + zipFile.getPath(),
                                         "Course Archive Was Created Successfully"));

      }
    }
  }

  private static void generateJson(VirtualFile parentDir, Course course) throws IOException {
    final Gson gson = new GsonBuilder().setPrettyPrinting().excludeFieldsWithoutExposeAnnotation().
      registerTypeAdapter(Task.class, new SerializationUtils.Json.TaskAdapter()).create();
    final String json = gson.toJson(course);
    final File courseJson = new File(parentDir.getPath(), EduNames.COURSE_META_FILE);
    try (OutputStreamWriter outputStreamWriter = new OutputStreamWriter(new FileOutputStream(courseJson), "UTF-8")) {
      outputStreamWriter.write(json);
    }
  }
}
