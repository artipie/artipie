# Artipie dashboard

The Artipie provides [front-end](https://github.com/artipie/front) web application to manage repositories using a web-browser.<br/>
The Artipie front-end provides a convenient dashboard with UI pages for managing repositories.<br/>
Artipie front-end is distributed as [Docker image](https://hub.docker.com/r/artipie/front) and as fat jar.
The jar file can be downloaded on GitHub [releases page](https://github.com/artipie/front/releases).

The Artipie front-end is independent part of Artipie project that interacts with Artipie server by using [REST-API services](./Rest-api) provided by Artipie server.

The Artipie dashboard provides following functionality:
* Sign in
* View of all available repositories
* Creating a new repository
* Edit a repository
* Remove a repository
* Sign out

To get more information about start up the Artipie front-end in Docker container please visit article ["Quickstart with docker-compose"](./DockerCompose) 
 
## Sign in

The `Sign in` page allows the Artipie users to login to Artipie dashboard.<br/> 
Page has two fields, `User`, and the `Password`.<br/>
The inputs will be verified by Artipie-server. 
Once the verification complete, the user will proceed to the list of repositories.

[[/images/dashboard/signin.jpg|Sign in]]

## Repository list
The `Repository list` page allows the Artipie user to view all available repositories.
The page provides short information for each repository such as:
* Repository name
* Repository type
* Repository port where Artipie-server serves http requests to repository from according tools

[[/images/dashboard/repository_list.jpg|Repository list]]

The Artipie user can filter repositories by a repository name:
[[/images/dashboard/repository_list_filtered.jpg|Filtered repository list]]

## Create repository
To create a new repository please click `Repositories->Create` link on the left side of page.

The `Create repository` page allows the Artipie user:
* to chose `type` of repository
* to provide `name` of repository
* to provide `configuration` of repository

The page provides default repository configuration that can be edited by the Artipie user.<br/>
Additionally page provides information how a new repository can be used by tools according to repository type.<br/>

To save a new repository configuration please click `Add repository` button.

Follow [this link](./Configuration-Repository) to know all the technical details about supported repositories and settings.

[[/images/dashboard/repository_create.jpg|Create repository]]

## Edit repository
To edit existing repository please click on `repository name` on the `Repository list` page.

The `Edit repository` page allows the Artipie user to change `configuration` of repository.<br/>
Page provides information how the existing repository can be used by tools according to repository type.<br/>

To save the repository configuration please click `Update` button.

Follow [this link](./Configuration-Repository) to know all the technical details about supported repositories and settings.

[[/images/dashboard/repository_edit.jpg|Edit repository]]

## Remove a repository
To remove existing repository please click `Remove` button on `Edit repository` page.

## Sign out
To log off dashboard please click `Sign out` link on the left side of page.<br/>
The current session will be broken and the user will be redirected to the `Sign in` page.
