package jp.ttlv.t.google;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Calender {

    private static final String APPLICATION_NAME = "java demo";
    private final JsonFactory json_factory = JacksonFactory.getDefaultInstance();
    private HttpTransport http_transport = null;

    public Calender() {
        try {
            http_transport = GoogleNetHttpTransport.newTrustedTransport();
        } catch (GeneralSecurityException | IOException ex) {
            Logger.getLogger(OAuth2.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public com.google.api.services.calendar.Calendar getCalendarClient(
            GoogleCredential credential) {
        return new com.google.api.services.calendar.Calendar.Builder(
                http_transport, json_factory, credential)
                .setApplicationName(APPLICATION_NAME).build();

    }

}
