FROM centeredge/nuget:5
COPY ./config.xml /test/NuGet.Config
COPY ./run.sh /test/run.sh
COPY ./newtonsoft.json.12.0.3.nupkg /test/7faad2d4-c3c2-4c23-a816-a09d1b1f89e8
WORKDIR /test
CMD "/test/run.sh"
