/*
 * The MIT License (MIT) Copyright (c) 2020-2023 artipie.com
 * https://github.com/artipie/http/blob/master/LICENSE.txt
 */
package custom.policy.file;

import com.artipie.asto.factory.Config;
import com.artipie.security.policy.ArtipiePolicyFactory;
import com.artipie.security.policy.PoliciesLoaderTest;
import com.artipie.security.policy.Policy;
import com.artipie.security.policy.PolicyFactory;
import java.security.Permissions;

/**
 * Test policy.
 * @since 1.2
 */
@ArtipiePolicyFactory("file-policy")
public final class FilePolicyFactory implements PolicyFactory {
    @Override
    public Policy<Permissions> getPolicy(final Config config) {
        return new PoliciesLoaderTest.TestPolicy();
    }
}
