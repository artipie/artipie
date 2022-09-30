# Artipie management Rest API

Artipie provides Rest API to manage [repositories](./Configuration-Repository), [users](./Configuration-Credentials) 
and [storages aliases](./Configuration-Storage#Storage-Aliases). API is self-documented with [Swagger](https://swagger.io/)
interface, Swagger documentation pages are available on URLs `http://{host}:{api}/api/index-{layout}.html` where
`{layout}` is the layout you run Artipie with, `org` or `flat`.

In Swagger documentation have three definitions - Repositories, Users and Auth Token. You can switch
between the definitions with the help of "Select a definition" listbox.

<img src="https://user-images.githubusercontent.com/14931449/193015387-3e25f937-7f23-4b27-884c-f183ca9dc8a0.png" alt="Swagger documentation" width="400"/>

All Rest API endpoints require JWT authentication token to be passed in `Authentification` header. 
The token can be issued with the help of `POST /api/v1/oauth/token` request on the "Auth Token" 
definition page in Swagger. Once token is received, copy it, open another definition, press 
"Authorize" button and paste the token. Swagger will add the token to any request you perform.

## Manage repository API

Rest API allows to manage repository settings: read, create, update and remove operations are supported. 
Note, that jsons, accepted by Rest endpoints, are equivalents of the YAML repository settings. Which means, 
that API accepts all the repository specific settings fields which are applicable to the repository. 
Choose repository you are interested in from [this table](./Configuration-Repository#Supported-repository-types) 
to learn all the details. 

Rest API provides method to rename repository `PUT /api/v1/{repo_name}/move` (`{repo_name}` is the 
name of the repository, includes username in the case of `org` layout) and move all the data
from repository with the `{repo_name}` to repository with new name (new name is provided in json 
request body, check Swagger docs to learn the format). Response is returned immediately, but data 
manipulation is performed in asynchronous mode, so to make sure data transfer is complete, 
call `HEAD /api/v1/{repo_name}` and verify status `404 NOT FOUND` is returned.

## Storage aliases
[Storage aliases](./Configuration-Storage#Storage-Aliases) can also be managed with Rest API, 
there are methods to read, create, update and remove aliases. Note, that concrete storage settings 
depends on storage type, Rest API accepts all the parameters in json format equivalent to the 
YAML storages setting. 

## Users management API

Use Rest API to obtain list of the users, check user info, add, update or remove user. Also, it's
possible to change password by calling `POST /api/v1/{username}/alter/password` method providing
old and new password in json request body.