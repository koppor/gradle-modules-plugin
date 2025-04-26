package org.javamodularity.moduleplugin.tasks;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.tasks.JavaExec;
import org.gradle.api.tasks.testing.Test;

public class JvmArgsHelper {
    private static final Logger LOGGER = Logging.getLogger(JvmArgsHelper.class);
    private static final String LINE_SEP = System.getProperty("line.separator");

    record JvmArgsData(Path parameterFile, List<String> newJvmArgs) {
    }

    /**
     * Workaround for 206 command line too long - https://github.com/java9-modularity/gradle-modules-plugin/issues/281
     */
    private static JvmArgsData getArgsFile(List<String> jvmArgs) throws IOException {
        List<String> newJvmArgs = new ArrayList<>();

        StringJoiner parametersJoiner = new StringJoiner("\"" + LINE_SEP + "\"", "\"", "\"");
        for (String jvmArg : jvmArgs) {
            if (jvmArg.startsWith("@")) {
                newJvmArgs.add(jvmArg);
            } else {
                parametersJoiner.add(jvmArg.replace("\\", "\\\\"));
            }
        }

        Path parameterFile = Files.createTempFile("jvm-args", ".txt");
        Files.write(parameterFile, parametersJoiner.toString().getBytes());
        parameterFile.toFile().deleteOnExit();

        newJvmArgs.add("@" + parameterFile.toAbsolutePath());

        return new JvmArgsData(parameterFile, newJvmArgs);
    }

    static void setJvmArgs(JavaExec task, List<String> jvmArgs) {
        try {
            JvmArgsData jvmArgsData = getArgsFile(jvmArgs);
            task.setJvmArgs(jvmArgsData.newJvmArgs);
            LOGGER.info("Patched jvmArgs for task {}: {}", task.getName(), jvmArgsData.newJvmArgs);
        } catch (IOException e) {
            LOGGER.warn("Could not create temporary file for jvmArgs. Falling back to default behavior.", e);
            task.setJvmArgs(jvmArgs);
        }
    }

    /**
     * Clone of setJvmArgs(JavaExec task, List<String> jvmArgs), because .setJvmArgs is not "backed" by an interface
     * and we did not want to use reflection
     */
    static void setJvmArgs(Test task, List<String> jvmArgs) {
        try {
            JvmArgsData jvmArgsData = getArgsFile(jvmArgs);
            task.setJvmArgs(jvmArgsData.newJvmArgs);
            LOGGER.info("Patched jvmArgs for task {}: {}", task.getName(), jvmArgsData.newJvmArgs);
        } catch (IOException e) {
            LOGGER.warn("Could not create temporary file for jvmArgs. Falling back to default behavior.", e);
            task.setJvmArgs(jvmArgs);
        }
    }

}
