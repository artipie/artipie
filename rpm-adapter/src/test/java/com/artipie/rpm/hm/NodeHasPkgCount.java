/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.rpm.hm;

import com.jcabi.xml.XMLDocument;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.hamcrest.core.IsEqual;
import org.llorllale.cactoos.matchers.MatcherEnvelope;
import org.llorllale.cactoos.matchers.MatcherOf;

/**
 * Metadata has given amount of packages.
 * @since 0.10
 */
public final class NodeHasPkgCount extends MatcherEnvelope<XMLDocument> {

    /**
     * RegEx pattern for packages attribute.
     */
    private static final Pattern ATTR = Pattern.compile("packages=\"(\\d+)\"");

    /**
     * Ctor.
     * @param count Expected packages count
     * @param tag Xml tag
     */
    public NodeHasPkgCount(final int count, final String tag) {
        super(
            new MatcherOf<>(
                xml -> new IsEqual<>(count).matches(countPackages(tag, xml))
                    && new IsEqual<>(count).matches(packagesAttributeValue(xml)),
                desc -> desc.appendText(String.format("%d packages expected", count)),
                (xml, desc) -> desc.appendText(
                    String.format(
                        "%d packages found, 'packages' attribute value is %d",
                        countPackages(tag, xml), packagesAttributeValue(xml)
                    )
                )
            )
        );
    }

    /**
     * Count packages in XMLDocument.
     * @param tag Tag
     * @param xml Xml document
     * @return Packages count
     */
    private static int countPackages(final String tag, final XMLDocument xml) {
        return Integer.parseInt(
            xml.xpath(
                String.format("count(/*[local-name()='%s']/*[local-name()='package'])", tag)
            ).get(0)
        );
    }

    /**
     * Returns `packages` attribute value.
     * @param xml Xml document
     * @return Value of the attribute
     */
    private static int packagesAttributeValue(final XMLDocument xml) {
        final Matcher matcher = ATTR.matcher(xml.toString());
        int res = Integer.MIN_VALUE;
        if (matcher.find()) {
            res = Integer.parseInt(matcher.group(1));
        }
        return res;
    }

}
