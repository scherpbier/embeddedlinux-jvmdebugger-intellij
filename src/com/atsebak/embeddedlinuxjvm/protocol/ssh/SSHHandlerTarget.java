package com.atsebak.embeddedlinuxjvm.protocol.ssh;

import com.atsebak.embeddedlinuxjvm.console.EmbeddedLinuxJVMConsoleView;
import com.atsebak.embeddedlinuxjvm.localization.EmbeddedLinuxJVMBundle;
import com.atsebak.embeddedlinuxjvm.protocol.ssh.jsch.EmbeddedSSHClient;
import com.atsebak.embeddedlinuxjvm.protocol.ssh.jsch.SFTPHandler;
import com.atsebak.embeddedlinuxjvm.runner.data.EmbeddedLinuxJVMRunConfigurationRunnerParameters;
import com.atsebak.embeddedlinuxjvm.utils.FileUtilities;
import com.intellij.execution.configurations.RuntimeConfigurationException;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import lombok.Builder;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;

@Builder
public class SSHHandlerTarget {
    private static final String NEW_LINE = System.getProperty("line.separator");
    private static final String OUTPUT_LOCATION = "IdeaProjects";
    private EmbeddedLinuxJVMRunConfigurationRunnerParameters piRunnerParameters;
    private EmbeddedLinuxJVMConsoleView consoleView;
    private EmbeddedSSHClient ssh;

    /**
     * Uploads Java application output folders
     *
     * @param compileOutput Output directory folder where to store the java application
     * @param cmd           The command to execute on the java files
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws RuntimeConfigurationException
     */
    public void uploadAndRunJavaApp(@NotNull final File compileOutput, @NotNull final String cmd)
            throws IOException, ClassNotFoundException, RuntimeConfigurationException {
        final String remoteDir = FileUtilities.SEPARATOR + "home" + FileUtilities.SEPARATOR
                + piRunnerParameters.getUsername() + FileUtilities.SEPARATOR + OUTPUT_LOCATION;
        String deploymentPath = remoteDir + FileUtilities.SEPARATOR + consoleView.getProject().getName();
        genericUpload(deploymentPath, compileOutput);
        consoleView.print(EmbeddedLinuxJVMBundle.getString("pi.deployment.finished") + NEW_LINE, ConsoleViewContentType.SYSTEM_OUTPUT);
        runJavaApp(deploymentPath, cmd);
    }

    /**
     * Generic SSh Ftp uploader
     *
     * @param deploymentPath the remote location storing the compressed file
     * @param fileToUpload   files to upload
     * @throws IOException
     * @throws RuntimeConfigurationException
     */
    public void genericUpload(@NotNull final String deploymentPath, @NotNull final File fileToUpload) throws IOException, RuntimeConfigurationException {
        forceCreateDirectories(deploymentPath);
        Session session = connect(ssh.get());
        try {
            consoleView.print(EmbeddedLinuxJVMBundle.getString("pi.upload"), ConsoleViewContentType.SYSTEM_OUTPUT);
            SFTPHandler sftpHandler = new SFTPHandler();
            sftpHandler.upload(session, fileToUpload, deploymentPath);
        } catch (Exception e) {
        }
    }


    /**
     * Force create directories, if it exists it won't do anything
     *
     * @param path
     * @throws IOException
     * @throws RuntimeConfigurationException
     */
    private void forceCreateDirectories(String path) throws IOException, RuntimeConfigurationException {
        Session session = connect(ssh.get());
        try {
            Channel channel = session.openChannel("shell");
            PrintStream shellStream = new PrintStream(channel.getOutputStream());
            channel.connect();
            List<String> commands = Arrays.asList(
                    String.format("mkdir -p %s", path),
                    String.format("cd %s", path),
                    String.format("mkdir -p %s", FileUtilities.CLASSES),
                    String.format("mkdir -p %s", FileUtilities.LIB),
                    String.format("cd %s", path + FileUtilities.SEPARATOR + FileUtilities.CLASSES),
                    "rm -rf *"
            );
            for (String command : commands) {
                consoleView.print(EmbeddedLinuxJVMBundle.getString("pi.deployment.command") + command + NEW_LINE, ConsoleViewContentType.SYSTEM_OUTPUT);
                shellStream.println(command);
                shellStream.flush();
            }

            channel.disconnect();
        } catch (JSchException e) {
            //todo throw exception message on ui
        }

    }

    /**
     * Runs that java app with the specified command and then takes the console output from target to host machine
     *
     * @param path
     * @param cmd
     * @throws IOException
     */
    private void runJavaApp(String path, String cmd) throws IOException, RuntimeConfigurationException {
        consoleView.print(NEW_LINE + EmbeddedLinuxJVMBundle.getString("pi.deployment.build") + NEW_LINE + NEW_LINE, ConsoleViewContentType.SYSTEM_OUTPUT);
        Session session = connect(ssh.get());
        try {
            Channel channel = session.openChannel("shell");
            channel.setOutputStream(System.out, true);
            channel.setExtOutputStream(System.err, true); //todo need error stream here
            PrintStream shellStream = new PrintStream(channel.getOutputStream());
            channel.connect();
            List<String> jarCommands = Arrays.asList(
                    String.format("sudo kill -9 $(ps -efww | grep \"%s\"| grep -v grep | tr -s \" \"| cut -d\" \" -f2)", piRunnerParameters.getMainclass()),
                    String.format("cd %s", path),
                    String.format("tar -xvf %s.tar", consoleView.getProject().getName()),
                    "rm *.tar",
                    cmd);
            for (String command : jarCommands) {
                shellStream.println(command);
                shellStream.flush();
            }
        } catch (JSchException e) {
            //todo throw exception message on ui
        }


    }

    /**
     * Authenticates and connects to remote target via ssh protocol
     *
     * @param session
     * @throws IOException
     * @throws RuntimeConfigurationException
     */
    @SneakyThrows({RuntimeConfigurationException.class})
    private Session connect(Session session) {
        if (!session.isConnected()) {
            session = EmbeddedSSHClient.builder().username(piRunnerParameters.getUsername())
                    .password(piRunnerParameters.getPassword()).hostname(piRunnerParameters.getHostname()).build().get();
            if (!session.isConnected()) {
                final Notification notification = new Notification(
                        com.atsebak.embeddedlinuxjvm.utils.Notifications.GROUPDISPLAY_ID,
                        EmbeddedLinuxJVMBundle.getString("pi.ssh.connection.error"), EmbeddedLinuxJVMBundle.getString("ssh.remote.error"),
                        NotificationType.ERROR);
                Notifications.Bus.notify(notification);
                throw new RuntimeConfigurationException(EmbeddedLinuxJVMBundle.getString("ssh.remote.error"));
            } else {
                return session;
            }
        }
        return session;
    }
}
