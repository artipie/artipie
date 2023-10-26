/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.rpm.meta;

import java.util.Comparator;
import org.apache.maven.artifact.versioning.ComparableVersion;

/**
 * Compare two dependencies by name.
 *
 * <p>It assumes that first parts must be same.
 * libc.so.6() < libc.so.6(GLIBC_2.3.4)(64 bit) < libc.so.6(GLIBC_2.4)</p>
 *
 * @see <a href="https://github.com/rpm-software-management/createrepo_c/blob/b49b8b2586c07d3e84009beba677162b86539f9d/src/parsehdr.c#L82">
 *  createrepo compare dependency implementation
 *  </a>
 * @since 1.9.9
 * @checkstyle ExecutableStatementCountCheck (500 lines)
 * @checkstyle JavaNCSSCheck (500 lines)
 * @checkstyle CyclomaticComplexityCheck (500 lines)
 * @checkstyle NestedIfDepthCheck (500 lines)
 */
@SuppressWarnings("PMD.CyclomaticComplexity")
public final class CrCompareDependency implements Comparator<String> {

    @Override
    public int compare(final String depa, final String depb) {
        final int result;
        if (depa.equals(depb)) {
            result = 0;
        } else {
            final int posa = depa.indexOf('(');
            final int posb = depb.indexOf('(');
            if (posa == -1 && posb == -1) {
                result = 0;
            } else if (posa == -1) {
                result = -1;
            } else if (posb == -1) {
                result = 1;
            } else {
                final int posea = depa.indexOf(')', posa + 1);
                final int poseb = depb.indexOf(')', posb + 1);
                if (posea == -1 && poseb == -1) {
                    throw new IllegalArgumentException(
                        "Wrong format for the names of dependencies !"
                    );
                } else if (posea == -1) {
                    result = -1;
                } else if (poseb == -1) {
                    result = 1;
                } else {
                    String vera = depa.substring(0, posa + 2);
                    String verb = depb.substring(0, posb + 2);
                    final String verea = depa.substring(0, posea + 1);
                    final String vereb = depb.substring(0, poseb + 1);
                    if (vera.equals(verea) && verb.equals(vereb)) {
                        result = 0;
                    } else if (vera.equals(verea)) {
                        result = -1;
                    } else if (verb.equals(vereb)) {
                        result = 1;
                    } else {
                        vera = toFirstNumberOfVersion(depa, vera.length() - 1);
                        verb = toFirstNumberOfVersion(depb, verb.length() - 1);
                        if (vera.compareTo(verea) > 0 && verb.compareTo(vereb) > 0) {
                            result = 0;
                        } else if (vera.compareTo(verea) > 0) {
                            result = -1;
                        } else if (verb.compareTo(vereb) > 0) {
                            result = 1;
                        } else {
                            vera = depa.substring(vera.length() - 1, verea.length() - 1);
                            verb = depb.substring(verb.length() - 1, vereb.length() - 1);
                            result = new ComparableVersion(vera).compareTo(
                                new ComparableVersion(verb)
                            );
                        }
                    }
                }
            }
        }
        return result;
    }

    /**
     * Extracts a substring from dependency name from start index
     * to the first version number.
     * @param dep Dependency name
     * @param stindex First character position
     * @return Substring
     */
    private static String toFirstNumberOfVersion(final String dep, final int stindex) {
        int pos = stindex;
        do {
            if (Character.isDigit(dep.charAt(pos))) {
                break;
            }
            pos += 1;
        } while (pos < dep.length());
        final String value;
        if (pos == dep.length()) {
            value = "";
        } else {
            value = dep.substring(0, pos + 1);
        }
        return value;
    }
}
