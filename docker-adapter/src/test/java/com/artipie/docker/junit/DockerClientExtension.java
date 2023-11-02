/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.docker.junit;

import com.jcabi.log.Logger;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Docker client extension. When it enabled for test class:
 *  - temporary dir is created when in BeforeAll phase and destroyed in AfterAll.
 *    Docker command output is stored there;
 *  - DockerClient field of test class are populated.
 *
 * @since 0.3
 */
@SuppressWarnings({
    "PMD.AvoidCatchingGenericException",
    "PMD.OnlyOneReturn",
    "PMD.AvoidDuplicateLiterals",
    "PMD.AvoidCatchingThrowable"
})
public final class DockerClientExtension
    implements BeforeEachCallback, BeforeAllCallback, AfterAllCallback {

    /**
     * Namespace for class-wide variables.
     */
    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(
        DockerClientExtension.class
    );

    @Override
    public void beforeAll(final ExtensionContext context) throws Exception {
        Logger.debug(this, "beforeAll called");
        final Path temp = Files.createTempDirectory("junit-docker-");
        Logger.debug(this, "Created temp dir: %s", temp.toAbsolutePath().toString());
        final DockerClient client = new DockerClient(temp);
        Logger.debug(this, "Created docker client");
        context.getStore(DockerClientExtension.NAMESPACE).put("temp-dir", temp);
        context.getStore(DockerClientExtension.NAMESPACE).put("client", client);
    }

    @Override
    public void beforeEach(final ExtensionContext context) throws Exception {
        Logger.debug(this, "beforeEach called");
        final DockerClient client = context.getStore(DockerClientExtension.NAMESPACE)
            .get("client", DockerClient.class);
        this.injectVariables(context, client);
    }

    @Override
    public void afterAll(final ExtensionContext context) throws Exception {
        Logger.debug(this, "afterAll called");
        final Path temp = context.getStore(DockerClientExtension.NAMESPACE)
            .remove("temp-dir", Path.class);
        temp.toFile().delete();
        Logger.debug(this, "Temp dir is deleted");
        context.getStore(DockerClientExtension.NAMESPACE).remove("client");
    }

    /**
     * Injects {@link DockerClient} variables in the test instance.
     *
     * @param context JUnit extension context
     * @param client Docker client instance
     * @throws Exception When something get wrong
     */
    private void injectVariables(final ExtensionContext context, final DockerClient client)
        throws Exception {
        final Object instance = context.getRequiredTestInstance();
        for (final Field field : context.getRequiredTestClass().getDeclaredFields()) {
            if (field.getType().isAssignableFrom(DockerClient.class)) {
                Logger.debug(
                    this, "Found %s field. Try to set DockerClient instance", field.getName()
                );
                this.ensureFieldIsAccessible(field);
                field.set(instance, client);
            }
        }
    }

    /**
     * Try to set field accessible.
     *
     * @param field Class field that need to be accessible
     */
    private void ensureFieldIsAccessible(final Field field) {
        field.setAccessible(true);
        Logger.debug(this, "%s field is accessible now", field.getName());
    }
}
