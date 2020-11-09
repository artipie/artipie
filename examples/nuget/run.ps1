# Trace executed commands.
Set-PSDebug -Trace 1

# Start Artipie.
docker run --rm -d --name artipie -it -v ${PWD}/artipie.yaml:/etc/artipie/artipie.yml -v  ${PWD}:/var/artipie -p 8080:80 artipie/artipie:latest

# Wait for container to be ready for new connections.
Start-Sleep 5

# Enter dir with a package for deployment.
Set-Location sample-for-deployment

# Clear cache.
Remove-Item SampleForDeployment.1.0.0.nupkg -ErrorAction Ignore
nuget locals all -list

# Create a NuGet package.
nuget pack

# Push the package to Artipie.
nuget push SampleForDeployment.1.0.0.nupkg -SkipDuplicate -src http://localhost:8080/my-nuget/index.json

# Getting to the consumer dir.
Set-Location ../sample-consumer

# Install just published package from Artipie.
nuget install SampleForDeployment -Version 1.0.0 -Source http://localhost:8080/my-nuget/index.json

# Remove container.
docker stop artipie
