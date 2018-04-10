package com.jetbrains.edu.coursecreator;

import com.google.common.collect.Collections2;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootModificationUtil;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.ui.InputValidatorEx;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileVisitor;
import com.intellij.util.Function;
import com.intellij.util.PathUtil;
import com.intellij.util.ThrowableConsumer;
import com.jetbrains.edu.learning.*;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.Lesson;
import com.jetbrains.edu.learning.courseFormat.StudyItem;
import com.jetbrains.edu.learning.courseFormat.TaskFile;
import com.jetbrains.edu.learning.courseFormat.ext.CourseExt;
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask;
import com.jetbrains.edu.learning.courseFormat.tasks.Task;
import org.apache.commons.codec.binary.Base64;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

public class CCUtils {
  public static final String ANSWER_EXTENSION_DOTTED = ".answer.";
  private static final Logger LOG = Logger.getInstance(CCUtils.class);
  public static final String GENERATED_FILES_FOLDER = ".coursecreator";
  public static final String COURSE_MODE = "Course Creator";

  /**
   * This method decreases index and updates directory names of
   * all tasks/lessons that have higher index than specified object
   *
   * @param dirs         directories that are used to get tasks/lessons
   * @param getStudyItem function that is used to get task/lesson from VirtualFile. This function can return null
   * @param threshold    index is used as threshold
   */
  public static void updateHigherElements(VirtualFile[] dirs,
                                          @NotNull final Function<VirtualFile, ? extends StudyItem> getStudyItem,
                                          final int threshold,
                                          final int delta) {
    ArrayList<VirtualFile> itemsToUpdate = new ArrayList<>
      (Collections2.filter(Arrays.asList(dirs), dir -> {
        final StudyItem item = getStudyItem.fun(dir);
        if (item == null) {
          return false;
        }
        int index = item.getIndex();
        return index > threshold;
      }));
    Collections.sort(itemsToUpdate, (o1, o2) -> {
      StudyItem item1 = getStudyItem.fun(o1);
      StudyItem item2 = getStudyItem.fun(o2);
      //if we delete some dir we should start increasing numbers in dir names from the end
      return (-delta) * EduUtils.INDEX_COMPARATOR.compare(item1, item2);
    });

    for (final VirtualFile dir : itemsToUpdate) {
      final StudyItem item = getStudyItem.fun(dir);
      final int newIndex = item.getIndex() + delta;
      item.setIndex(newIndex);
    }
  }

  public static VirtualFile getGeneratedFilesFolder(@NotNull Project project, @NotNull Module module) {
    VirtualFile baseDir = project.getBaseDir();
    VirtualFile folder = baseDir.findChild(GENERATED_FILES_FOLDER);
    if (folder != null) {
      return folder;
    }
    final Ref<VirtualFile> generatedRoot = new Ref<>();
    ApplicationManager.getApplication().runWriteAction(new Runnable() {
      @Override
      public void run() {
        try {
          generatedRoot.set(baseDir.createChildDirectory(this, GENERATED_FILES_FOLDER));
          VirtualFile contentRootForFile =
            ProjectRootManager.getInstance(module.getProject()).getFileIndex().getContentRootForFile(generatedRoot.get());
          if (contentRootForFile == null) {
            return;
          }
          ModuleRootModificationUtil.updateExcludedFolders(module, contentRootForFile, Collections.emptyList(),
                                                           Collections.singletonList(generatedRoot.get().getUrl()));
        }
        catch (IOException e) {
          LOG.info("Failed to create folder for generated files", e);
        }
      }
    });
    return generatedRoot.get();
  }

  @Nullable
  public static VirtualFile generateFolder(@NotNull Project project, @NotNull Module module, String name) {
    VirtualFile generatedRoot = getGeneratedFilesFolder(project, module);
    if (generatedRoot == null) {
      return null;
    }

    final Ref<VirtualFile> folder = new Ref<>(generatedRoot.findChild(name));
    //need to delete old folder
    ApplicationManager.getApplication().runWriteAction(() -> {
      try {
        if (folder.get() != null) {
          folder.get().delete(null);
        }
        folder.set(generatedRoot.createChildDirectory(null, name));
      }
      catch (IOException e) {
        LOG.info("Failed to generate folder " + name, e);
      }
    });
    return folder.get();
  }

  public static boolean isCourseCreator(@NotNull Project project) {
    Course course = StudyTaskManager.getInstance(project).getCourse();
    if (course == null) {
      return false;
    }

    if (COURSE_MODE.equals(course.getCourseMode())) {
      return true;
    }
    return COURSE_MODE.equals(EduUtils.getCourseModeForNewlyCreatedProject(project));
  }

  public static void updateActionGroup(AnActionEvent e) {
    Presentation presentation = e.getPresentation();
    Project project = e.getProject();
    presentation.setEnabledAndVisible(project != null && isCourseCreator(project));
  }

  @Nullable
  public static Lesson createAdditionalLesson(@NotNull final Course course, @NotNull final Project project,
                                              @NotNull final String name) {
    final VirtualFile baseDir = project.getBaseDir();
    EduConfigurator configurator = EduConfiguratorManager.forLanguage(course.getLanguageById());

    final Lesson lesson = new Lesson();
    lesson.setName(name);
    lesson.setCourse(course);
    final Task task = new EduTask();
    task.setLesson(lesson);
    task.setName(name);
    task.setIndex(1);

    String sanitizedName = FileUtil.sanitizeFileName(course.getName());
    final String archiveName = String.format("%s.zip", sanitizedName.startsWith("_") ? EduNames.COURSE : sanitizedName);

    final VirtualFile utilDir = baseDir.findChild(EduNames.UTIL);
    final String sourceDirName = CourseExt.getSourceDir(course);
    final String testDirName = CourseExt.getTestDir(course);
    final VirtualFile utilSourceDir;
    final VirtualFile utilTestDir;
    if (utilDir != null && sourceDirName != null && testDirName != null) {
      utilSourceDir = utilDir.findChild(sourceDirName);
      utilTestDir = utilDir.findChild(testDirName);
    } else {
      utilSourceDir = null;
      utilTestDir = null;
    }

    VfsUtilCore.visitChildrenRecursively(baseDir, new VirtualFileVisitor(VirtualFileVisitor.NO_FOLLOW_SYMLINKS) {
      @Override
      public boolean visitFile(@NotNull VirtualFile file) {
        final String name = file.getName();
        if (name.equals(EduNames.COURSE_META_FILE) || name.equals(EduNames.HINTS) || name.startsWith(".")) return false;
        if (name.equals(archiveName)) return false;
        if (GENERATED_FILES_FOLDER.equals(name) || Project.DIRECTORY_STORE_FOLDER.equals(name)) {
          return false;
        }
        if (file.isDirectory()) return true;

        if (EduUtils.isTaskDescriptionFile(name) || EduUtils.isTestsFile(project, file)) return true;

        if (name.contains(".iml") || (configurator != null && configurator.excludeFromArchive(file.getPath()))) {
          return false;
        }
        final TaskFile taskFile = EduUtils.getTaskFile(project, file);
        if (taskFile == null) {
          if (utilSourceDir != null && VfsUtilCore.isAncestor(utilSourceDir, file, true)) {
            addTaskFile(task, baseDir, file);
          } else if (utilTestDir != null && VfsUtilCore.isAncestor(utilTestDir, file, true)) {
            addTestFile(task, baseDir, file);
          } else {
            addAdditionalFile(task, baseDir, file);
          }
        }
        return true;
      }
    });
    if (taskIsEmpty(task)) return null;
    lesson.addTask(task);
    lesson.setIndex(course.getItems().size() + 1);
    return lesson;
  }

  private static boolean taskIsEmpty(@NotNull Task task) {
    return task.getTaskFiles().isEmpty() &&
           task.getTestsText().isEmpty() &&
           task.getAdditionalFiles().isEmpty();
  }

  private static void addTaskFile(@NotNull Task task, @NotNull VirtualFile baseDir, @NotNull VirtualFile file) {
    addToTask(baseDir, file, path -> {
      TaskFile utilTaskFile = new TaskFile();
      utilTaskFile.name = path;
      utilTaskFile.text = VfsUtilCore.loadText(file);
      utilTaskFile.setTask(task);
      task.addTaskFile(utilTaskFile);
    });
  }

  private static void addTestFile(@NotNull Task task, @NotNull VirtualFile baseDir, @NotNull VirtualFile file) {
    addToTask(baseDir, file, path -> task.addTestsTexts(path, VfsUtilCore.loadText(file)));
  }

  private static void addAdditionalFile(@NotNull Task task, @NotNull VirtualFile baseDir, @NotNull VirtualFile file) {
    addToTask(baseDir, file, path -> {
      String text;
      if (EduUtils.isImage(file.getName())) {
        text = Base64.encodeBase64URLSafeString(VfsUtilCore.loadBytes(file));
      } else {
        text = VfsUtilCore.loadText(file);
      }
      task.addAdditionalFile(path, text);
    });
  }

  private static void addToTask(@NotNull VirtualFile baseDir, @NotNull VirtualFile file,
                                @NotNull ThrowableConsumer<String, IOException> action) {
    String path = VfsUtilCore.getRelativePath(file, baseDir);
    if (path == null) return;
    try {
      action.consume(path);
    } catch (IOException e) {
      LOG.error(e);
    }
  }

  public static class PathInputValidator implements InputValidatorEx {
    @Nullable private final VirtualFile myParentDir;
    @Nullable private final String myName;
    @Nullable private String myErrorText;

    public PathInputValidator(@Nullable VirtualFile parentDir) {
      myParentDir = parentDir;
      myName = null;
    }

    public PathInputValidator(@Nullable VirtualFile parentDir, @NotNull String name) {
      myParentDir = parentDir;
      myName = name;
    }

    @Override
    public boolean checkInput(String inputString) {
      if (myParentDir == null) {
        myErrorText = "invalid parent directory";
        return false;
      }
      myErrorText = null;
      if (!PathUtil.isValidFileName(inputString)) {
        myErrorText = "invalid name";
        return false;
      }
      if (myParentDir.findChild(inputString) != null && !inputString.equals(myName)) {
        myErrorText = String.format("%s already contains directory named %s", myParentDir.getName(), inputString);
      }
      return myErrorText == null;
    }

    @Override
    public boolean canClose(String inputString) {
      return checkInput(inputString);
    }

    @Nullable
    @Override
    public String getErrorText(String inputString) {
      return myErrorText;
    }
  }
}
