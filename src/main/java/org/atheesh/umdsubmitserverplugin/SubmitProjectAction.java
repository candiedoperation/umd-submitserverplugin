package org.atheesh.umdsubmitserverplugin;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Files;
import java.nio.file.Path;

public class SubmitProjectAction extends AnAction {
    @Override
    public void update(AnActionEvent e) {
        /* Get the Current Project */
        Project currentProject = e.getProject();
        if (currentProject == null) e.getPresentation().setEnabled(false);
        else {
            /* Check if the .submit file Exists */
            boolean submitExists =
                Files.exists(Path.of(currentProject.getBasePath() + "/.submit"));

            /* Set Visibility Status */
            e.getPresentation().setEnabled(submitExists);
        }
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        /* Initiate Submission Middleware */
        new SubmitServerMiddleware(e.getProject())
                .initiateSubmission();
    }
}
