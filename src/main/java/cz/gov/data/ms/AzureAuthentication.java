package cz.gov.data.ms;

import com.azure.identity.ClientSecretCredentialBuilder;
import com.azure.identity.UsernamePasswordCredentialBuilder;
import com.microsoft.graph.serviceclient.GraphServiceClient;

import java.util.List;

public class AzureAuthentication {

    private GraphServiceClient graphClient = null;

    /**
     * ID aplikace (klienta), application overview
     */
    private final String applicationId;

    /**
     * ID adresáře (tenanta), application overview
     */
    private final String tenantId;

    public AzureAuthentication(String applicationId, String tenantId) {
        this.applicationId = applicationId;
        this.tenantId = tenantId;
    }

    public GraphServiceClient graphClient() {
        return graphClient;
    }

    public void authenticateAsUser(String username, String password, String[] scopes) {
        // https://learn.microsoft.com/en-us/graph/sdks/choose-authentication-providers?tabs=java#usernamepassword-provider
        // Application must have "Povolit toky veřejných klientů" turn on.

        // In addition, user must agree with application rights.
        // https://learn.microsoft.com/en-us/graph/auth-v2-user?tabs=http

        this.graphClient = createUserTokenProvider(username, password, scopes);
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

    // https://learn.microsoft.com/en-us/graph/sdks/choose-authentication-providers?tabs=java#usernamepassword-provider
    protected GraphServiceClient createUserTokenProvider(String username, String password, String[] scopes) {
        var credential = new UsernamePasswordCredentialBuilder()
                .clientId(applicationId)
                .tenantId(tenantId)
                .username(username)
                .password(password)
                .build();

        if (credential == null) {
            throw new RuntimeException("Credentials are null!");
        }

        return new GraphServiceClient(credential, scopes);
    }

    public void authenticateAsApplication(String secret) {
        // Certificates and Secrets
        // Secret codes
        // Value - visible only after creation
        this.graphClient = createApplicationTokenProvider(secret);
    }

    // https://learn.microsoft.com/en-us/graph/sdks/choose-authentication-providers?tabs=java#using-a-client-secret-2
    protected GraphServiceClient createApplicationTokenProvider(String secret) {
        var credential = new ClientSecretCredentialBuilder()
                .clientId(applicationId)
                .tenantId(tenantId)
                .clientSecret(secret)
                .build();

        if (credential == null) {
            throw new RuntimeException("Credentials are null!");
        }

        String[] scopes = new String[] {"https://graph.microsoft.com/.default"};
        return new GraphServiceClient(credential, scopes);
    }

}
