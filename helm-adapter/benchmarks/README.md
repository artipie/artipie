## Benchmarking

To run benchmarks:
 1. Build `helm-bench` with `mvn package` from folder `benchmarks`
 2. Copy dependencies to `target/dependency/` dir: `mvn dependency:copy-dependencies`
 3. Create directory for tests and copy test resources to this directory
 4. Run benchmarks with `env BENCH_DIR=/tmp/helm-test java -cp "target/benchmarks.jar:target/classes/*:target/dependency/*" org.openjdk.jmh.Main BenchToRun`, where `/tmp/helm-test` is a directory with test data.

There are available bundles:
  - https://artipie.s3.amazonaws.com/helm-test/helm100.tar.gz
  
### Results
Results located in `benchmarks/result`. This folder includes subfolders whose names are versions of adapter
for which benchamrks were run. The name of file with results consists of three parts which are
divided by hyphen: benchmark class, date when benchmark was run, version of adapter. 
  
**Available benchmark classes**
--
 
### HelmAstoRemoveBench 

This benchmark class removes Helm charts from the repository index files, it works with 
`com.artipie.helm.Helm.Asto#delete(Collection<Key>, Key)` method and requires unpacked tgz archives in the test directory
and index file which contains information about them. Available bundles are specified above.

### HelmAstoAddBench

`HelmAstoAddBench` updates repository metadata with the collection of the provided keys of charts, 
it works with `com.artipie.helm.Helm.Asto#add(Collection<Key>, Key)` method and requires unpacked tgz archives to add 
in the test directory. Available bundles are specified above.

### HelmAstoReindexBench

`HelmAstoReindexBench` reindexes whole repository, it works with `com.artipie.helm.Helm.Asto#reindex(Key)` 
method and requires unpacked tgz archives in the test directory. Index repository could be absent 
or malformed. Available bundles are specified above.
