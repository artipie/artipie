/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.tools;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import javax.tools.DiagnosticCollector;
import javax.tools.DiagnosticListener;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;
import org.apache.commons.io.FileUtils;

/**
 * Compiles java-sources.
 * @since 0.28
 */
public class CompilerTool {
    /**
     * Classpath for compilation.
     */
    private final List<URL> classpath;

    /**
     * Sources for compilation.
     */
    private final List<URL> sources;

    /**
     * Diagnostic listener.
     */
    @SuppressWarnings("PMD.AvoidFieldNameMatchingMethodName")
    private final DiagnosticListener<JavaFileObject> diagnostic;

    /**
     * Code blobs of compiled classes.
     */
    @SuppressWarnings("PMD.AvoidFieldNameMatchingMethodName")
    private final List<CodeBlob> blobs;

    /**
     * Ctor.
     */
    public CompilerTool() {
        this.classpath = new ArrayList<>(0);
        this.sources = new ArrayList<>(0);
        this.blobs = new ArrayList<>(0);
        this.diagnostic = new DiagnosticCollector<>();
    }

    /**
     * Add list of URLs to compilation classpath.
     * @param urls List of URLs.
     */
    public void addClasspaths(final List<URL> urls) {
        this.classpath.addAll(urls);
    }

    /**
     * Add list of java source URLs for compilation.
     * @param urls List of URLs.
     */
    public void addSources(final List<URL> urls) {
        this.sources.addAll(urls);
    }

    /**
     * Diagnostic listener.
     * @return Diagnostic listener.
     */
    public DiagnosticListener<JavaFileObject> diagnostic() {
        return this.diagnostic;
    }

    /**
     * Compiled code blobs.
     * @return Code blobs.
     */
    public List<CodeBlob> classesToCodeBlobs() {
        return this.blobs;
    }

    /**
     * Compiles java sources and stores compiled classes to list of code blobs.
     * @throws IOException IOException
     */
    public void compile() throws IOException {
        final Path output = Files.createTempDirectory("compiled");
        final Iterable<String> options = Arrays.asList("-d", output.toString());
        try {
            final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            final StandardJavaFileManager manager = compiler.getStandardFileManager(
                this.diagnostic, Locale.ENGLISH, Charset.defaultCharset()
            );
            manager.setLocation(StandardLocation.CLASS_PATH, urlsToFiles(this.classpath));
            final Iterable<? extends JavaFileObject> units = manager.getJavaFileObjectsFromFiles(
                urlsToFiles(this.sources)
            );
            if (!compiler.getTask(null, manager, this.diagnostic, options, null, units).call()) {
                manager.close();
                throw new AssertionError("compilation failed");
            }
            manager.close();
            this.blobs.addAll(classesToCodeBlobs(output));
        } finally {
            FileUtils.deleteDirectory(output.toFile());
        }
    }

    /**
     * Loads result of compilation from directory to set of code blobs.
     * @param dir Directory stores result of compilation.
     * @return Set of code blobs.
     * @throws IOException Exception
     */
    private static Set<CodeBlob> classesToCodeBlobs(final Path dir) throws IOException {
        final Set<CodeBlob> blobs = new HashSet<>();
        Files.walkFileTree(
            dir,
            new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(final Path path, final BasicFileAttributes attrs)
                    throws IOException {
                    if (!Files.isDirectory(path) && path.toString().endsWith(".class")) {
                        final String classname = dir.relativize(path).toString()
                            .replace(File.separatorChar, '.').replaceAll("\\.class$", "");
                        final byte[] bytes = Files.readAllBytes(path);
                        blobs.add(new CodeBlob(classname, bytes));
                    }
                    return FileVisitResult.CONTINUE;
                }
            }
        );
        return blobs;
    }

    /**
     * Converts list of URL to List of File.
     * @param urls List of URL.
     * @return List of File.
     */
    private static List<File> urlsToFiles(final List<URL> urls) {
        return urls.stream().map(FileUtils::toFile).toList();
    }
}
