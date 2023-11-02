package keycloak;

import java.util.Collections;
import java.util.Objects;
import javax.ws.rs.core.Response;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

/**
 * Keycloak docker initializer.
 * Initializes docker image: quay.io/keycloak/keycloak:20.0.1
 * As follows:
 * 1. Creates new realm
 * 2. Creates new role
 * 3. Creates new client application
 * 4. Creates new client's application role.
 * 5. Creates new user with realm role and client application role.
 */
public class KeycloakDockerInitializer {
    /**
     * Keycloak url.
     */
    private final static String KEYCLOAK_URL = "http://localhost:8080";

    /**
     * Keycloak admin login.
     */
    private final static String KEYCLOAK_ADMIN_LOGIN = "admin";

    /**
     * Keycloak admin password.
     */
    private final static String KEYCLOAK_ADMIN_PASSWORD = KEYCLOAK_ADMIN_LOGIN;

    /**
     * Realm name.
     */
    private final static String REALM = "test_realm";

    /**
     * Realm role name.
     */
    private final static String REALM_ROLE = "role_realm";

    /**
     * Client role.
     */
    private final static String CLIENT_ROLE = "client_role";

    /**
     * Client application id.
     */
    private final static String CLIENT_ID = "test_client";

    /**
     * Client application password.
     */
    private final static String CLIENT_PASSWORD = "secret";

    /**
     * Test user id.
     */
    private final static String USER_ID = "user1";

    /**
     * Test user password.
     */
    private final static String USER_PASSWORD = "password";

    /**
     * Keycloak server url.
     */
    private final String url;

    /**
     * Start point of application.
     * @param args Arguments, can contains keycloak server url
     */
    public static void main(String[] args) {
        final String url;
        if (!Objects.isNull(args) && args.length > 0) {
            url = args[0];
        } else {
            url = KEYCLOAK_URL;
        }
        new KeycloakDockerInitializer(url).init();
    }

    public KeycloakDockerInitializer(final String url) {
        this.url = url;
    }

    /**
     * Using admin connection to keycloak server initializes keycloak instance.
     */
    public void init() {
        Keycloak keycloak = Keycloak.getInstance(
            url,
            "master",
            KEYCLOAK_ADMIN_LOGIN,
            KEYCLOAK_ADMIN_PASSWORD,
            "admin-cli");
        createRealm(keycloak);
        createRealmRole(keycloak);
        createClient(keycloak);
        createClientRole(keycloak);
        createUserNew(keycloak);
    }

    /**
     * Creates new realm 'test_realm'.
     * @param keycloak Keycloak instance.
     */
    private void createRealm(final Keycloak keycloak) {
        RealmRepresentation realm = new RealmRepresentation();
        realm.setRealm(REALM);
        realm.setEnabled(true);
        keycloak.realms().create(realm);
    }

    /**
     * Creates new role 'role_realm' in realm 'test_realm'
     * @param keycloak Keycloak instance.
     */
    private void createRealmRole(final Keycloak keycloak) {
        keycloak.realm(REALM).roles().create(new RoleRepresentation(REALM_ROLE, null, false));
    }

    /**
     * Creates new client application with ID 'test_client' and password 'secret'.
     * @param keycloak Keycloak instance.
     */
    private void createClient(final Keycloak keycloak) {
        ClientRepresentation client = new ClientRepresentation();
        client.setEnabled(true);
        client.setPublicClient(false);
        client.setDirectAccessGrantsEnabled(true);
        client.setStandardFlowEnabled(false);
        client.setClientId(CLIENT_ID);
        client.setProtocol("openid-connect");
        client.setSecret(CLIENT_PASSWORD);
        client.setAuthorizationServicesEnabled(true);
        client.setServiceAccountsEnabled(true);
        keycloak.realm(REALM).clients().create(client);
    }

    /**
     * Creates new client's application role 'client_role' for client application.
     * @param keycloak Keycloak instance.
     */
    private void createClientRole(final Keycloak keycloak) {
        RoleRepresentation clientRoleRepresentation = new RoleRepresentation();
        clientRoleRepresentation.setName(CLIENT_ROLE);
        clientRoleRepresentation.setClientRole(true);
        keycloak.realm(REALM)
            .clients()
            .findByClientId(CLIENT_ID)
            .forEach(clientRepresentation ->
                keycloak.realm(REALM)
                    .clients()
                    .get(clientRepresentation.getId())
                    .roles()
                    .create(clientRoleRepresentation)
            );
    }

    /**
     * Creates new user with realm role and client application role.
     * @param keycloak
     */
    private void createUserNew(final Keycloak keycloak) {
        // Define user
        UserRepresentation user = new UserRepresentation();
        user.setEnabled(true);
        user.setUsername(USER_ID);
        user.setFirstName("First");
        user.setLastName("Last");
        user.setEmail(USER_ID + "@localhost");

        // Get realm
        RealmResource realmResource = keycloak.realm(REALM);
        UsersResource usersRessource = realmResource.users();

        // Create user (requires manage-users role)
        Response response = usersRessource.create(user);
        String userId = response.getLocation().getPath().substring(response.getLocation().getPath().lastIndexOf('/') + 1);

        // Define password credential
        CredentialRepresentation passwordCred = new CredentialRepresentation();
        passwordCred.setTemporary(false);
        passwordCred.setType(CredentialRepresentation.PASSWORD);
        passwordCred.setValue(USER_PASSWORD);

        UserResource userResource = usersRessource.get(userId);

        // Set password credential
        userResource.resetPassword(passwordCred);

        // Get realm role "tester" (requires view-realm role)
        RoleRepresentation testerRealmRole = realmResource
            .roles()
            .get(REALM_ROLE)
            .toRepresentation();

        // Assign realm role tester to user
        userResource.roles().realmLevel().add(Collections.singletonList(testerRealmRole));

        // Get client
        ClientRepresentation appClient = realmResource
            .clients()
            .findByClientId(CLIENT_ID)
            .get(0);

        // Get client level role (requires view-clients role)
        RoleRepresentation userClientRole = realmResource
            .clients()
            .get(appClient.getId())
            .roles()
            .get(CLIENT_ROLE)
            .toRepresentation();

        // Assign client level role to user
        userResource
            .roles()
            .clientLevel(appClient.getId())
            .add(Collections.singletonList(userClientRole));
    }
}
