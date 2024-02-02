package org.atheesh.umdsubmitserverplugin;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import javax.swing.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class SubmitServerMiddleware {
    private Project project;
    private HashMap<String, String> config;
    private ProgressIndicator progressIndicator;
    private CloseableHttpClient httpClient;

    public SubmitServerMiddleware(Project project, ProgressIndicator progressIndicator) throws SubmitProjectException {
        /* Initialize Instance Variables */
        this.project = project;
        this.config = new HashMap<>();
        this.progressIndicator = progressIndicator;
        this.httpClient = HttpClients.createDefault();

        /* Call Init Methods */
        parseSubmitServerConfig();
    }

    public void initiateSubmission() throws SubmitProjectException {
        /* Update Progress */
        this.progressIndicator.setText("Authenticating with submit server");

        /* Check Authentication Method in Config */
        if (this.config.get("authentication.type").equals("ldap"))
            negotiateCredentialsLDAP();
    }

    private void negotiateCredentialsLDAP() throws SubmitProjectException {
        try {
            /* Get LDAP Username and Password, Add it to Config */
            SwingUtilities.invokeAndWait(() -> {
                SubmitLDAPCredDialog credDialog = new SubmitLDAPCredDialog(this.project);
                credDialog.show();

                /* Dialog is Synchronous, Get Username and Password */
                this.config.put("loginName", credDialog.getUsername());
                this.config.put("password", credDialog.getPassword());
            });

            /* Define the Request */
            HttpPost request = new HttpPost(this.config.get("baseURL") + "/eclipse/NegotiateOneTimePassword");
            request.setHeader("Content-Type", "application/x-www-form-urlencoded");

            /* Define Post Parameters */
            List<NameValuePair> params = new ArrayList<>();
            String[] configParams = new String[]{ "loginName", "password", "courseKey", "projectNumber" };

            for (String param : configParams) {
                /* Get Param values from Config */
                params.add(new BasicNameValuePair(param, this.config.get(param)));
            }

            request.setEntity(new UrlEncodedFormEntity(params));
            CloseableHttpResponse response = httpClient.execute(request);
            String responseString = EntityUtils.toString(response.getEntity());

            /* Check if Authentication is Successful */
            if (responseString.startsWith("failed") || responseString.length() < 5)
                throw new SubmitProjectException("Invalid Username or Password");

            /* String consists of a Submit Server User Config File */
            List<String> rsConfig = Arrays.asList(responseString.split("\n"));
            parseSubmitServerConfig(rsConfig);

            Thread.sleep(500);
        } catch (Exception ex) {
            throw new SubmitProjectException("Authentication with Server Failed: " + ex.getMessage());
        }
    }

    private void parseSubmitServerConfig() throws SubmitProjectException {
        /* Update Status */
        this.progressIndicator.setText("Reading configuration files");

        try {
            /* Read All lines into String */
            List<String> submitServerConfig =
                    Files.readAllLines(
                            Path.of(this.project.getBasePath() + "/.submit"),
                            StandardCharsets.UTF_8
                    );

            /* Call Wrapper Method */
            parseSubmitServerConfig(submitServerConfig);
        } catch (IOException ex) {
            throw new SubmitProjectException("Unable to access the Submit Server configuration for this project.");
        }
    }

    private void parseSubmitServerConfig(List<String> submitServerConfig) throws SubmitProjectException {
        try {
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

            /* Set Short Delay */
            Thread.sleep(500);
        } catch (Exception ex) {
            throw new SubmitProjectException("Failed to parse submit server configuration.");
        }
    }
}
