package jp.ttlv.t;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.model.CalendarList;
import com.google.api.services.calendar.model.CalendarListEntry;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import jp.ttlv.t.felica.Polling;
import jp.ttlv.t.google.Calender;
import jp.ttlv.t.google.OAuth2;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.rosaloves.bitlyj.Bitly;
import com.rosaloves.bitlyj.Bitly.Provider;
import static com.rosaloves.bitlyj.Bitly.shorten;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

public class TimeCard {

    private static boolean flag = true;
    private static final String store_dir = System.getProperty("user.home") + File.separator + ".store";
    private static final String[] slack_add_command = new String[]{
        "/usr/bin/node", "/root/slack_say/slack_say.js", "(message)"
    };

    public static void main(String[] args) {
        while (flag) {
            try {
                do_service();
            } catch (Exception ex) {
                Logger.getLogger(TimeCard.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public static void do_service() throws Exception {
        Polling polling = new Polling();
        String IDm = polling.do_service();
        if (IDm == null || IDm.isEmpty()) {
            return;
        }

        led_on();
        System.out.println("IDm:" + IDm);
        try {
            Properties properties = get_store_file(IDm);
            if (properties == null) {
                properties = create_auth_file(IDm);
                if (properties == null) {
                    return;
                }
            }

            if (update_calender(properties, IDm) == false) {
                File file = new File(store_dir + File.separator + "token." + IDm + ".properties");
                file.delete();
            }

        } finally {
            led_off();
        }

    }

    private static boolean update_calender(Properties properties, String IDm) throws IOException {
        OAuth2 obj = new OAuth2();
        GoogleCredential credential = obj.getGoogleCredential(properties.getProperty("refresh_token"));
        if (credential == null) {
            return false;
        }

        properties.setProperty("access_token", credential.getAccessToken());
        properties.setProperty("refresh_token", credential.getRefreshToken());
        properties.store(new FileOutputStream(store_dir + File.separator + "token." + IDm + ".properties"), "");

        Calender cal = new Calender();
        com.google.api.services.calendar.Calendar client = cal.getCalendarClient(credential);
        if (client == null) {
            return false;
        }

        String title = "おしごと";
        Date now = new Date();
        Date endDate = new Date(now.getTime() + (30 * 1000));
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);

        String calendar_id = "";
        CalendarList feed = client.calendarList().list().execute();
        for (CalendarListEntry entry : feed.getItems()) {
            if (entry.isPrimary()) {
                calendar_id = entry.getId();
                break;
            }
        }

        Event cur_event = null;
        String pageToken = null;
        do {
            com.google.api.services.calendar.model.Events events = client.events()
                    .list("primary")
                    .setQ(title)
                    .setCalendarId(calendar_id)
                    .setTimeMin(new DateTime(today.getTime(), TimeZone.getTimeZone("UTC")))
                    .setTimeMax(new DateTime(endDate, TimeZone.getTimeZone("UTC")))
                    .setPageToken(pageToken)
                    .setOrderBy("updated")
                    .execute();
            List<Event> items = events.getItems();
            for (Event event : items) {
                cur_event = event;
                System.out.println(event.getStart());
            }
            pageToken = events.getNextPageToken();
        } while (pageToken != null);

        if (cur_event == null) {
            Event event = new Event();
            event.setSummary(title);
            event.setVisibility("private");
            event.setTransparency("transparent");
            event.setPrivateCopy(true);
            event.setStart(new EventDateTime().setDateTime(new DateTime(now, TimeZone.getTimeZone("UTC"))));
            event.setEnd(new EventDateTime().setDateTime(new DateTime(endDate, TimeZone.getTimeZone("UTC"))));
            client.events().insert(calendar_id, event).execute();
            slack_add_exec("出勤しました。");
        } else {
            cur_event.setEnd(new EventDateTime().setDateTime(new DateTime(now, TimeZone.getTimeZone("UTC"))));
            cur_event.setVisibility("private");
            cur_event.setTransparency("transparent");
            client.events().update(calendar_id, cur_event.getId(), cur_event).execute();
            slack_add_exec("退勤しました。(たぶん)");

        }
        return true;
    }

    private static Properties create_auth_file(String IDm) {
        try {
            OAuth2 obj = new OAuth2();
            String url = obj.getGoogleOAuthURL();

            Provider bitly = Bitly.as("username", "_secret_");
            System.out.println("Please open the following URL in your browser.\n");
            System.out.println(bitly.call(shorten(url)).getShortUrl());
            System.out.print("\nautorization code? : ");
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            GoogleTokenResponse response = obj.getGoogleResponse(br.readLine());
            if (response == null) {
                return null;
            }

            Properties properties = new Properties();
            properties.setProperty("access_token", response.getAccessToken());
            properties.setProperty("refresh_token", response.getRefreshToken());
            properties.store(new FileOutputStream(store_dir + File.separator + "token." + IDm + ".properties"), "");
            return properties;

        } catch (IOException ex) {
            Logger.getLogger(TimeCard.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    private static Properties get_store_file(String IDm) throws FileNotFoundException, IOException {
        Properties rtn = new Properties();

        File dir = new File(store_dir);
        if (!dir.exists()) {
            dir.mkdir();
            return null;
        }

        File file = new File(store_dir + File.separator + "token." + IDm + ".properties");
        if (!file.exists()) {
            return null;
        }

        try (InputStream inputStream = new FileInputStream(file)) {
            rtn.load(inputStream);
        }

        return rtn;
    }

    private static void led_on() {
        led_control("led5", "green", 1, "none", -1);
        led_control("led4", "green", 1, "none", 500);
        led_control("led3", "green", 1, "none", 500);
        led_control("led2", "green", 1, "none", 500);
        led_control("led1", "green", 1, "none", 500);
        led_control("led0", "green", 1, "none", 500);
        led_exec();
    }

    private static void led_off() {
        led_control("led5", "green", 1, "none", -1);
        led_control("led4", "green", 0, "none", -1);
        led_control("led3", "green", 0, "none", -1);
        led_control("led2", "green", 0, "none", -1);
        led_control("led1", "green", 0, "none", -1);
        led_control("led0", "green", 0, "none", -1);
        led_exec();
    }

    private static void led_exec() {
        Runtime runtime = Runtime.getRuntime();
        try {
            Process process = runtime.exec(new String[]{
                "/usr/sbin/spitool",
                "-d",
                "/dev/fpga",
                "0x80",
                "0x02"
            });
            int rtn = process.waitFor();
            System.out.println("rtn:" + rtn);

        } catch (IOException | InterruptedException ex) {
            Logger.getLogger(TimeCard.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private static void led_control(String led, String color, int value, String trigger, int timer) {
        String path = "/sys/class/fpga/" + led;

        file_put_contents(path + "/brightness", String.valueOf(value));
        file_put_contents(path + "/color", color);

        if (timer < 0) {
            file_put_contents(path + "/delay", String.valueOf(timer));
        }
        if (trigger != null && !trigger.isEmpty()) {
            file_put_contents(path + "/trigger", trigger);
        }
    }

    private static void file_put_contents(String filename, String value) {
        byte buf[] = value.getBytes();

        try (FileOutputStream fos = new FileOutputStream(filename)) {
            fos.write(buf, 0, buf.length);

        } catch (FileNotFoundException ex) {
//            Logger.getLogger(TimeCard.class.getName()).log(Level.SEVERE, "" + filename, ex);
        } catch (IOException ex) {
//            Logger.getLogger(TimeCard.class.getName()).log(Level.SEVERE, "" + filename, ex);
        }
    }
    
    private static boolean slack_add_exec(String message) {
        boolean ret = false;
        slack_add_command[2] = message;

        Runtime runtime = Runtime.getRuntime();
        try {
            Process process = runtime.exec(slack_add_command);

            int rtn = process.waitFor();
            if (rtn != 0) {
                System.out.println("error?" + message);
                return ret;
            }
            process.getInputStream().close();
            process.getErrorStream().close();
            process.getOutputStream().close();
            ret = true;

        } catch (IOException | InterruptedException ex) {
            ret = false;
        }
        return ret;
    }

}
