set -x
set -e
# start artipie
docker run --name artipie -d -it -v $(pwd)/artipie.yaml:/etc/artipie.yml -v $(pwd):/var/artipie -p 8080:80 artipie/artipie:latest
# wait for container to be ready for the new connections
sleep 5
# create a file for subsequent publication
echo "hello world" > text.txt
# publish text.txt
curl --silent -X PUT --data-binary "@text.txt" http://localhost:8080/repo/text.txt
# download the file
STATUSCODE=$(curl --silent --output /dev/stderr --write-out "%{http_code}" -X GET http://localhost:8080/repo/text.txt)
# remove container
docker rm -f artipie
# make sure status code is 200
if test $STATUSCODE -ne 200; then
  exit 1
fi
