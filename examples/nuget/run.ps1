# Trace executed commands.
Set-PSDebug -Trace 1

# Start Artipie.
docker run --rm -d --name artipie -it -v ${PWD}/artipie.yaml:/etc/artipie.yml -v  ${PWD}:/var/artipie -p 8080:80 al

# Wait for container to be ready for new connections.
Start-Sleep 5

# Enter dir with a package for deployment.
Set-Location sample-for-deployment

# Clear cache.
Remove-Item -Force SampleForDeployment.1.0.0.nupkg
nuget locals all -list

# Create a NuGet package.
nuget pack

# Push the package to Artipie.
nuget push SampleForDeployment.1.0.0.nupkg -SkipDuplicate -src http://localhost:8080/my-nuget/index.json

# Gettings out of the deployed package dir
Set-Location ..

# Remove container.
docker stop artipie