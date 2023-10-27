#!/bin/bash

nuget push ./7faad2d4-c3c2-4c23-a816-a09d1b1f89e8 -ConfigFile ./NuGet.Config -Verbosity detailed -Source artipie-nuget-test

nuget install Newtonsoft.Json -Version 12.0.3 -NoCache -ConfigFile ./NuGet.Config -Verbosity detailed -Source artipie-nuget-test