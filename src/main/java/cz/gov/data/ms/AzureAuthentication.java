package cz.gov.data.ms;

import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.azure.identity.UsernamePasswordCredentialBuilder;

import com.microsoft.graph.authentication.TokenCredentialAuthProvider;

import java.util.List;

public class AzureAuthentication {

    private TokenCredentialAuthProvider authProvider = null;

    // ID aplikace (klienta), application overview
    private final String applicationId;

    // ID adresáře (tenanta), application overview
    private final String tenantId;

    public AzureAuthentication(String applicationId, String tenantId) {
        this.applicationId = applicationId;
        this.tenantId = tenantId;
    }

    public TokenCredentialAuthProvider provider() {
        return authProvider;
    }

    public void authenticateAsUser(String username, String password, List<String> scopes) {
        // https://learn.microsoft.com/en-us/graph/sdks/choose-authentication-providers?tabs=java#usernamepassword-provider
        // Application must have "Povolit toky veřejných klientů" turn on.

        // In addition, user must agree with application rights.
        // https://learn.microsoft.com/en-us/graph/auth-v2-user?tabs=http

        this.authProvider = createUserTokenProvider(username, password, scopes);
    }

    /**
     * Return URL user must visit in web-browser and grant access to our application.
     */
    public String createLoginLink(List<String> scopes) {
        // https://learn.microsoft.com/en-us/graph/auth-v2-user?tabs=http
        String encodedSpace = "%20";
        return "https://login.microsoftonline.com/" + tenantId + "/oauth2/v2.0/authorize?" +
                "client_id=" + applicationId +
                // We do not need the response code or the response, so we redirect
                // back to localhost.
                "&response_type=code&response_mode=query" +
                "&redirect_uri=http%3A%2F%2Flocalhost%2Fmyapp%2F" +
                // Allow offline access for the application.
                "$scope=offline_access" + encodedSpace +
                // Request required scopes.
                String.join(encodedSpace, scopes);
    }

    protected TokenCredentialAuthProvider createUserTokenProvider(String username, String password, List<String> scopes) {
        var credential = new UsernamePasswordCredentialBuilder()
                .clientId(applicationId)
                .tenantId(tenantId)
                .username(username)
                .password(password)
                .build();

        if (credential == null) {
            throw new RuntimeException("Credentials are null!");
        }

        return new TokenCredentialAuthProvider(scopes, credential);
    }

    public void authenticateAsApplication(String secret) {
        // Certificates and Secrets
        // Secret codes
        // Value - visible only after creation
        this.authProvider = createApplicationTokenProvider(secret);
    }

    protected TokenCredentialAuthProvider createApplicationTokenProvider(String secret) {
        // https://learn.microsoft.com/en-us/graph/sdks/choose-authentication-providers?tabs=java#using-a-clients-secret
        ClientSecretCredential credential = new ClientSecretCredentialBuilder()
                .clientId(applicationId)
                .tenantId(tenantId)
                .clientSecret(secret)
                .build();

        if (credential == null) {
            throw new RuntimeException("Credentials are null!");
        }

        return new TokenCredentialAuthProvider(
                // The /.default scope infers the permissions from the according application.
                List.of("https://graph.microsoft.com/.default"),
                credential);
    }

}
