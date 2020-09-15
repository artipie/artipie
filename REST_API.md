# Artipie REST API

One objective of Artipie is to provide APIs that are compatible with Artifactory APIs so that customers can have an easy and smooth transition from Artifactory to Artipie if they choose to.  The set of APIs are to manage repositories, users, repository permissions and more.

## Create Repository

Creates new docker repository with `default` storage alias and provided name.

> **PUT** /api/repositories/{repoKey}

Consumes json with the following fields (any other fields are ignored): 

Field name | Type | Meaning | Required
------------ | ------------- | ------------ | ---------
key | string | New repository name | Y
rclass | string | Repository class, only `local` type is supported | Y
packageType | string | Artifact type, only `docker` type is supported | Y
dockerApiVersion | string | Docker API version, we only support `V2` | Y

Possible responses:
- `200 OK` when new repository was successfully created
- `400 BAD REQUEST` in the cases when repository with such name already exists or invalid json was sent
- `500 INTERNAL ERROR` in the case of unexpected server error

Artifactory documentation can be found [here](https://www.jfrog.com/confluence/display/rtf/artifactory+rest+api#ArtifactoryRESTAPI-CreateRepository). 

## Users

### Get Users

Endpoint to obtain [list](https://www.jfrog.com/confluence/display/rtf/artifactory+rest+api#ArtifactoryRESTAPI-GetUsers) of the existing users.

> **GET** /api/security/users

Returns json array of the following form:
```json
[
  {
    "name": "davids",
    "uri" : "http://localhost:8081/artifactory/api/security/users/davids",
    "realm" : "Internal"
  }, {
    "name": "danl",
    "uri" : "http://localhost:8081/artifactory/api/security/users/danl",
    "realm" : "Internal"
  }
]
```
Where `name` field contains user name, `uri` - URI to obtain user details, user `realm` is always `Internal`.

### Get User Details

Endpoint to obtain [user details](https://www.jfrog.com/confluence/display/rtf/artifactory+rest+api#ArtifactoryRESTAPI-GetUserDetails).

> **GET** /api/security/users/{userName}

Returns json with the following fields:

Field name | Type | Meaning | Required
------ | ------ | ------ | ------
name | string | User name | Y
email | string | User email | Y
lastLoggedIn | string | Default `2020-01-01T01:01:01.000+01:00` | Y
realm | string | User realm, value `Internal` is always returned | Y

If user is not found `404 NOT FOUND` status is returned.

### Create Or Update User

[Creates, replaces](https://www.jfrog.com/confluence/display/rtf/artifactory+rest+api#ArtifactoryRESTAPI-CreateorReplaceUser) or [updates](https://www.jfrog.com/confluence/display/rtf/artifactory+rest+api#ArtifactoryRESTAPI-UpdateUser) user with `{userName}` from request URL.

> **PUT**/**POST** /api/security/users/{userName}

Consumes json with the following fields (any other fields are ignored): 

Field name | Type | Meaning | Required
------ | ------ | ------ | ------
password | string | User password | Y
email | string | User email | Y

Possible responses:
- `200 OK` when user was successfully created or updated
- `400 BAD REQUEST` when invalid json was sent
- `500 INTERNAL ERROR` in the case of unexpected server error

### Delete User

[Removes](https://www.jfrog.com/confluence/display/rtf/artifactory+rest+api#ArtifactoryRESTAPI-DeleteUser) an Artipie user. 

> **DELETE** /api/security/users/{userName}

Possible responses:
- `200 OK User '{userName}' has been removed successfully.` when user with `{userName}` was successfully removed
- `404 NOT FOUND` when user was not found
- `500 INTERNAL ERROR` in the case of unexpected server error

## Permission Targets

### Get Permission Targets

Endpoint to obtain the permission targets [list](https://www.jfrog.com/confluence/display/rtf/artifactory+rest+api#ArtifactoryRESTAPI-GetPermissionTargets). 
From the Artipie point of view permission target is a repository, so basically this endpoint returns list of the 
existing repositories. 

> **GET**  /api/security/permissions

Returns json array of the following form:
```json
[
  {
    "name": "readSourceArtifacts",
    "uri" : "http://localhost:8081/artifactory/api/security/permissions/readSourceArtifacts"
  }, {
    "name": "populateCaches",
    "uri" : "http://localhost:8081/artifactory/api/security/permissions/populateCaches"
  }
]
```
Where `name` is a permission target name and `uri` - URI to obtain permission target details.

### Get Permission Target Details

Endpoint to get the [details](https://www.jfrog.com/confluence/display/rtf/artifactory+rest+api#ArtifactoryRESTAPI-GetPermissionTargetDetails) 
of a Permission Target, or, if other words, Artipie repository permissions details.

> **GET** /api/security/permissions/{permissionTargetName}

Returns json of the following format:

```json
{
  "repositories": ["{permissionTargetName}"],
  "principals": {
    "users" : {
      "bob": ["r", "w", "m"],
      "alice" : ["d", "w", "r"]
    }   
  }
}
```

Fields description:

Field name | Type | Meaning | Required
------ | ------ | ------ | ------
repositories | json array | Repository name, always one-element array with the `{permissionTargetName}` item | Y
principals | json object | Repository permissions details, contains `users` element with user permission details | Y

Here is the set of supported permissions along with shortening convention 
(`delete` is supported for file storage only):

```text
w=deploy; m=admin; d=delete; r=read
```

If requested `{permissionTargetName}` does not exist, `404 NOT FOUND` status is returned.

### Create or Replace Permission Target 

[Creates](https://www.jfrog.com/confluence/display/rtf/artifactory+rest+api#ArtifactoryRESTAPI-CreateorReplacePermissionTarget) or updates repository permissions.

> **PUT** /api/security/permissions/{permissionTargetName}

Consumes json of the following form: 

```json
{
   "repo": {
     "actions": {
       "users" : {
          "bob": ["read", "write", "manage"],
          "alice" : ["write", "read"]
       }
     }
   }
}
```
where field `users` contains list of the user names and corresponding permissions, all other fields 
are ignored. 

The following synonyms and shortened values for standard operations are supported:
- `read` - `r` 
- `write` - `w`, `deploy`
- `delete` - `d`
- `manage` (any operation is allowed) - `m`, `admin`

Possible responses:
- `200 OK` when permissions were added successfully
- `500 INTERNAL ERROR` in the case of unexpected server error

### Delete Permission Target 

[Deletes](https://www.jfrog.com/confluence/display/JFROG/Artifactory+REST+API#ArtifactoryRESTAPI-DeletePermissionTarget) 
permission target, that is removes all permissions for repository leaving repository inaccessible.

> **DELETE** /api/security/permissions/{permissionTargetName}

Possible responses:
- `200 OK Permission Target '{permissionTargetName}' has been removed successfully.` when user with `{permissionTargetName}` was successfully removed
- `404 NOT FOUND` when repository with `{permissionTargetName}` was not found
- `500 INTERNAL ERROR` in the case of unexpected server error

## File List

[Get](https://www.jfrog.com/confluence/display/JFROG/Artifactory+REST+API#ArtifactoryRESTAPI-FileList) a flat listing of the items within a repository.

> **GET** /api/storage/{repoKey}

Returns json array of the following format:

```json
[
  {
    "uri": "/doc.txt",
    "folder": "false"
  },
  {
    "uri": "/one",
    "folder": "true"
  }
]
```
 where `uri` is a storage item name and `folder` flag indicates whether item is a folder or not.  
