package org.atheesh.umdsubmitserverplugin;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;

public class SubmitServerMiddleware {
    private Project project;
    private HashMap<String, String> config;

    public SubmitServerMiddleware(Project project) {
        /* Initialize Instance Variables */
        this.project = project;
        this.config = new HashMap<>();

        /* Call Init Methods */
        parseSubmitServerConfig();
    }

    public void initiateSubmission() {

    }

    public void parseSubmitServerConfig() {
        try {
            /* Read All lines into String */
            List<String> submitServerConfig =
                    Files.readAllLines(
                            Path.of(this.project.getBasePath() + "/.submit"),
                            StandardCharsets.UTF_8
                    );

            /* Parse Each Line of Config */
            for (String configLine : submitServerConfig) {
                /* Ignore comments (Lines startWith #) */
                if(!configLine.startsWith("#")) {
                    /* Key-Value Pairs are split with = */
                    String[] params = configLine.split("=");

                    /* Ensure the existence of a K-V Pair */
                    if (params.length == 2)
                        this.config.put(params[0], params[1]);
                }
            }
        } catch (IOException ex) {
            /* Show Error Dialog */
            Messages.showErrorDialog(
                    "Unable to access the Submit Server configuration for this project.",
                    "Project Submission Error"
            );
        } catch (Exception ex) {
            /* Show Error Dialog */
            Messages.showErrorDialog(
                    "Failed to parse submit server configuration.",
                    "Project Submission Error"
            );
        }
    }
}
