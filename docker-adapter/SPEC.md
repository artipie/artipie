# Docker registry file structure

The storage backend layout is broken up into a content-addressable blob
store and repositories. The content-addressable blob store holds most data
throughout the backend, keyed by algorithm and digests of the underlying
content. Access to the blob store is controlled through links from the
repository to blobstore.

A repository is made up of layers, manifests and tags. The layers component
is just a directory of layers which are "linked" into a repository. A layer
can only be accessed through a qualified repository name if it is linked in
the repository. Uploads of layers are stored in `_uploads` directory by upload UUID. 
When all data for an upload is received, the
data is moved into the blob store and the upload directory is deleted.
Abandoned uploads can be garbage collected by reading the startedat file
and removing uploads that have been active for longer than a certain time.

The third component of the repository directory is the manifests store,
which is made up of a revision store and tag store. Manifests are stored in
the blob store and linked into the revision store.
Registry stores all revisions of a manifest. Revisions are stored
by digest and history of changes (which revision was first, which one was second etc.)
is not preserved. The tag store provides
support for name, tag lookups of manifests, using "current/link" under a
named tag directory. An index is maintained to support deletions of all
revisions of a given manifest tag.

We cover the path formats implemented by this path mapper below.

Manifests:

```
manifestRevisionsPathSpec:      <root>/v2/repositories/<name>/_manifests/revisions/
manifestRevisionPathSpec:      <root>/v2/repositories/<name>/_manifests/revisions/<algorithm>/<hex digest>/
manifestRevisionLinkPathSpec:  <root>/v2/repositories/<name>/_manifests/revisions/<algorithm>/<hex digest>/link
```

Tags:

```
manifestTagsPathSpec:                  <root>/v2/repositories/<name>/_manifests/tags/
manifestTagPathSpec:                   <root>/v2/repositories/<name>/_manifests/tags/<tag>/
manifestTagCurrentPathSpec:            <root>/v2/repositories/<name>/_manifests/tags/<tag>/current/link
manifestTagIndexPathSpec:              <root>/v2/repositories/<name>/_manifests/tags/<tag>/index/
manifestTagIndexEntryPathSpec:         <root>/v2/repositories/<name>/_manifests/tags/<tag>/index/<algorithm>/<hex digest>/
manifestTagIndexEntryLinkPathSpec:     <root>/v2/repositories/<name>/_manifests/tags/<tag>/index/<algorithm>/<hex digest>/link
```

Blobs:

```
layerLinkPathSpec:            <root>/v2/repositories/<name>/_layers/<algorithm>/<hex digest>/link
layersPathSpec:               <root>/v2/repositories/<name>/_layers
```

Uploads:

```
uploadDataPathSpec:             <root>/v2/repositories/<name>/_uploads/<id>/data
uploadStartedAtPathSpec:        <root>/v2/repositories/<name>/_uploads/<id>/startedat
uploadHashStatePathSpec:        <root>/v2/repositories/<name>/_uploads/<id>/hashstates/<algorithm>/<offset>
```

Blob Store:

```
blobsPathSpec:                  <root>/v2/blobs/
blobPathSpec:                   <root>/v2/blobs/<algorithm>/<first two hex bytes of digest>/<hex digest>
blobDataPathSpec:               <root>/v2/blobs/<algorithm>/<first two hex bytes of digest>/<hex digest>/data
blobMediaTypePathSpec:          <root>/v2/blobs/<algorithm>/<first two hex bytes of digest>/<hex digest>/data
```

For more information on the semantic meaning of each path and their
contents, please see the path spec documentation.

## Repository name

Classically, repository names have always been two path components where each path component
is less than 30 characters.
The V2 registry API does not enforce this.
The rules for a repository name are as follows:
 - A repository name is broken up into path components.
 A component of a repository name must be at least one lowercase,
 alpha-numeric characters, optionally separated by periods, dashes or underscores.
 More strictly, it must match the regular expression: `[a-z0-9]+(?:[._-][a-z0-9]+)*`.
 - If a repository name has two or more path components,
 they must be separated by a forward slash (`/`).
 - The total length of a repository name, including slashes,
 must be less than 256 characters.

## Manifest resolution strategy

Manifest can be resolved by repo "name" and "reference". The reference may include a tag
or digest. Manifest is represented as a JSON object. It can be resolved using these steps:
 1. Resolve tag or reference by reading a file at
 `docker/registry/v2/repositories/<name>/_manifests`: `revisions/<alg>/<digest>/link` for
 references and `tags/<tag>/current/link` for tags. The file content has a digest of
 image blob in plain text format: `<alg>:<hex>`, where `<alg>` is algorithm name, `hex`
 is hex-encoded hash sum of the digest.
 2. Extract content from blob file located at
 `docker/registry/v2/blobs/<alg>/<prefix>/<hex>/data`, where `<alg>` and `<hex>` are values from
 previous step, `<prefix>` is first two chars of `<hex>` string.
 3. Parse this file as JSON object. It contains all manifest metadata including layers info.

