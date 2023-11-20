/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/http/blob/master/LICENSE.txt
 */
package com.artipie.scheduling;

import java.util.Objects;

/**
 * Artifact data record.
 * @since 1.3
 */
@SuppressWarnings("PMD.AvoidFieldNameMatchingMethodName")
public final class ArtifactEvent {

    /**
     * Default value for owner when owner is not found or irrelevant.
     */
    public static final String DEF_OWNER = "UNKNOWN";

    /**
     * Repository type.
     */
    private final String rtype;

    /**
     * Repository name.
     */
    private final String rname;

    /**
     * Owner username.
     */
    private final String owner;

    /**
     * Event type.
     */
    private final Type etype;

    /**
     * Artifact name.
     */
    private final String aname;

    /**
     * Artifact version.
     */
    private final String version;

    /**
     * Package size.
     */
    private final long size;

    /**
     * Artifact uploaded time.
     */
    private final long created;

    /**
     * Ctor for the event to remove all artifact versions.
     * @param rtype Repository type
     * @param rname Repository name
     * @param aname Artifact name
     * @checkstyle ParameterNumberCheck (5 lines)
     */
    public ArtifactEvent(final String rtype, final String rname, final String aname) {
        this(rtype, rname, ArtifactEvent.DEF_OWNER, aname, "", 0L, 0L, Type.DELETE_ALL);
    }

    /**
     * Ctor for the event to remove artifact with specified version.
     * @param rtype Repository type
     * @param rname Repository name
     * @param aname Artifact name
     * @param version Artifact version
     * @checkstyle ParameterNumberCheck (5 lines)
     */
    public ArtifactEvent(final String rtype, final String rname,
        final String aname, final String version) {
        this(rtype, rname, ArtifactEvent.DEF_OWNER, aname, version, 0L, 0L, Type.DELETE_VERSION);
    }

    /**
     * Ctor.
     * @param rtype Repository type
     * @param rname Repository name
     * @param owner Owner username
     * @param aname Artifact name
     * @param version Artifact version
     * @param size Artifact size
     * @param created Artifact created date
     * @param etype Event type
     * @checkstyle ParameterNumberCheck (5 lines)
     */
    public ArtifactEvent(final String rtype, final String rname, final String owner,
        final String aname, final String version, final long size,
        final long created, final Type etype) {
        this.rtype = rtype;
        this.rname = rname;
        this.owner = owner;
        this.aname = aname;
        this.version = version;
        this.size = size;
        this.created = created;
        this.etype = etype;
    }

    /**
     * Ctor.
     * @param rtype Repository type
     * @param rname Repository name
     * @param owner Owner username
     * @param aname Artifact name
     * @param version Artifact version
     * @param size Artifact size
     * @param created Artifact created date
     * @checkstyle ParameterNumberCheck (5 lines)
     */
    public ArtifactEvent(final String rtype, final String rname, final String owner,
        final String aname, final String version, final long size,
        final long created) {
        this(rtype, rname, owner, aname, version, size, created, Type.INSERT);
    }

    /**
     * Ctor to insert artifact data with creation time {@link System#currentTimeMillis()}.
     * @param rtype Repository type
     * @param rname Repository name
     * @param owner Owner username
     * @param aname Artifact name
     * @param version Artifact version
     * @param size Artifact size
     * @checkstyle ParameterNumberCheck (5 lines)
     */
    public ArtifactEvent(final String rtype, final String rname, final String owner,
        final String aname, final String version, final long size) {
        this(rtype, rname, owner, aname, version, size, System.currentTimeMillis(), Type.INSERT);
    }

    /**
     * Repository identification.
     * @return Repo info
     */
    public String repoType() {
        return this.rtype;
    }

    /**
     * Repository identification.
     * @return Repo info
     */
    public String repoName() {
        return this.rname;
    }

    /**
     * Artifact identifier.
     * @return Repo id
     */
    public String artifactName() {
        return this.aname;
    }

    /**
     * Artifact identifier.
     * @return Repo id
     */
    public String artifactVersion() {
        return this.version;
    }

    /**
     * Package size.
     * @return Size of the package
     */
    public long size() {
        return this.size;
    }

    /**
     * Artifact uploaded time.
     * @return Created datetime
     */
    public long createdDate() {
        return this.created;
    }

    /**
     * Owner username.
     * @return Username
     */
    public String owner() {
        return this.owner;
    }

    /**
     * Event type.
     * @return The type of event
     */
    public Type eventType() {
        return this.etype;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.rname, this.aname, this.version, this.etype);
    }

    @Override
    public boolean equals(final Object other) {
        final boolean res;
        if (this == other) {
            res = true;
        } else if (other == null || getClass() != other.getClass()) {
            res = false;
        } else {
            final ArtifactEvent that = (ArtifactEvent) other;
            res = that.rname.equals(this.rname) && that.aname.equals(this.aname)
                && that.version.equals(this.version) && that.etype.equals(this.etype);
        }
        return res;
    }

    /**
     * Events type.
     * @since 1.3
     */
    public enum Type {
        /**
         * Add artifact data.
         */
        INSERT,

        /**
         * Remove artifact data by version.
         */
        DELETE_VERSION,

        /**
         * Remove artifact data by artifact name (all versions).
         */
        DELETE_ALL
    }

}
