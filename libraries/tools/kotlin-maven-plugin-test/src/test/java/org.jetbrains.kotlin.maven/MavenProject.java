package org.jetbrains.kotlin.maven;

import com.intellij.openapi.util.io.FileUtil;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.*;

class MavenProject {
    @NotNull
    private final File workingDir;

    MavenProject(@NotNull String name) throws IOException {
        File originalProjectDir = new File("src/test/resources/" + name);
        workingDir = FileUtil.createTempDirectory("maven-test-" + name, null);
        File[] filesToCopy = originalProjectDir.listFiles();

        for (File from : filesToCopy) {
            File to = new File(workingDir, from.getName());
            FileUtil.copyFileOrDir(from, to);
        }
    }

    @NotNull
    File file(@NotNull String path) {
        return new File(workingDir, path);
    }

    MavenExecutionResult exec(String... targets) throws Exception {
        List<String> cmd = buildCmd(targets);
        ProcessBuilder processBuilder = new ProcessBuilder(cmd);
        setUpEnvVars(processBuilder.environment());

        processBuilder.directory(workingDir);
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();

        StringBuilder sb = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
            sb.append('\n');
        }

        int exitCode = process.waitFor();
        return new MavenExecutionResult(sb.toString(), workingDir, exitCode);
    }

    private void setUpEnvVars(Map<String, String> env) throws IOException {
        String mavenHome = System.getProperty("maven.home");
        if (mavenHome == null) {
            throw new IllegalStateException("A system property 'maven.home' is not set");
        }

        env.put("M2_HOME", mavenHome);
        String mavenPath = mavenHome + File.separator + "bin";
        env.put("PATH", env.get("PATH") + File.pathSeparator + mavenPath);
    }

    private List<String> buildCmd(String... args) {
        List<String> cmd = new ArrayList<String>();

        String osName = System.getProperty("os.name");
        if (osName == null) throw new IllegalStateException("A system property 'os.name' is not set");

        if (osName.contains("Windows")) {
            cmd.addAll(Arrays.asList("cmd", "/C"));
        }
        else {
            cmd.add("/bin/bash");
        }

        cmd.add("mvn");
        cmd.add("-Dkotlin.incremental.log.level=info");
        cmd.addAll(Arrays.asList(args));

        return cmd;
    }
}

