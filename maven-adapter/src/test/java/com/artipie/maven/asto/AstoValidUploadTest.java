/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.maven.asto;

import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.asto.test.TestResource;
import com.artipie.maven.MetadataXml;
import com.artipie.maven.http.PutMetadataSlice;
import java.util.Arrays;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Test for {@link AstoValidUpload}.
 * @since 0.5
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public final class AstoValidUploadTest {

    /**
     * Test storage.
     */
    private Storage storage;

    /**
     * Blocking storage.
     */
    private BlockingStorage bsto;

    /**
     * Asto valid upload instance.
     */
    private AstoValidUpload validupload;

    @BeforeEach
    void init() {
        this.storage = new InMemoryStorage();
        this.bsto = new BlockingStorage(this.storage);
        this.validupload = new AstoValidUpload(this.storage);
    }

    @Test
    void returnsTrueWhenUploadIsValid() throws InterruptedException {
        final Key upload = new Key.From(".upload/com/test");
        final Key artifact = new Key.From("com/test");
        final Key jar = new Key.From(upload, "1.0/my-package.jar");
        final Key war = new Key.From(upload, "1.0/my-package.war");
        final byte[] jbytes = "jar artifact".getBytes();
        final byte[] wbytes = "war artifact".getBytes();
        this.bsto.save(jar, jbytes);
        this.bsto.save(war, wbytes);
        this.addMetadata(upload);
        this.addMetadata(artifact);
        this.bsto.save(jar, jbytes);
        this.bsto.save(war, wbytes);
        new RepositoryChecksums(this.storage).generate(jar).toCompletableFuture().join();
        new RepositoryChecksums(this.storage).generate(war).toCompletableFuture().join();
        MatcherAssert.assertThat(
            this.validupload.validate(upload, artifact).toCompletableFuture().join(),
            new IsEqual<>(true)
        );
    }

    @Test
    void returnsFalseWhenNotAllChecksumsAreValid() throws InterruptedException {
        final Key key = new Key.From("org/example");
        final Key jar = new Key.From("org/example/1.0/my-package.jar");
        final Key war = new Key.From("org/example/1.0/my-package.war");
        final byte[] bytes = "artifact".getBytes();
        this.bsto.save(jar, bytes);
        this.bsto.save(war, "war artifact".getBytes());
        this.bsto.save(new Key.From(String.format("%s.sha256", war.string())), "123".getBytes());
        this.addMetadata(key);
        this.bsto.save(jar, bytes);
        new RepositoryChecksums(this.storage).generate(jar).toCompletableFuture().join();
        MatcherAssert.assertThat(
            this.validupload.validate(key, key).toCompletableFuture().join(),
            new IsEqual<>(false)
        );
    }

    @Test
    void returnsFalseWhenNoArtifactsFound() {
        final Key upload = new Key.From(".upload/com/test/logger");
        this.addMetadata(upload);
        MatcherAssert.assertThat(
            this.validupload.validate(upload, upload).toCompletableFuture().join(),
            new IsEqual<>(false)
        );
    }

    @Test
    void returnsFalseWhenMetadataIsNotValid() throws InterruptedException {
        final Key upload = new Key.From(".upload/com/test/logger");
        final Key artifact = new Key.From("com/test/logger");
        final Key jar = new Key.From("com/test/logger/1.0/my-package.jar");
        final byte[] bytes = "artifact".getBytes();
        this.bsto.save(jar, bytes);
        new MetadataXml("com.test", "jogger").addXmlToStorage(
            this.storage, new Key.From(upload, PutMetadataSlice.SUB_META, "maven-metadata.xml"),
            new MetadataXml.VersionTags("1.0", "1.0", "1.0")
        );
        this.addMetadata(artifact);
        this.bsto.save(jar, bytes);
        new RepositoryChecksums(this.storage).generate(jar).toCompletableFuture().join();
        MatcherAssert.assertThat(
            this.validupload.validate(upload, artifact).toCompletableFuture().join(),
            new IsEqual<>(false)
        );
    }

    @ParameterizedTest
    @CsvSource({
        "pom;pom.sha1;jar;jar.sha1,xml;xml.sha1,true",
        "war;war.md5;war.sha1,xml;xml.sha1;xml.md5,true",
        "pom;rar,xml;xml.sha1;xml.sha256,false",
        "'',xml;xml.sha1;xml.md5,false",
        "jar;jar.sha256,xml;xml.sha1,false",
        "war;war.sha256,xml,false"
    })
    void returnsTrueWhenReady(final String artifacts, final String meta, final boolean res) {
        final Key location = new Key.From(".upload/com/artipie/example/0.2");
        Arrays.stream(artifacts.split(";")).forEach(
            item -> this.bsto.save(
                new Key.From(location, String.format("example-0.2.%s", item)), new byte[]{}
            )
        );
        Arrays.stream(meta.split(";")).forEach(
            item -> this.bsto.save(
                new Key.From(
                    location, PutMetadataSlice.SUB_META, String.format("maven-metadata.%s", item)
                ), new byte[]{}
            )
        );
        MatcherAssert.assertThat(
            this.validupload.ready(location).toCompletableFuture().join(),
            new IsEqual<>(res)
        );
    }

    private void addMetadata(final Key base) {
        new TestResource("maven-metadata.xml.example").saveTo(
            this.storage, new Key.From(base, PutMetadataSlice.SUB_META, "maven-metadata.xml")
        );
    }

}
