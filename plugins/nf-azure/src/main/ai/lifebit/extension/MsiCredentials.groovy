package ai.lifebit.extension

import com.azure.core.credential.AccessToken
import com.azure.core.credential.TokenRequestContext
import com.azure.identity.ManagedIdentityCredential
import com.microsoft.azure.batch.auth.BatchCredentials
import com.microsoft.rest.credentials.TokenCredentials
import groovy.transform.CompileStatic
import okhttp3.Request

@CompileStatic
class MsiCredentials extends TokenCredentials implements BatchCredentials {

    private String baseUrl

    private ManagedIdentityCredential credential

    /** The Batch service auth endpoint */
    private String serviceEndpoint;

    private AccessToken accessToken;

    /**
     * Initializes a new instance of the TokenCredentials.
     *
     * @param scheme scheme to use. If null, defaults to Bearer
     * @param token valid token
     */
    MsiCredentials(ManagedIdentityCredential credential, String serviceEndpoint, String baseUrl) {
        super(null, null)
        if (baseUrl == null) {
            throw new IllegalArgumentException("Parameter baseUrl is required and cannot be null.");
        }
        if (credential == null) {
            throw new IllegalArgumentException("Parameter credential is required and cannot be null.");
        }
        if (serviceEndpoint == null) {
            throw new IllegalArgumentException("Parameter serviceEndpoint is required and cannot be null.");
        }
        this.baseUrl = baseUrl
        this.credential = credential
        this.serviceEndpoint = serviceEndpoint
    }

    @Override
    String baseUrl() {
        return this.baseUrl
    }

    @Override
    public String getToken(Request request) throws IOException {
        if (this.accessToken == null || this.accessToken.expired) {
            def tokenRequest = new TokenRequestContext()
            tokenRequest.addScopes("${this.serviceEndpoint}/.default")
            this.accessToken = this.credential.getTokenSync(tokenRequest);
            println("Token validity is expiring on ${this.accessToken.expiresAt}")
        }
        return accessToken.token;
    }



}
