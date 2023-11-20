/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.rpm;

import com.amihaiemil.eoyaml.Node;
import com.amihaiemil.eoyaml.Yaml;
import com.amihaiemil.eoyaml.YamlMapping;
import com.amihaiemil.eoyaml.YamlNode;
import com.artipie.ArtipieException;
import java.util.Locale;
import java.util.Optional;

/**
 * Repository configuration.
 * @since 0.10
 */
public interface RepoConfig {

    /**
     * Repository digest.
     * @return Digest algorithm
     */
    Digest digest();

    /**
     * Repository naming policy.
     * @return Naming
     */
    NamingPolicy naming();

    /**
     * Is filelists.xml metadata required?
     * @return True if filelists.xml is needed, false otherwise
     */
    boolean filelists();

    /**
     * Repository update mode, default is {@link UpdateMode#UPLOAD}.
     * @return Instance of {@link UpdateMode}
     * @throws ArtipieException When configuration is invalid
     */
    UpdateMode mode();

    /**
     * Schedule to update repository in cron format, available for {@link UpdateMode#CRON} only.
     * @return Cron update schedule
     * @throws ArtipieException When configuration is invalid
     */
    Optional<String> cron();

    /**
     * Repository name.
     * @return String name
     */
    String name();

    /**
     * Rpm repository update mode.
     * @since 1.9
     */
    enum UpdateMode {

        /**
         * Update on upload: repository is updated when HTTP methods
         * to upload/remove package are called.
         */
        UPLOAD,

        /**
         * Repository is updated by schedule.
         */
        CRON
    }

    /**
     * Repository configuration from yaml.
     * @since 0.10
     */
    final class FromYaml implements RepoConfig {

        /**
         * Update yaml section.
         */
        private static final String UPDATE = "update";

        /**
         * Cron yaml mapping in yaml config.
         */
        private static final String CRON = "cron";

        /**
         * Settings.
         */
        private final YamlMapping yaml;

        /**
         * Repository name.
         */
        private final String name;

        /**
         * Ctor.
         * @param yaml Yaml settings
         * @param name Repository name
         */
        public FromYaml(final YamlMapping yaml, final String name) {
            this.yaml = yaml;
            this.name = name;
        }

        /**
         * Ctor.
         * @param yaml Yaml settings
         * @param name Repository name
         */
        public FromYaml(final Optional<YamlMapping> yaml, final String name) {
            this(yaml.orElse(Yaml.createYamlMappingBuilder().build()), name);
        }

        @Override
        public Digest digest() {
            return Optional.ofNullable(this.yaml.string(RpmOptions.DIGEST.optionName()))
                .map(dgst -> Digest.valueOf(dgst.toUpperCase(Locale.US))).orElse(Digest.SHA256);
        }

        @Override
        public NamingPolicy naming() {
            return Optional.ofNullable(this.yaml.string(RpmOptions.NAMING_POLICY.optionName()))
                .map(naming -> StandardNamingPolicy.valueOf(naming.toUpperCase(Locale.US)))
                .orElse(StandardNamingPolicy.SHA256);
        }

        @Override
        public boolean filelists() {
            return !Boolean.FALSE.toString()
                .equals(this.yaml.string(RpmOptions.FILELISTS.optionName()));
        }

        @Override
        public UpdateMode mode() {
            return Optional.ofNullable(this.yaml.yamlMapping(FromYaml.UPDATE)).map(
                upd -> {
                    final YamlNode node = upd.value("on");
                    final UpdateMode res;
                    if (node.type() == Node.MAPPING
                        && node.asMapping().value(FromYaml.CRON) != null) {
                        res = UpdateMode.CRON;
                    } else if (node.type() == Node.SCALAR
                        && node.asScalar().value().equals("upload")) {
                        res = UpdateMode.UPLOAD;
                    } else {
                        throw new ArtipieException(
                            "Repository settings section `upload` is incorrectly configured"
                        );
                    }
                    return res;
                }
            ).orElse(UpdateMode.UPLOAD);
        }

        @Override
        public Optional<String> cron() {
            Optional<String> res = Optional.empty();
            if (this.mode() == UpdateMode.CRON) {
                res = Optional.of(
                    this.yaml.yamlMapping(FromYaml.UPDATE).yamlMapping("on").string(FromYaml.CRON)
                );
            }
            return res;
        }

        @Override
        public String name() {
            return this.name;
        }
    }

    /**
     * Simple.
     * @since 0.10
     */
    final class Simple implements RepoConfig {

        /**
         * Digest.
         */
        private final Digest dgst;

        /**
         * Naming policy.
         */
        private final NamingPolicy npolicy;

        /**
         * Is filelist needed?
         */
        private final boolean filelist;

        /**
         * Is filelist needed?
         */
        private final RepoConfig.UpdateMode umode;

        /**
         * Ctor.
         * @param dgst Digest
         * @param npolicy Naming policy
         * @param filelist Filelist
         * @param umode Update mode
         * @checkstyle ParameterNumberCheck (5 lines)
         */
        public Simple(final Digest dgst, final NamingPolicy npolicy, final boolean filelist,
            final RepoConfig.UpdateMode umode) {
            this.dgst = dgst;
            this.npolicy = npolicy;
            this.filelist = filelist;
            this.umode = umode;
        }

        /**
         * Ctor.
         * @param dgst Digest
         * @param npolicy Naming policy
         * @param filelist Filelist
         */
        public Simple(final Digest dgst, final NamingPolicy npolicy, final boolean filelist) {
            this(dgst, npolicy, filelist, UpdateMode.UPLOAD);
        }

        /**
         * Ctor.
         */
        public Simple() {
            this(Digest.SHA256, StandardNamingPolicy.PLAIN, false, UpdateMode.UPLOAD);
        }

        /**
         * Ctor.
         * @param umode Update mode
         */
        public Simple(final RepoConfig.UpdateMode umode) {
            this(Digest.SHA256, StandardNamingPolicy.PLAIN, false, umode);
        }

        @Override
        public Digest digest() {
            return this.dgst;
        }

        @Override
        public NamingPolicy naming() {
            return this.npolicy;
        }

        @Override
        public boolean filelists() {
            return this.filelist;
        }

        @Override
        public UpdateMode mode() {
            return this.umode;
        }

        @Override
        public Optional<String> cron() {
            return Optional.empty();
        }

        @Override
        public String name() {
            return "test";
        }
    }
}
