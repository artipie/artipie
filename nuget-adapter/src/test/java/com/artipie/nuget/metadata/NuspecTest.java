/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.nuget.metadata;

import com.artipie.asto.test.TestResource;
import com.artipie.nuget.NewtonJsonResource;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Test for {@link Nuspec.Xml}.
 * @since 0.6
 */
@SuppressWarnings({"PMD.AvoidDuplicateLiterals", "PMD.TooManyMethods"})
class NuspecTest {

    @Test
    void returnsBytes() {
        final byte[] nuspec = new NewtonJsonResource("newtonsoft.json.nuspec").bytes();
        MatcherAssert.assertThat(
            new Nuspec.Xml(nuspec).bytes(),
            new IsEqual<>(nuspec)
        );
    }

    @Test
    void readsVersion() {
        MatcherAssert.assertThat(
            new Nuspec.Xml(new NewtonJsonResource("newtonsoft.json.nuspec").bytes())
                .version().raw(),
            new IsEqual<>("12.0.3")
        );
    }

    @Test
    void readsId() {
        MatcherAssert.assertThat(
            new Nuspec.Xml(new NewtonJsonResource("newtonsoft.json.nuspec").bytes())
                .id().raw(),
            new IsEqual<>("Newtonsoft.Json")
        );
    }

    @Test
    void readsAuthors() {
        MatcherAssert.assertThat(
            new Nuspec.Xml(new NewtonJsonResource("newtonsoft.json.nuspec").bytes())
                .authors(),
            new IsEqual<>("James Newton-King")
        );
    }

    @Test
    void readsDescription() {
        MatcherAssert.assertThat(
            new Nuspec.Xml(new NewtonJsonResource("newtonsoft.json.nuspec").bytes())
                .description(),
            new IsEqual<>("Json.NET is a popular high-performance JSON framework for .NET")
        );
    }

    @ParameterizedTest
    @CsvSource({
        "TITLE,Json.NET",
        "LICENSE_URL,https://licenses.nuget.org/MIT",
        "REQUIRE_LICENSE_ACCEPTANCE,false",
        "TAGS,json",
        "PROJECT_URL,https://www.newtonsoft.com/json"
    })
    void readsOptField(final OptFieldName name, final String val) {
        MatcherAssert.assertThat(
            new Nuspec.Xml(new NewtonJsonResource("newtonsoft.json.nuspec").bytes())
                .fieldByName(name).get(),
            new IsEqual<>(val)
        );
    }

    @Test
    void returnsEmptyWhenFieldIsAbsent() {
        MatcherAssert.assertThat(
            new Nuspec.Xml(new NewtonJsonResource("newtonsoft.json.nuspec").bytes())
                .fieldByName(OptFieldName.RELEASE_NOTES).isPresent(),
            new IsEqual<>(false)
        );
    }

    @Test
    void returnsDependencies() {
        MatcherAssert.assertThat(
            new Nuspec.Xml(new NewtonJsonResource("newtonsoft.json.nuspec").bytes())
                .dependencies(),
            Matchers.containsInAnyOrder(
                "::.NETFramework2.0", "::.NETFramework3.5", "::.NETFramework4.0",
                "::.NETFramework4.5", "::.NETPortable0.0-Profile259",
                "::.NETPortable0.0-Profile328", "Microsoft.CSharp:4.3.0:.NETStandard1.0",
                "NETStandard.Library:1.6.1:.NETStandard1.0",
                "System.ComponentModel.TypeConverter:4.3.0:.NETStandard1.0",
                "System.Runtime.Serialization.Primitives:4.3.0:.NETStandard1.0",
                "Microsoft.CSharp:4.3.0:.NETStandard1.3",
                "NETStandard.Library:1.6.1:.NETStandard1.3",
                "System.ComponentModel.TypeConverter:4.3.0:.NETStandard1.3",
                "System.Runtime.Serialization.Formatters:4.3.0:.NETStandard1.3",
                "System.Runtime.Serialization.Primitives:4.3.0:.NETStandard1.3",
                "System.Xml.XmlDocument:4.3.0:.NETStandard1.3", "::.NETStandard2.0"
            )
        );
    }

    @Test
    void readsDependenciesInAllPossibleFormats() {
        MatcherAssert.assertThat(
            new Nuspec.Xml(new TestResource("deps-format.nuspec").asBytes()).dependencies(),
            Matchers.containsInAnyOrder(
                "RouteMagic:1.1.0:", "jQuery:1.6.2:.NETFramework4.7.2",
                "WebActivator:1.4.4:.NETFramework4.7.2", "::netcoreapp3.1"
            )
        );
    }

    @Test
    void returnsEmptyWhenPackageTypesAreAbsent() {
        MatcherAssert.assertThat(
            new Nuspec.Xml(new NewtonJsonResource("newtonsoft.json.nuspec").bytes())
                .packageTypes(),
            Matchers.emptyIterable()
        );
    }

    @Test
    void readsPackagesTypes() {
        MatcherAssert.assertThat(
            new Nuspec.Xml(new TestResource("types-format.nuspec").asBytes()).packageTypes(),
            Matchers.containsInAnyOrder("PackageType1:1.0.0.0", "PackageType2:")
        );
    }

    @Test
    void readsMinClientVersion() {
        MatcherAssert.assertThat(
            new Nuspec.Xml(new TestResource("types-format.nuspec").asBytes()).minClientVersion()
                .get(),
            new IsEqual<>("2.12")
        );
    }

    @Test
    void returnsEmptyWhenMinClientVersionIsAbsent() {
        MatcherAssert.assertThat(
            new Nuspec.Xml("<package><metadata><id>Any</id></metadata></package>".getBytes())
                .minClientVersion().isPresent(),
            new IsEqual<>(false)
        );
    }

}
