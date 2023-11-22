/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.debian.metadata;

import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.asto.test.TestResource;
import com.artipie.debian.AstoGzArchive;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.cactoos.list.ListOf;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.StringContains;
import org.hamcrest.text.StringContainsInOrder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link Package.Asto}.
 * @since 0.1
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 * @checkstyle MagicNumberCheck (500 lines)
 */
@SuppressWarnings({"PMD.AvoidDuplicateLiterals", "PMD.AssignmentInOperand"})
class PackageAstoTest {

    /**
     * Packages file index key.
     */
    private static final String KEY = "Packages.gz";

    /**
     * Test storage.
     */
    private Storage asto;

    @BeforeEach
    void init() {
        this.asto = new InMemoryStorage();
    }

    @Test
    void addsOnePackagesItem() throws IOException {
        new TestResource(PackageAstoTest.KEY).saveTo(this.asto);
        new Package.Asto(this.asto)
            .add(new ListOf<>(this.firstPackageInfo()), new Key.From(PackageAstoTest.KEY))
            .toCompletableFuture().join();
        MatcherAssert.assertThat(
            "Packages index has info about 3 packages",
            new AstoGzArchive(this.asto).unpack(new Key.From(PackageAstoTest.KEY)),
            new StringContainsInOrder(
                new ListOf<String>(
                    "Package: aglfn",
                    "Package: pspp",
                    "\n\n",
                    this.firstPackageInfo()
                )
            )
        );
        this.verifyThatTempDirIsCleanedUp();
    }

    @Test
    void addsSeveralPackagesItems() throws IOException {
        new TestResource(PackageAstoTest.KEY).saveTo(this.asto);
        new Package.Asto(this.asto).add(
            new ListOf<>(this.firstPackageInfo(), this.secondPackageInfo()),
            new Key.From(PackageAstoTest.KEY)
        ).toCompletableFuture().join();
        MatcherAssert.assertThat(
            "Packages index has info about 4 packages",
            new AstoGzArchive(this.asto).unpack(new Key.From(PackageAstoTest.KEY)),
            new StringContainsInOrder(
                new ListOf<String>(
                    "Package: aglfn",
                    "Package: pspp",
                    "\n\n",
                    this.firstPackageInfo(),
                    "\n\n",
                    this.secondPackageInfo()
                )
            )
        );
        this.verifyThatTempDirIsCleanedUp();
    }

    @Test
    void addsOnePackagesItemWhenIndexIsNew() throws IOException {
        new Package.Asto(this.asto)
            .add(new ListOf<>(this.firstPackageInfo()), new Key.From(PackageAstoTest.KEY))
            .toCompletableFuture().join();
        MatcherAssert.assertThat(
            "Packages index was created with added package",
            new AstoGzArchive(this.asto).unpack(new Key.From(PackageAstoTest.KEY)),
            new StringContains(this.firstPackageInfo())
        );
    }

    @Test
    void addsSeveralPackagesItemsWhenIndexIsNew() {
        new Package.Asto(this.asto)
            .add(
                new ListOf<>(this.firstPackageInfo(), this.secondPackageInfo()),
                new Key.From(PackageAstoTest.KEY)
            ).toCompletableFuture().join();
        MatcherAssert.assertThat(
            "Packages index was created with added packages",
            new AstoGzArchive(this.asto).unpack(new Key.From(PackageAstoTest.KEY)),
            new StringContainsInOrder(
                new ListOf<String>(
                    this.firstPackageInfo(),
                    "\n\n",
                    this.secondPackageInfo()
                )
            )
        );
    }

    private String firstPackageInfo() {
        return String.join(
            "\n",
            "Package: abc",
            "Version: 0.1",
            "Architecture: all",
            "Maintainer: Task Force",
            "Installed-Size: 130",
            "Section: The Force",
            "Filename: some/debian/package.deb",
            "Size: 23",
            "MD5sum: e99a18c428cb38d5f260853678922e03"
        );
    }

    private String secondPackageInfo() {
        return String.join(
            "\n",
            "Package: some package",
            "Version: 0.3",
            "Architecture: all",
            "Maintainer: Unknown",
            "Installed-Size: 45",
            "Section: The Unknown",
            "Filename: some/debian/unknown.deb",
            "Size: 23",
            "MD5sum: e99a18c428cb78d5f2608536128922e03"
        );
    }

    private void verifyThatTempDirIsCleanedUp() throws IOException {
        final Path systemtemp = Paths.get(System.getProperty("java.io.tmpdir"));
        MatcherAssert.assertThat(
            "Temp dir for indexes removed",
            Files.list(systemtemp)
                .noneMatch(path -> path.getFileName().toString().startsWith("packages")),
            new IsEqual<>(true)
        );
    }

}
