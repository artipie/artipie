#!/bin/sh -ex
baseDir="$(dirname $0)"
cd "$baseDir"
tar xf /w/minio-bin-20231120.txz -C /root
/root/bin/minio server /var/minio > /tmp/minio.log 2>&1 &
timeout 30 sh -c "until nc -z localhost 9000; do sleep 0.1; done;"
/root/bin/mc alias set srv1 http://localhost:9000 minioadmin minioadmin 2>&1 |tee /tmp/mc.log
/root/bin/mc mb srv1/buck1 --region s3test 2>&1|tee -a /tmp/mc.log
/root/bin/mc anonymous set public srv1/buck1 2>&1|tee -a /tmp/mc.log
rm -f /tmp/mc.log /tmp/minio.log
