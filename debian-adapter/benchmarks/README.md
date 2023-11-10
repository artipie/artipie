# Debian benchmarks

Here is general algorithm to run benchmarks:
 1. Install snapshot locally of `debian-adapter`: `mvn install`
 2. Build `debian-bench` project: `mvn package -f ./benchmarks`
 3. Copy dependencies to `target/dependency/` directory: `mvn dependency:copy-dependencies`
 4. Create directory and copy resources required for test into this directory
 5. Run benchmarks with `env BENCH_DIR=/tmp/debian-test java -cp "target/benchmarks.jar:target/classes/*:target/dependency/*" org.openjdk.jmh.Main BenchToRun`, 
 where `/tmp/debian-test` is a directory with resources for tests, `BenchToRun` is benchmark class name.

There are several benchmarks in debian-adapter: `com.artipie.debian.benchmarks.IndexMergeBench` to 
test indexes merging and `com.artipie.debian.benchmarks.RepoUpdateBench` for generation of 
repository indexes test.

## Benchmarks

### IndexMergeBench

`IndexMergeBench` calls `MultiPackages.Unique.merge()` to perform Packages indexes merging. To run 
this benchmark it's necessary to provide gziped Packages indexes in the test directory, all the 
files from the directory will be merged. Sample Packages indexes can be found 
[here](https://artipie.s3.amazonaws.com/debian-test/debian-merge.tar.gz).

### RepoUpdateBench 

`RepoUpdateBench` works with `Debian.Asto` to first generate Packages.gz index and Release index 
second. To run this benchmark it's necessary to provide `.deb` files and Packages.gz files in 
the test directory. The first ones will be used to create Packages.gz index, and the second ones - 
to create Release index. Sample data for this benchmark can be downloaded 
[here](https://artipie.s3.amazonaws.com/debian-test/debian-repo.tar.gz). 