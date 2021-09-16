/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.helm;

import com.artipie.ArtipieServer;
import com.artipie.RepoConfigYaml;
import com.artipie.RepoPerms;
import com.artipie.asto.Key;
import com.artipie.asto.fs.FileStorage;
import com.artipie.asto.test.TestResource;
import com.artipie.http.rs.RsStatus;
import com.artipie.nuget.RandomFreePort;
import com.artipie.test.RepositoryUrl;
import com.artipie.test.TestContainer;
import java.io.IOException;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.StringContains;
import org.hamcrest.text.StringContainsInOrder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.testcontainers.containers.GenericContainer;

/**
 * Integration tests for Helm repository.
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 * @todo #607:60min Verify install using kubectl in docker.
 *  Now test just check that `index.yaml` was created. It would
 *  be better to verify install within `helm install`. For this,
 *  it's necessary to create a kubernetes cluster in Docker.
 * @since 0.13
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
@EnabledOnOs({OS.LINUX, OS.MAC})
@Disabled(value = "FIXME: migrate artipie to testonctainers")
final class HelmITCase {

    /**
     * Chart name.
     */
    private static final String CHART = "tomcat-0.4.1.tgz";

    /**
     * Repo name.
     */
    private static final String REPO = "my-helm";

    /**
     * Temporary directory for all tests.
     * @checkstyle VisibilityModifierCheck (3 lines)
     */
    @TempDir
    Path tmp;

    /**
     * Tested Artipie server.
     */
    private ArtipieServer server;

    /**
     * Container.
     */
    private TestContainer cntn;

    /**
     * Repository url.
     */
    private RepositoryUrl url;

    /**
     * Artipie server port.
     */
    private int port;

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void uploadChartAndCreateIndexYaml(final boolean anonymous) throws Exception {
        final String chartrepo = "chartrepo";
        this.init(anonymous);
        final HttpURLConnection con = this.putToLocalhost(anonymous);
        MatcherAssert.assertThat(
            "Response status is 200",
            con.getResponseCode(),
            new IsEqual<>(Integer.parseInt(RsStatus.OK.code()))
        );
        this.cntn.execStdout(
            "helm", "init",
            "--stable-repo-url", this.url.string(anonymous),
            "--client-only"
        );
        MatcherAssert.assertThat(
            "Chart repository was added",
            this.helmRepoAdd(anonymous, chartrepo),
            new StringContains(
                String.format("\"%s\" has been added to your repositories", chartrepo)
            )
        );
        con.disconnect();
        MatcherAssert.assertThat(
            "Repo update from chart repository",
            this.cntn.execStdout("helm", "repo", "update"),
            new StringContainsInOrder(
                Arrays.asList(
                    String.format(
                        "Successfully got an update from the \"%s\" chart repository", chartrepo
                    ),
                    "Update Complete."
                )
            )
        );
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void helmCreateIndexYaml(final boolean anonymous) throws Exception {
        this.init(anonymous);
        final HttpURLConnection con = this.putToLocalhost(anonymous);
        MatcherAssert.assertThat(
            "Response status is 200",
            con.getResponseCode(),
            new IsEqual<>(Integer.parseInt(RsStatus.OK.code()))
        );
        MatcherAssert.assertThat(
            "`Index.yaml` was created",
            new FileStorage(this.tmp.resolve("repos")).exists(
                new Key.From(HelmITCase.REPO, "index.yaml")
            ).join(),
            new IsEqual<>(true)
        );
        con.disconnect();
    }

    @AfterEach
    void tearDown() {
        this.server.stop();
        this.cntn.close();
    }

    private void init(final boolean anonymous) throws IOException {
        this.port = new RandomFreePort().value();
        this.url = new RepositoryUrl(this.port, "my-helm");
        this.server = new ArtipieServer(
            this.tmp, "my-helm", this.config(anonymous).toString(), this.port
        );
        this.server.start();
        this.cntn = new TestContainer(
            new GenericContainer<>("alpine/helm:2.16.9")
                .withCommand("tail", "-f", "/dev/null")
                .withWorkingDirectory("/home/")
                .withFileSystemBind(this.tmp.toString(), "/home")
                .withCreateContainerCmdModifier(
                    cmd -> cmd.withEntrypoint("/bin/sh")
                        .withCmd("-c", "while true; do sleep 300; done;")
                )
        );
        this.cntn.start(this.port);
    }

    private RepoConfigYaml config(final boolean anonymous) {
        final RepoConfigYaml yaml = new RepoConfigYaml("helm")
            .withFileStorage(this.tmp.resolve("repos"))
            .withUrl(this.url.string(anonymous))
            .withPath(this.url.string());
        if (!anonymous) {
            yaml.withPermissions(
                new RepoPerms(ArtipieServer.ALICE.name(), "*")
            );
        }
        return yaml;
    }

    private String helmRepoAdd(final boolean anonymous, final String chartrepo) throws Exception {
        final List<String> cmdlst = new ArrayList<>(
            Arrays.asList("helm", "repo", "add", chartrepo, this.url.string())
        );
        if (!anonymous) {
            cmdlst.add("--username");
            cmdlst.add(ArtipieServer.ALICE.name());
            cmdlst.add("--password");
            cmdlst.add(ArtipieServer.ALICE.password());
        }
        final String[] cmdarr = cmdlst.toArray(new String[0]);
        return this.cntn.execStdout(cmdarr);
    }

    private HttpURLConnection putToLocalhost(final boolean anonymous) throws IOException {
        final HttpURLConnection con = (HttpURLConnection) new URL(
            String.format(
                "http://localhost:%s/%s/%s", this.port, HelmITCase.REPO, HelmITCase.CHART
            )
        ).openConnection();
        con.setRequestMethod("PUT");
        con.setDoOutput(true);
        if (!anonymous) {
            con.setAuthenticator(
                new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(
                            ArtipieServer.ALICE.name(),
                            ArtipieServer.ALICE.password().toCharArray()
                        );
                    }
                }
            );
        }
        new TestResource(String.format("helm/%s", HelmITCase.CHART)).asInputStream()
            .transferTo(con.getOutputStream());
        return con;
    }

}
