# Keycloak docker initializer

This directory 'artipie/src/test/resources/auth/keycloak-docker-initializer' contains source code of keycloak.KeycloakDockerInitializer.java class
and all required dependencies for its compilation in directory 'lib'.

Project can be opened in any IDE by manually adding dependencies from 'lib' to project.

All dependencies are created by command:

``mvn dependency:copy-dependencies -DoutputDirectory=./lib``

The according pom.xml dependency definition is:
``
<dependency>
<groupId>org.keycloak</groupId>
<artifactId>keycloak-admin-client</artifactId>
<version>20.0.1</version>
</dependency>
``

The reason why keycloak.KeycloakDockerInitializer.java is defined in resources is
because ``keycloak-admin-client`` artifact has a clash of dependencies with Artipie dependency 'com.jcabi:jcabi-github:1.3.2'. 

The usage of keycloak.KeycloakDockerInitializer.java is in test 'AuthFromKeycloakTest' that dynamically compiles 'keycloak.KeycloakDockerInitializer' class 
and starts it to fill following on keycloak server:
1. Creates new realm
2. Creates new role
3. Creates new client application
4. Creates new client's application role.
5. Creates new user with realm role and client application role.
