# RPM adapter benchmarks

To run benchmarks:
 1. Install snapshot locally of `rpm-adapter`: `mvn install`
 2. Build `rpm-bench` project: `mvn package -f ./benchmarks`
 4. Create directory for tests and copy test resources to this directory
 5. Run benchmarks with `env BENCH_DIR=${test-dir} java -cp "benchmarks/target/benchmarks.jar" org.openjdk.jmh.Main ${bench-name}`, where `${test-dir}` is a directory with test data, and `${bench-name}` is a benchbmark name.

## Benchmarks

### RpmBench

This benchmark class creates/updates repository indexes over provided RPM packages, it calls
`com.artipie.rpm.Rpm.batchUpdateIncrementally` and requires the set of the RPMs in the test directory.
There are available bundles:
  - https://artipie.s3.amazonaws.com/rpm-test/bundle100.tar.gz
  - https://artipie.s3.amazonaws.com/rpm-test/bundle1000.tar.gz

### RpmMetadataRemoveBench

This benchmark class removes RPM packages records from the repository index files, it works with
`com.artipie.rpm.RpmMetadata.Remove` class and requires xml (unpacked) indexes in the test directory.
Example repository index xmls can be found
[here](https://artipie.s3.amazonaws.com/rpm-test/centos-7-os-x86_64-repodata.tar.gz).

### RpmMetadataAppendBench

`RpmMetadataAppendBench` updates repository metadata with the list of the provided `.rpm` packages,
it works with `com.artipie.rpm.RpmMetadata.Append` class and requires xml (unpacked) indexes and
`.rpm` packages to add in the test directory. Example data set for the benchmark can be found
[here](https://artipie.s3.amazonaws.com/rpm-test/rpm-metadata-append-bench.tar.gz).
