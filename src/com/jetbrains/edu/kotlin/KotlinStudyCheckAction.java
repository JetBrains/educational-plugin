package com.jetbrains.edu.kotlin;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.compiler.CompilerManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.jetbrains.edu.learning.actions.StudyCheckAction;
import com.jetbrains.edu.learning.actions.StudyRunAction;

import java.util.ArrayList;


public class KotlinStudyCheckAction extends StudyCheckAction {

    private static final Logger LOG = Logger.getInstance(StudyRunAction.class.getName());

    public static final String ACTION_ID = "KotlinStudyCheckAction";
    private static final String KOTLIN_EXTENSION = "kt";
    private static final String UTIL_FOLDER = "util";
    private static final String TEST_HELPER = "TestHelper.java";

    @Override
    public void actionPerformed(AnActionEvent e) {
        DataContext dataContext = e.getDataContext();
        final Project project = e.getProject();

        if (project != null) {

            compileTaskFiles(project, dataContext);
//            project.getBaseDir().getFileSystem().refresh(false);
//            final Condition<Project> myCond = new Condition<Project>() {
//                @Override
//                public boolean value(Project project) {
//                    final IdeFrame frame = ((WindowManagerEx) WindowManager.getInstance()).findFrameFor(project);
//                    final StatusBarEx statusBar = frame == null ? null : (StatusBarEx)frame.getStatusBar();
//                    if (statusBar != null) {
//                        final List<Pair<TaskInfo, ProgressIndicator>> processes = statusBar.getBackgroundProcesses();
//                        return processes.isEmpty();
//                    }
//                    return true;
//                }
//            };
//            Runnable myRun = new Runnable() {
//                @Override
//                public void run() {
//                    ApplicationManager.getApplication().invokeLater(new Runnable() {
//                        @Override
//                        public void run() {
//                            check(project);
//                        }
//                    });//, ModalityState.NON_MODAL, myCond);
//                }
//            };
//            DumbService.getInstance(project).runWhenSmart(new Runnable() {
//                @Override
//                public void run() {
//                    VirtualFileManager.getInstance().refreshWithoutFileWatcher(true);
//                }
//            });
//            DumbService.getInstance(project).runWhenSmart(myRun);
//        Bad way because of check runs after every indexing
/*            DumbService.DumbModeListener dumbModeListener = new DumbService.DumbModeListener() {
                @Override
                public void enteredDumbMode() {}

                @Override
                public void exitDumbMode() {
                    ApplicationManager.getApplication().invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            check(project);
                        }
                    });
                }
            };
            project.getMessageBus().connect().subscribe(DumbService.DUMB_MODE, dumbModeListener);
            */
        }
    }

//  ----------------------------------------------Compile Functions----------------------------------------------
    private void compileTaskFiles(Project project, DataContext dataContext) {
        VirtualFile[] files = getCompilableFiles(project, dataContext);
        if (files.length > 0) {
            CompilerManager.getInstance(project).compile(files, null);
        }
    }

    private static VirtualFile[] getCompilableFiles(final Project project, DataContext dataContext) {
        VirtualFile taskFile = CommonDataKeys.VIRTUAL_FILE_ARRAY.getData(dataContext)[0];
//        VirtualFile taskFile = StudyUtils.getSelectedStudyEditor(project).getTaskFile().
        ArrayList<VirtualFile> files = new ArrayList<VirtualFile>();
        for (VirtualFile file: taskFile.getParent().getChildren()) {
            if (file.getExtension() != null && file.getExtension().equals(KOTLIN_EXTENSION))
                files.add(file);
        }
        for (VirtualFile file: project.getBaseDir().getChildren()) {
            if (file.getName().equals(UTIL_FOLDER)) {
                for (VirtualFile file_h: file.getChildren()) {
                    if (file_h.getName().equals(TEST_HELPER)) {
                        files.add(file_h);
                    }
                }
            }
        }
        return VfsUtilCore.toVirtualFileArray(files);
    }
}
