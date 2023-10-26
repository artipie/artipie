Below is information for adapter testing.
conanfile.txt - test Conanfile for checking `conan install` command.
conan_server-data.tar.gz - test data for server-side (artipie-conan or conan_server)

Uploading specific list of packages:

cat conanfile.txt|grep ".*/.*"|cut -d/ -f1|https_proxy= http_proxy= xargs -n1 conan upload -r conan-local --confirm --all
ls ~/.conan/data|http_proxy= https_proxy= xargs -n1 conan upload -r conan-local --confirm --all

At the moment, following commands were successfully tested with conan v1.37.2 and conan-artipie adapter (with empty cache).

conan remote add conan-local http://localhost:9300 false
conan get zlib/1.2.11 -r conan-local
CONAN_LOGGING_LEVEL=debug conan download -r conan-local zlib/1.2.11@

mkdir build && cd build
rm -rf ./* && rm -rf ~/.conan/data && conan install -r conan-local ..

Testing package uploading, with logging:

export CONAN_TRACE_FILE=/tmp/conan_trace.log
cd conan-center-index/recipes/zmqpp/all
git checkout df1fb263e85c1543055231310ecb8c4d5bc4957b #in master, when tested for conan v1.50.0
conan create . 4.2.0@
conan upload zmqpp/4.2.0@ -r conan-local --all

