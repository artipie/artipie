#!/bin/sh
set -e

BUNDLE="bundle100"
while getopts b: flag
do
    case "${flag}" in
        b) BUNDLE=${OPTARG};;
    esac
done

TMPDIR=$(mktemp --directory)

mvn package -Pbench
wget https://artipie.s3.amazonaws.com/rpm-test/${BUNDLE}.tar.gz
tar -xvzf ${BUNDLE}.tar.gz -C ${TMPDIR}
mvn dependency:copy-dependencies
num=$(echo ${BUNDLE} | cut -c7-11)
env BENCH_DIR=${TMPDIR}/bundle/${num} java -cp "target/benchmarks.jar:target/classes/*:target/dependency/*" org.openjdk.jmh.Main RpmBench > ${TMPDIR}/out.txt
tail -2 ${TMPDIR}/out.txt
rm -rf ${TMPDIR}
