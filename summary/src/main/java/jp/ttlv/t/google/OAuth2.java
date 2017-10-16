package jp.ttlv.t.google;

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.calendar.CalendarScopes;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class OAuth2 {

    private static final String CLIENT_ID = "99999999999-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx.apps.googleusercontent.com";
    private static final String CLIENT_SECRET = "XXXXXXXXXXXXXX_XXXXXXXXX";
    private static final String REDIRECT_URL = "urn:ietf:wg:oauth:2.0:oob";

    private final JsonFactory json_factory = JacksonFactory.getDefaultInstance();
    private HttpTransport http_transport = null;

    public OAuth2() {
        try {
            http_transport = GoogleNetHttpTransport.newTrustedTransport();
        } catch (GeneralSecurityException | IOException ex) {
            Logger.getLogger(OAuth2.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public String getGoogleOAuthURL() {
        GoogleAuthorizationCodeFlow flow;
        try {
            flow = get_flow();
        } catch (IOException | GeneralSecurityException ex) {
            Logger.getLogger(OAuth2.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
        return flow.newAuthorizationUrl()
                .setRedirectUri(REDIRECT_URL)
                .build();
    }

    public GoogleTokenResponse getGoogleResponse(String code) {
        try {
            GoogleAuthorizationCodeFlow flow = get_flow();
            return flow.newTokenRequest(code).setRedirectUri(REDIRECT_URL).execute();
        } catch (IOException | GeneralSecurityException ex) {
            Logger.getLogger(OAuth2.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    public GoogleCredential getGoogleCredential(String refresh_token) throws IOException {
        GoogleCredential credential = new GoogleCredential.Builder()
                .setClientSecrets(CLIENT_ID, CLIENT_SECRET)
                .setJsonFactory(json_factory).setTransport(http_transport)
                .build();
        credential.setRefreshToken(refresh_token);
        credential.refreshToken();
        return credential;
    }

    private GoogleAuthorizationCodeFlow get_flow() throws IOException, GeneralSecurityException {
        Set<String> scopes = new HashSet<>();
        scopes.add(CalendarScopes.CALENDAR);
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                http_transport, json_factory, CLIENT_ID, CLIENT_SECRET, scopes)
                .setAccessType("offline")
                .setApprovalPrompt("auto").build();

        return flow;
    }
}
