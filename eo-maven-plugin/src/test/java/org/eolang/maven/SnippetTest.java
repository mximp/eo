/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016-2023 Objectionary.com
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.eolang.maven;

import com.jcabi.log.Logger;
import com.jcabi.log.VerboseProcess;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.cactoos.Input;
import org.cactoos.Output;
import org.cactoos.io.InputOf;
import org.cactoos.io.OutputTo;
import org.cactoos.io.TeeInput;
import org.cactoos.iterable.Mapped;
import org.cactoos.list.Joined;
import org.cactoos.list.ListOf;
import org.cactoos.scalar.LengthOf;
import org.eolang.jucs.ClasspathSource;
import org.eolang.maven.objectionary.OyFilesystem;
import org.eolang.maven.util.Walk;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.yaml.snakeyaml.Yaml;

/**
 * Integration test for simple snippets.
 *
 * This test will/may fail if you change something in {@code to-java.xsl}
 * or some other place where Java sources are generated. This happens
 * because this test relies on {@code eo-runtime.jar}, which it finds in your local
 * Maven repository. This file is supposed to be generated by a previous run
 * of Maven, but will not exist at the first run. Thus, when changes are made
 * it is recommended to disable this test. Then, when new {@code eo-runtime.jar} is
 * released, you enable this test again.
 *
 * @since 0.1
 * @todo #1107:30m Method `jdkExecutable` is duplicated in eo-runtime.
 *  Find a way to make it reusable (i.e making it part of
 *  VerboseProcess) and remove it from MainTest.
 */
@ExtendWith(OnlineCondition.class)
final class SnippetTest {

    /**
     * Temp dir.
     * @checkstyle VisibilityModifierCheck (5 lines)
     */
    @TempDir
    public Path temp;

    /**
     * Runs and checks of eo snippets.
     *
     * @param yml Yaml test case.
     * @throws Exception If fails
     */
    @ParameterizedTest
    @SuppressWarnings("unchecked")
    @ClasspathSource(value = "org/eolang/maven/snippets/", glob = "**.yaml")
    void runsAllSnippets(final String yml) throws Exception {
        final Yaml yaml = new Yaml();
        final Map<String, Object> map = yaml.load(yml);
        final ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        final int result = SnippetTest.run(
            this.temp,
            new InputOf(String.format("%s\n", map.get("eo"))),
            (List<String>) map.get("args"),
            new InputOf(map.get("in").toString()),
            new OutputTo(stdout)
        );
        MatcherAssert.assertThat(
            String.format("'%s' returned wrong exit code", yml),
            result,
            Matchers.equalTo(map.get("exit"))
        );
        final String actual = new String(stdout.toByteArray(), StandardCharsets.UTF_8);
        Logger.debug(this, "Stdout: \"%s\"", actual);
        MatcherAssert.assertThat(
            String.format("'%s' printed something wrong", yml),
            actual,
            Matchers.allOf(
                new Mapped<>(
                    ptn -> Matchers.matchesPattern(
                        Pattern.compile(ptn, Pattern.DOTALL | Pattern.MULTILINE)
                    ),
                    (Iterable<String>) map.get("out")
                )
            )
        );
    }

    /**
     * Compile EO to Java and run.
     * @param tmp Temp dir
     * @param code EO sources
     * @param args Command line arguments
     * @param stdin The input
     * @param stdout Where to put stdout
     * @return All Java code
     * @throws Exception If fails
     * @checkstyle ParameterNumberCheck (5 lines)
     */
    @SuppressWarnings({"unchecked", "PMD.ExcessiveMethodLength"})
    private static int run(
        final Path tmp,
        final Input code,
        final List<String> args,
        final Input stdin,
        final Output stdout
    ) throws Exception {
        final Path src = tmp.resolve("src");
        final FakeMaven maven = new FakeMaven(tmp)
            .withProgram(code)
            .with("sourcesDir", src.toFile())
            .with("objects", Arrays.asList("org.eolang.bool"))
            .with("objectionary", new OyFilesystem());
        maven.execute(RegisterMojo.class);
        maven.execute(DemandMojo.class);
        maven.execute(AssembleMojo.class);
        maven.execute(TranspileMojo.class);
        final Path classes = maven.targetPath().resolve("classes");
        SnippetTest.compileJava(maven.generatedPath(), classes);
        SnippetTest.runJava(args, stdin, stdout, classes);
        return 0;
    }

    /**
     * Compile Java sources.
     * @param generated Where to find Java sources
     * @param classes Where to put compiled classes
     * @throws Exception If fails
     */
    private static void compileJava(final Path generated, final Path classes) throws Exception {
        SnippetTest.exec(
            String.format(
                "%s -encoding utf-8 %s -d %s -cp %s",
                SnippetTest.jdkExecutable("javac"),
                new Walk(generated).stream()
                    .map(Path::toAbsolutePath)
                    .map(Path::toString)
                    .collect(Collectors.joining(" ")),
                classes,
                SnippetTest.classpath()
            ),
            generated
        );
    }

    /**
     * Run Java.
     * @param args Command line arguments
     * @param stdin The input
     * @param stdout Where to put stdout
     * @param classes Where to find compiled classes
     * @throws Exception If fails
     * @checkstyle ParameterNumberCheck (5 lines)
     */
    private static void runJava(
        final List<String> args,
        final Input stdin,
        final Output stdout,
        final Path classes
    ) throws Exception {
        SnippetTest.exec(
            String.join(
                " ",
                new Joined<String>(
                    new ListOf<>(
                        SnippetTest.jdkExecutable("java"),
                        "-Dfile.encoding=UTF-8",
                        "-Dsun.stdout.encoding=UTF-8",
                        "-Dsun.stderr.encoding=UTF-8",
                        "-cp",
                        SnippetTest.classpath(),
                        "org.eolang.Main"
                    ),
                    args
                )
            ),
            classes, stdin, stdout
        );
    }

    /**
     * Run some command and print out the output.
     *
     * @param cmd The command
     * @param dir The home dir
     */
    private static void exec(final String cmd, final Path dir) throws Exception {
        SnippetTest.exec(
            cmd,
            dir,
            new InputOf(""),
            new OutputTo(new ByteArrayOutputStream())
        );
    }

    /**
     * Run some command and print out the output.
     *
     * @param cmd The command
     * @param dir The home dir
     * @param stdin Stdin
     * @param stdout Stdout
     * @checkstyle ParameterNumberCheck (5 lines)
     */
    private static void exec(
        final String cmd,
        final Path dir,
        final Input stdin,
        final Output stdout
    ) throws Exception {
        Logger.debug(SnippetTest.class, "+%s", cmd);
        final Process proc = new ProcessBuilder()
            .command(cmd.split(" "))
            .directory(dir.toFile())
            .redirectErrorStream(true)
            .start();
        new LengthOf(
            new TeeInput(
                stdin,
                new OutputTo(proc.getOutputStream())
            )
        ).value();
        try (VerboseProcess vproc = new VerboseProcess(proc)) {
            new LengthOf(
                new TeeInput(
                    new InputOf(vproc.stdout()),
                    stdout
                )
            ).value();
        }
    }

    /**
     * Locate executable inside JAVA_HOME.
     * @param name Name of executable.
     * @return Path to java executable.
     */
    private static String jdkExecutable(final String name) {
        final String result;
        final String relative = "%s/bin/%s";
        final String property = System.getProperty("java.home");
        if (property == null) {
            final String environ = System.getenv("JAVA_HOME");
            if (environ == null) {
                result = name;
            } else {
                result = String.format(relative, environ, name);
            }
        } else {
            result = String.format(relative, property, name);
        }
        return result;
    }

    /**
     * Classpath.
     * @return Classpath.
     */
    private static String classpath() {
        return String.format(
            ".%s%s",
            File.pathSeparatorChar,
            System.getProperty(
                "runtime.jar",
                Paths.get(System.getProperty("user.home")).resolve(
                    String.format(
                        ".m2/repository/org/eolang/eo-runtime/%s/eo-runtime-%1$s.jar",
                        "1.0-SNAPSHOT"
                    )
                ).toString()
            )
        );
    }
}
