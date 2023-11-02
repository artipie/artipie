/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/LICENSE.txt
 */
package com.artipie.gem.ruby;

import com.artipie.gem.GemMeta;
import java.nio.file.Path;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyObject;
import org.jruby.javasupport.JavaEmbedUtils;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * JRuby implementation of GemInfo metadata parser.
 * @since 1.0
 * @todo #103:30min Inspect rubygems API response to add more fields.
 *  Check responses for different Gem requests for origin rubygems.org
 *  or reverse-engineer Gem repository Ruby code to understand all fields
 *  that should be added to gems API response. Now all mandatory fields
 *  are present in this metadata genrator but different gem responses may have
 *  optional fields. E.g. https://rubygems.org/api/v1/gems/builder.json
 */
public final class RubyGemMeta implements GemMeta, SharedRuntime.RubyPlugin {

    /**
     * Ruby runtime.
     */
    private final Ruby ruby;

    /**
     * Ctor.
     * @param ruby Runtime
     */
    public RubyGemMeta(final Ruby ruby) {
        this.ruby = ruby;
    }

    @Override
    public MetaInfo info(final Path gem) {
        final RubyObject spec = (RubyObject) JavaEmbedUtils.newRuntimeAdapter().eval(
            this.ruby, String.format(
                "Gem::Package.new('%s').spec", gem.toString()
            )
        );
        return new RubyMetaInfo(spec);
    }

    @Override
    public String identifier() {
        return this.getClass().getCanonicalName();
    }

    @Override
    public void initialize() {
        JavaEmbedUtils.newRuntimeAdapter()
            .eval(this.ruby, "require 'rubygems/package.rb'");
    }

    /**
     * Meta info implementation for Ruby spec object.
     * @since 1.0
     */
    private static final class RubyMetaInfo implements MetaInfo {

        /**
         * Ruby meta spec object.
         */
        private final RubyObject spec;

        /**
         * New meta info.
         * @param spec Spec object
         */
        RubyMetaInfo(final RubyObject spec) {
            this.spec = spec;
        }

        @Override
        @SuppressWarnings("PMD.AvoidDuplicateLiterals")
        public void print(final MetaFormat fmt) {
            fmt.print("name", this.spec.getInstanceVariable("@name").asJavaString());
            fmt.print(
                "version",
                this.spec.getInstanceVariable("@version")
                    .getInstanceVariables()
                    .getInstanceVariable("@version")
                    .asJavaString()
            );
            fmt.print("platform", this.spec.getInstanceVariable("@platform").asJavaString());
            fmt.print(
                "authors",
                rubyToJavaStringArray(this.spec.getInstanceVariable("@authors").convertToArray())
            );
            fmt.print("info", this.spec.getInstanceVariable("@description").asJavaString());
            fmt.print(
                "licenses",
                rubyToJavaStringArray(this.spec.getInstanceVariable("@licenses").convertToArray())
            );
            fmt.print("homepage_uri", this.spec.getInstanceVariable("@homepage").asJavaString());
        }

        /**
         * Convert JRuby array to Java array of stirngs.
         * @param src JRuby array
         * @return String array
         */
        private static String[] rubyToJavaStringArray(final RubyArray<?> src) {
            final IRubyObject[] jarr = src.toJavaArray();
            final String[] res = new String[jarr.length];
            for (int id = 0; id < jarr.length; ++id) {
                res[id] = jarr[id].asJavaString();
            }
            return res;
        }
    }
}
