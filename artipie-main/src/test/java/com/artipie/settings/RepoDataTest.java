/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.settings;

import com.artipie.api.RepositoryName;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.asto.fs.FileStorage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.test.TestStoragesCache;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Test for {@link RepoData}.
 * @since 0.1
 */
@SuppressWarnings({"PMD.AvoidDuplicateLiterals", "PMD.TooManyMethods"})
class RepoDataTest {

    /**
     * Test repository name.
     */
    private static final String REPO = "my-repo";

    /**
     * Maximum awaiting time duration.
     */
    private static final long MAX_WAIT = Duration.ofMinutes(1).toMillis();

    /**
     * Sleep duration.
     */
    private static final long SLEEP_DURATION = Duration.ofMillis(100).toMillis();

    /**
     * Temp dir.
     */
    @TempDir
    Path temp;

    /**
     * Test settings storage.
     */
    private BlockingStorage stngs;

    /**
     * Test settings storage.
     */
    private Storage storage;

    /**
     * Test data storage.
     */
    private BlockingStorage data;

    /**
     * Storages cache.
     */
    private StoragesCache cache;

    @BeforeEach
    void init() {
        this.cache = new TestStoragesCache();
        this.storage = new InMemoryStorage();
        this.stngs = new BlockingStorage(this.storage);
        this.data = new BlockingStorage(new FileStorage(this.temp));
    }

    @Test
    void removesData() {
        this.stngs.save(
            new Key.From(String.format("%s.yml", RepoDataTest.REPO)),
            this.repoSettings().getBytes(StandardCharsets.UTF_8)
        );
        this.data.save(new Key.From(RepoDataTest.REPO, "first.txt"), new byte[]{});
        this.data.save(new Key.From(RepoDataTest.REPO, "second.txt"), new byte[]{});
        new RepoData(this.storage, this.cache)
            .remove(new RepositoryName.Simple(RepoDataTest.REPO)).toCompletableFuture().join();
        MatcherAssert.assertThat(
            "Repository data are removed",
            this.waitCondition(() -> this.data.list(Key.ROOT).isEmpty())
        );
    }

    @Test
    void movesData() {
        this.stngs.save(
            new Key.From(String.format("%s.yml", RepoDataTest.REPO)),
            this.repoSettings().getBytes(StandardCharsets.UTF_8)
        );
        this.data.save(new Key.From(RepoDataTest.REPO, "first.txt"), new byte[]{});
        this.data.save(new Key.From(RepoDataTest.REPO, "second.txt"), new byte[]{});
        new RepoData(this.storage, this.cache)
            .move(
                new RepositoryName.Simple(RepoDataTest.REPO), new RepositoryName.Simple("new-repo")
            ).toCompletableFuture().join();
        MatcherAssert.assertThat(
            "Repository data are moved",
            this.waitCondition(
                () ->
                    this.data.list(Key.ROOT).stream()
                        .map(Key::string).toList()
                        .containsAll(List.of("new-repo/first.txt", "new-repo/second.txt"))
            )
        );
    }

    @ParameterizedTest
    @ValueSource(
        strings = {"_storages.yaml", "my-repo/_storages.yaml"}
    )
    void movesDataWithAlias(final String key) {
        this.stngs.save(
            new Key.From(String.format("%s.yml", RepoDataTest.REPO)),
            this.repoSettingsWithAlias().getBytes(StandardCharsets.UTF_8)
        );
        this.stngs.save(
            new Key.From(key),
            this.storageAlias().getBytes(StandardCharsets.UTF_8)
        );
        this.data.save(new Key.From(RepoDataTest.REPO, "first.txt"), new byte[]{});
        this.data.save(new Key.From(RepoDataTest.REPO, "second.txt"), new byte[]{});
        new RepoData(this.storage, this.cache).move(
            new RepositoryName.Simple(RepoDataTest.REPO), new RepositoryName.Simple("new-repo")
        ).toCompletableFuture().join();
        MatcherAssert.assertThat(
            "Repository data are moved",
            this.waitCondition(
                () ->
                    this.data.list(Key.ROOT).stream()
                        .map(Key::string).toList()
                        .containsAll(List.of("new-repo/first.txt", "new-repo/second.txt"))
            )
        );
    }

    @ParameterizedTest
    @ValueSource(
        strings = {"_storages.yaml", "my-repo/_storages.yaml"}
    )
    void removesDataWithAlias(final String key) {
        this.stngs.save(
            new Key.From(String.format("%s.yml", RepoDataTest.REPO)),
            this.repoSettingsWithAlias().getBytes(StandardCharsets.UTF_8)
        );
        this.stngs.save(
            new Key.From(key), this.storageAlias().getBytes(StandardCharsets.UTF_8)
        );
        this.data.save(new Key.From(RepoDataTest.REPO, "first.txt"), new byte[]{});
        this.data.save(new Key.From(RepoDataTest.REPO, "second.txt"), new byte[]{});
        new RepoData(this.storage, this.cache)
            .remove(new RepositoryName.Simple(RepoDataTest.REPO)).toCompletableFuture().join();
        MatcherAssert.assertThat(
            "Repository data are moved",
            this.waitCondition(() -> this.data.list(Key.ROOT).isEmpty())
        );
    }

    private String repoSettings() {
        return String.join(
            System.lineSeparator(),
            "repo:",
            "  type: binary",
            "  storage:",
            "    type: fs",
            String.format("    path: %s", this.temp.toString())
        );
    }

    private String repoSettingsWithAlias() {
        return String.join(
            System.lineSeparator(),
            "repo:",
            "  type: binary",
            "  storage: local"
        );
    }

    private String storageAlias() {
        return String.join(
            System.lineSeparator(),
            "storages:",
            "  default:",
            "    type: fs",
            "    path: /usr/def",
            "  local:",
            "    type: fs",
            String.format("    path: %s", this.temp.toString())
        );
    }

    /**
     * Awaiting of action during maximum 5 seconds.
     * Allows to wait result of action during period of time.
     * @param action Action
     * @return Result of action
     */
    private Boolean waitCondition(final Supplier<Boolean> action) {
        final long max = System.currentTimeMillis() + RepoDataTest.MAX_WAIT;
        boolean res;
        do {
            res = action.get();
            if (res) {
                break;
            } else {
                try {
                    TimeUnit.MILLISECONDS.sleep(RepoDataTest.SLEEP_DURATION);
                } catch (final InterruptedException exc) {
                    break;
                }
            }
        } while (System.currentTimeMillis() < max);
        if (!res) {
            res = action.get();
        }
        return res;
    }
}
