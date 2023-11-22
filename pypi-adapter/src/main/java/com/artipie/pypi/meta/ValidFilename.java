/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.pypi.meta;

import com.artipie.pypi.NormalizedProjectName;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Python package valid filename.
 * @since 0.6
 */
public final class ValidFilename {

    /**
     * Pattern to obtain package name from uploaded file name: for file name
     * 'Artipie-Testpkg-0.0.3.tar.gz', then package name is 'Artipie-Testpkg'.
     */
    private static final Pattern ARCHIVE_PTRN =
        Pattern.compile("(?<name>.*)-(?<version>[0-9a-z.]+?)\\.([a-zA-Z.]+)");

    /**
     * Python wheel package name pattern, for more details see
     * <a href="https://www.python.org/dev/peps/pep-0427/#file-name-convention">docs</a>.
     */
    private static final Pattern WHEEL_PTRN =
        Pattern.compile("(?<name>.*?)-(?<version>[0-9a-z.]+)(-\\d+)?-((py\\d.?)+)-(.*)-(.*).whl");

    /**
     * Package info data.
     */
    private final PackageInfo data;

    /**
     * Filename.
     */
    private final String filename;

    /**
     * Ctor.
     * @param data Package info data
     * @param filename Filename
     */
    public ValidFilename(final PackageInfo data, final String filename) {
        this.data = data;
        this.filename = filename;
    }

    /**
     * Is filename valid?
     * @return True if filename corresponds to project metadata, false - otherwise.
     */
    public boolean valid() {
        return Stream.of(
            ValidFilename.WHEEL_PTRN.matcher(this.filename),
            ValidFilename.ARCHIVE_PTRN.matcher(this.filename)
        ).filter(Matcher::matches).findFirst().map(
            matcher -> {
                final String name = new NormalizedProjectName.Simple(this.data.name()).value();
                return name.equals(new NormalizedProjectName.Simple(matcher.group("name")).value())
                    && this.data.version().equals(matcher.group("version"));
            }
        ).orElse(false);
    }

}
