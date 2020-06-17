#!/bin/bash

[[ -z "$1" ]] && usage

base="$1/artipie"

function die {
  echo "$1"
  rm -fr "${base}"
  exit 1
}

function usage {
  echo -ne "Usage: setup.sh <basedir>\n\t- basedir - the root of Artipie configuration\n"
  exit 0
}

if [[ -d "${base}" ]]; then
  read -p "Directory \`${base}' already exist. Remove to continue? [y/n] " -n 1 -r
  echo
  if [[ ! $REPLY =~ ^[Yy]$ ]]; then
      exit 1
  fi
  rm -vfr "${base}"
fi


mkdir -pv "${base}/repo" || die "Failed to create repo layout"
mkdir -pv "${base}/data" || die "Failed to create data directory"

cp -v "example/artipie.yaml" -t "${base}" || die "Failed to copy artipie config"
cp -v "example/_credentials.yaml" -t "${base}/repo" || die "Failed to copy credentials"
cp -v "example/_permissions.yaml" -t "${base}/repo" || die "Failed to copy permissions"
cp -v "example/_storages.yaml" -t "${base}/repo" || die "Failed to copy storage aliases"

sed -i "s/__storage__/${base//\//\\/}\/repo/g" "${base}/artipie.yaml" || die "Failed to configure the server"
sed -i "s/__location__/${base//\//\\/}\/data/g" "${base}/repo/_storages.yaml" || die "Failed to configure storages"

echo ""
echo ""
echo "Done: Artipie example configuration created at ${base}"
echo "To start with Docker:"
echo "  run commands:"
echo "    docker pull artipie/artipie:latest"
echo "    docker run --name=artipie-example --rm \\"
echo "      -v ${base}/artipie.yaml:/etc/artipie.yml \\"
echo "      -v ${base}:${base} -p 80:80 artipie/artipie:latest"
echo "To start with Java (requires JRE >= 11):"
echo "  build artipie using \`mvn clean package -Passembly\`"
echo "  run assembly-jar with \`java -jar target/artipie-jar-with-dependencies.jar --config=${base}/artipie.yaml --port=80\`"
