/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/artipie/blob/master/LICENSE.txt
 */
package com.artipie.scheduling;

import java.util.Objects;

/**
 * Artifact data record.
 */
public final class ArtifactEvent {

    /**
     * Default value for owner when owner is not found or irrelevant.
     */
    public static final String DEF_OWNER = "UNKNOWN";

    /**
     * Repository type.
     */
    private final String repoType;

    /**
     * Repository name.
     */
    private final String repoName;

    /**
     * Owner username.
     */
    private final String owner;

    /**
     * Event type.
     */
    private final Type eventType;

    /**
     * Artifact name.
     */
    private final String artifactName;

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
     * @param repoType Repository type
     * @param repoName Repository name
     * @param artifactName Artifact name
     */
    public ArtifactEvent(String repoType, String repoName, String artifactName) {
        this(repoType, repoName, ArtifactEvent.DEF_OWNER, artifactName, "", 0L, 0L, Type.DELETE_ALL);
    }

    /**
     * Ctor for the event to remove artifact with specified version.
     * @param repoType Repository type
     * @param repoName Repository name
     * @param artifactName Artifact name
     * @param version Artifact version
     */
    public ArtifactEvent(String repoType, String repoName,
                         String artifactName, String version) {
        this(repoType, repoName, ArtifactEvent.DEF_OWNER, artifactName, version, 0L, 0L, Type.DELETE_VERSION);
    }

    /**
     * @param repoType Repository type
     * @param repoName Repository name
     * @param owner Owner username
     * @param artifactName Artifact name
     * @param version Artifact version
     * @param size Artifact size
     * @param created Artifact created date
     * @param etype Event type
     */
    public ArtifactEvent(String repoType, String repoName, String owner,
                         String artifactName, String version, long size,
                         long created, Type etype) {
        this.repoType = repoType;
        this.repoName = repoName;
        this.owner = owner;
        this.artifactName = artifactName;
        this.version = version;
        this.size = size;
        this.created = created;
        this.eventType = etype;
    }

    /**
     * @param repoType Repository type
     * @param repoName Repository name
     * @param owner Owner username
     * @param artifactName Artifact name
     * @param version Artifact version
     * @param size Artifact size
     * @param created Artifact created date
     */
    public ArtifactEvent(final String repoType, final String repoName, final String owner,
                         final String artifactName, final String version, final long size,
                         final long created) {
        this(repoType, repoName, owner, artifactName, version, size, created, Type.INSERT);
    }

    /**
     * Ctor to insert artifact data with creation time {@link System#currentTimeMillis()}.
     * @param repoType Repository type
     * @param repoName Repository name
     * @param owner Owner username
     * @param artifactName Artifact name
     * @param version Artifact version
     * @param size Artifact size
     */
    public ArtifactEvent(final String repoType, final String repoName, final String owner,
                         final String artifactName, final String version, final long size) {
        this(repoType, repoName, owner, artifactName, version, size, System.currentTimeMillis(), Type.INSERT);
    }

    /**
     * Repository identification.
     * @return Repo info
     */
    public String repoType() {
        return this.repoType;
    }

    /**
     * Repository identification.
     * @return Repo info
     */
    public String repoName() {
        return this.repoName;
    }

    /**
     * Artifact identifier.
     * @return Repo id
     */
    public String artifactName() {
        return this.artifactName;
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
        return this.eventType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.repoName, this.artifactName, this.version, this.eventType);
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
            res = that.repoName.equals(this.repoName) && that.artifactName.equals(this.artifactName)
                && that.version.equals(this.version) && that.eventType.equals(this.eventType);
        }
        return res;
    }

    @Override
    public String toString() {
        return "ArtifactEvent{" +
            "repoType='" + repoType + '\'' +
            ", repoName='" + repoName + '\'' +
            ", owner='" + owner + '\'' +
            ", eventType=" + eventType +
            ", artifactName='" + artifactName + '\'' +
            ", version='" + version + '\'' +
            ", size=" + size +
            ", created=" + created +
            '}';
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
