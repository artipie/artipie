/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.rpm;

import org.apache.commons.cli.Option;

/**
 * Rpm repository configuration options. These options are used in rpm-adapter CLI mode and
 * on Artipie storage configuration level.
 * @since 0.10
 */
public enum RpmOptions {

    /**
     * Digest option.
     */
    DIGEST(
        "digest", "dgst",
        "(optional, default sha256) configures Digest instance for Rpm: sha256 or sha1"
    ),

    /**
     * Naming policy option.
     */
    NAMING_POLICY(
        "naming-policy", "np",
        "(optional, default plain) configures NamingPolicy for Rpm: plain, sha256 or sha1"
    ),

    /**
     * FileLists option.
     */
    FILELISTS(
        "filelists", "fl",
        "(optional, default true) includes File Lists for Rpm: true or false"
    ),

    /**
     * Update option allows to set schedule to update repository in cron format.
     */
    UPDATE(
        "update", "upd",
        "(optional) allows to set schedule to update repository in cron format"
    );

    /**
     * Option full name.
     */
    private final String name;

    /**
     * Command line argument.
     */
    private final String arg;

    /**
     * Description.
     */
    private final String desc;

    /**
     * Ctor.
     * @param name Option full ame
     * @param opt Option
     * @param desc Description
     */
    RpmOptions(final String name, final String opt, final String desc) {
        this.name = name;
        this.arg = opt;
        this.desc = desc;
    }

    /**
     * Option name.
     * @return String name
     */
    public final String optionName() {
        return this.name;
    }

    /**
     * Builds command line option.
     * @return Instance of {@link Option}.
     */
    public final Option option() {
        return Option.builder(this.name.substring(0, 1))
            .argName(this.arg)
            .longOpt(this.name)
            .desc(this.desc)
            .hasArg()
            .build();
    }
}
