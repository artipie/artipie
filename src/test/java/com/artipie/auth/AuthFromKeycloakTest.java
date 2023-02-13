/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.auth;

import com.amihaiemil.eoyaml.Yaml;
import com.artipie.ArtipieException;
import com.artipie.asto.test.TestResource;
import com.artipie.http.auth.Authentication;
import com.artipie.settings.YamlSettings;
import com.artipie.tools.CodeBlob;
import com.artipie.tools.CodeClassLoader;
import com.artipie.tools.CompilerTool;
import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.Is;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.StringContains;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Test for {@link AuthFromKeycloak}.
 *
 * @since 0.28
 * @checkstyle IllegalCatchCheck (500 lines)
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidCatchingThrowable")
@Testcontainers
@DisabledOnOs(OS.WINDOWS)
public class AuthFromKeycloakTest {
    /**
     * Keycloak port.
     */
    private static final int KEYCLOAK_PORT = 8080;

    /**
     * Keycloak admin login.
     */
    private static final String ADMIN_LOGIN = "admin";

    /**
     * Keycloak admin password.
     */
    private static final String ADMIN_PASSWORD = AuthFromKeycloakTest.ADMIN_LOGIN;

    /**
     * Keycloak realm.
     */
    private static final String REALM = "test_realm";

    /**
     * Keycloak client application id.
     */
    private static final String CLIENT_ID = "test_client";

    /**
     * Keycloak client application password.
     */
    private static final String CLIENT_PASSWORD = "secret";

    /**
     * Keycloak docker container.
     */
    @Container
    private static GenericContainer<?> keycloak = new GenericContainer<>(
        DockerImageName.parse("quay.io/keycloak/keycloak:20.0.1")
    )
        .withEnv("KEYCLOAK_ADMIN", AuthFromKeycloakTest.ADMIN_LOGIN)
        .withEnv("KEYCLOAK_ADMIN_PASSWORD", AuthFromKeycloakTest.ADMIN_PASSWORD)
        .withExposedPorts(AuthFromKeycloakTest.KEYCLOAK_PORT)
        .withCommand("start-dev");

    /**
     * Jars of classpath used for compilation java sources and loading of compiled classes.
     */
    private static Set<URL> jars;

    /**
     * Sources of java-code for compilation.
     */
    private static Set<URL> sources;

    /**
     * Compiles, loads 'keycloak.KeycloakDockerInitializer' class and start 'main'-method.
     * Runtime compilation is required because 'keycloak.KeycloakDockerInitializer' class
     * has a clash of dependencies with Artipie's dependency 'com.jcabi:jcabi-github:1.3.2'.
     */
    @BeforeAll
    static void init() {
        try {
            AuthFromKeycloakTest.prepareJarsAndSources();
            final List<CodeBlob> blobs = AuthFromKeycloakTest.compileKeycloakInitializer();
            final CodeClassLoader loader = AuthFromKeycloakTest.initCodeClassloader(blobs);
            final MethodHandle main = AuthFromKeycloakTest.mainMethod(loader);
            AuthFromKeycloakTest.initializeKeycloakInstance(loader, main);
        } catch (final Throwable exc) {
            throw new ArtipieException(exc);
        }
    }

    @Test
    void authenticateExistingUserReturnsUserWithRealmAndClientRoles() {
        final String login = "user1";
        final String password = "password";
        final YamlSettings settings = AuthFromKeycloakTest.settings(
            AuthFromKeycloakTest.keycloakUrl(),
            AuthFromKeycloakTest.REALM,
            AuthFromKeycloakTest.CLIENT_ID,
            AuthFromKeycloakTest.CLIENT_PASSWORD
        );
        final Optional<Authentication.User> opt = settings.auth().user(login, password);
        MatcherAssert.assertThat(
            opt.isPresent(),
            new IsEqual<>(true)
        );
        final Authentication.User user = opt.get();
        MatcherAssert.assertThat(
            user.name(),
            Is.is(login)
        );
        MatcherAssert.assertThat(user.groups().contains("role_realm"), new IsEqual<>(true));
        MatcherAssert.assertThat(user.groups().contains("client_role"), new IsEqual<>(true));
    }

    @Test
    void authenticateNoExistingUser() {
        final String fake = "fake";
        final YamlSettings settings = AuthFromKeycloakTest.settings(
            AuthFromKeycloakTest.keycloakUrl(),
            AuthFromKeycloakTest.REALM,
            AuthFromKeycloakTest.CLIENT_ID,
            AuthFromKeycloakTest.CLIENT_PASSWORD
        );
        MatcherAssert.assertThat(
            Assertions.assertThrows(
                ArtipieException.class,
                () -> settings.auth().user(fake, fake)
            ).getMessage(),
            new StringContains("Failed to obtain authorization data")
        );
    }

    /**
     * Composes yaml settings.
     *
     * @param url Keycloak server url
     * @param realm Keycloak realm
     * @param client Keycloak client application ID
     * @param password Keycloak client application password
     * @checkstyle ParameterNumberCheck (3 lines)
     */
    @SuppressWarnings("PMD.UseObjectForClearerAPI")
    private static YamlSettings settings(final String url, final String realm,
        final String client, final String password) {
        return new YamlSettings(
            Yaml.createYamlMappingBuilder().add(
                "meta",
                Yaml.createYamlMappingBuilder().add(
                    "credentials",
                    Yaml.createYamlSequenceBuilder()
                        .add(
                            Yaml.createYamlMappingBuilder()
                                .add("type", "keycloak")
                                .add("url", url)
                                .add("realm", realm)
                                .add("client-id", client)
                                .add("client-password", password)
                                .build()
                        ).build()
                ).build()
            ).build()
        );
    }

    /**
     * Loads dependencies from jar-files and java-sources for compilation.
     *
     * @throws IOException Exception.
     */
    private static void prepareJarsAndSources() throws IOException {
        final String resources = "auth/keycloak-docker-initializer";
        AuthFromKeycloakTest.jars = files(
            new TestResource(String.format("%s/lib", resources)).asPath(), ".jar"
        );
        AuthFromKeycloakTest.sources = files(
            new TestResource(String.format("%s/src", resources)).asPath(), ".java"
        );
    }

    /**
     * Compiles 'keycloak.KeycloakDockerInitializer' class from sources.
     *
     * @return List of compiled classes as CodeBlobs.
     * @throws IOException Exception.
     */
    private static List<CodeBlob> compileKeycloakInitializer() throws IOException {
        final CompilerTool compiler = new CompilerTool();
        compiler.addClasspaths(jars.stream().toList());
        compiler.addSources(sources.stream().toList());
        compiler.compile();
        return compiler.classesToCodeBlobs();
    }

    /**
     * Create instance of CodeClassLoader.
     *
     * @param blobs Code blobs.
     * @return CodeClassLoader CodeClassLoader
     */
    private static CodeClassLoader initCodeClassloader(final List<CodeBlob> blobs) {
        final URLClassLoader urlld = new URLClassLoader(
            jars
                .stream()
                .map(
                    file -> {
                        try {
                            return file.toURI().toURL();
                        } catch (final MalformedURLException | URISyntaxException exc) {
                            throw new ArtipieException(exc);
                        }
                    }
                )
                .toList()
                .toArray(new URL[0]),
            null
        );
        final CodeClassLoader codeld = new CodeClassLoader(urlld);
        codeld.addBlobs(blobs);
        return codeld;
    }

    /**
     * Lookups 'public static void main(String[] args)' method
     * of 'keycloak.KeycloakDockerInitializer' class.
     *
     * @param loader CodeClassLoader
     * @return Method 'public static void main(String[] args)'
     *  of 'keycloak.KeycloakDockerInitializer' class
     * @throws ClassNotFoundException Exception.
     * @throws NoSuchMethodException  Exception.
     * @throws IllegalAccessException Exception.
     */
    private static MethodHandle mainMethod(final CodeClassLoader loader)
        throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException {
        final Class<?> clazz = Class.forName("keycloak.KeycloakDockerInitializer", true, loader);
        final MethodType methodtype = MethodType.methodType(void.class, String[].class);
        return MethodHandles.publicLookup().findStatic(clazz, "main", methodtype);
    }

    /**
     * Starts 'keycloak.KeycloakDockerInitializer' class by passing url of keycloak server
     * in first argument of 'main'-method.
     * CodeClassLoader is used as context class loader.
     *
     * @param loader CodeClassLoader.
     * @param main Main-method.
     */
    private static void initializeKeycloakInstance(final CodeClassLoader loader,
        final MethodHandle main) {
        final ClassLoader originalld = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(loader);
            main.invoke(
                new String[]{keycloakUrl()}
            );
        } catch (final Throwable exc) {
            throw new ArtipieException(exc);
        } finally {
            Thread.currentThread().setContextClassLoader(originalld);
        }
    }

    /**
     * Lookup files in directory by specified extension.
     *
     * @param dir Directory for listing.
     * @param ext Extension of files, example '.jar'
     * @return URLs of files.
     * @throws IOException Exception
     */
    private static Set<URL> files(final Path dir, final String ext) throws IOException {
        final Set<URL> files = new HashSet<>();
        Files.walkFileTree(
            dir,
            new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs)
                    throws MalformedURLException {
                    if (!Files.isDirectory(file)
                        && (ext == null || file.toString().endsWith(ext))) {
                        files.add(file.toFile().toURI().toURL());
                    }
                    return FileVisitResult.CONTINUE;
                }
            }
        );
        return files;
    }

    /**
     * Keycloak server url loaded by docker container.
     *
     * @return Keycloak server url.
     */
    private static String keycloakUrl() {
        return String.format(
            "http://localhost:%s",
            keycloak.getMappedPort(AuthFromKeycloakTest.KEYCLOAK_PORT)
        );
    }
}
