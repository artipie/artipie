/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/http/blob/master/LICENSE.txt
 */
package custom.policy.duplicate;

import com.artipie.asto.factory.Config;
import com.artipie.security.policy.ArtipiePolicyFactory;
import com.artipie.security.policy.Policy;
import com.artipie.security.policy.PolicyFactory;
import java.security.PermissionCollection;

/**
 * Test policy.
 * @since 1.2
 */
@ArtipiePolicyFactory("db-policy")
public final class DuplicatedDbPolicyFactory implements PolicyFactory {
    @Override
    public Policy<?> getPolicy(final Config config) {
        return (Policy<PermissionCollection>) uname -> null;
    }
}
