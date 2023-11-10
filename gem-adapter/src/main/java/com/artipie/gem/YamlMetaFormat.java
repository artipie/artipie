/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.gem;

import com.amihaiemil.eoyaml.Yaml;
import com.amihaiemil.eoyaml.YamlMapping;
import com.amihaiemil.eoyaml.YamlMappingBuilder;
import com.amihaiemil.eoyaml.YamlSequenceBuilder;
import com.artipie.gem.GemMeta.MetaFormat;
import com.artipie.gem.GemMeta.MetaInfo;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

/**
 * New JSON format for Gem meta info.
 *
 * @since 1.0
 */
public final class YamlMetaFormat implements MetaFormat {

    /**
     * Yaml transformations consumer.
     */
    private final Consumer<UnaryOperator<YamlMappingBuilder>> yamler;

    /**
     * New yaml format.
     * @param yamler Yaml transformation consumer
     */
    public YamlMetaFormat(final Consumer<UnaryOperator<YamlMappingBuilder>> yamler) {
        this.yamler = yamler;
    }

    @Override
    public void print(final String name, final String value) {
        this.yamler.accept(yaml -> yaml.add(name, value));
    }

    @Override
    public void print(final String name, final MetaInfo value) {
        final Yamler child = new Yamler();
        value.print(new YamlMetaFormat(child));
        this.yamler.accept(yaml -> yaml.add(name, child.build()));
    }

    @Override
    public void print(final String name, final String[] values) {
        YamlSequenceBuilder seqi = Yaml.createYamlSequenceBuilder();
        for (final String item : values) {
            seqi = seqi.add(item);
        }
        final YamlSequenceBuilder seq = seqi;
        this.yamler.accept(yaml -> yaml.add(name, seq.build()));
    }

    /**
     * Yaml tranformation consumer with volatile in-memory state.
     * @implNote This implementation is not thread safe
     * @since 1.3
     */
    public static final class Yamler implements Consumer<UnaryOperator<YamlMappingBuilder>> {

        /**
         * Memory for yaml builder.
         */
        private volatile YamlMappingBuilder yaml;

        /**
         * New yaml transformation consumer.
         */
        public Yamler() {
            this(Yaml.createYamlMappingBuilder());
        }

        /**
         * New Yaml tranfsormation consumer with initial state.
         * @param yaml Initial Yaml builder
         */
        public Yamler(final YamlMappingBuilder yaml) {
            this.yaml = yaml;
        }

        @Override
        public void accept(final UnaryOperator<YamlMappingBuilder> transform) {
            this.yaml = transform.apply(this.yaml);
        }

        /**
         * Build yaml from curren state.
         * @return Yaml mapping
         */
        public YamlMapping build() {
            return this.yaml.build();
        }
    }
}
