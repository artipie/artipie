## Artipie test images

Here's base images dockerfiles, mostly one image for every adapter type in the project.
Small changes in similar images often produce very different resulting docker image. 
Using single test image per adapter type greatly reduces total amount of network and disk usage by docker images.
It reduces time to fetch and start docker test images during CI runs too.
Also, test images prefetch and preinstall third-party packages required for test runs.
It reduces CI times even further, also reducing potential internet-related fails.
For maven client, AppCDS JVM cache is generated inside dockerfiles + tier 1 JVM JIT mode is enforced to reduce startup time of every related test.

Usage:
```
./build.sh
# test code locally...
./upload.sh
```

## Updating images

1. Update dockerfile locally for the target adapter.
2. Update corresponding image version in `build.sh`.
3. Build image locally via `./build.sh`
4. Test your artipie code locally
5. Update image version in `upload.sh`
6. Push to the artipie DockerHub via `upload.sh`.
7. Push your working branch for PR
