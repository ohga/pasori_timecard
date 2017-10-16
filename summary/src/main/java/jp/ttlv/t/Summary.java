package jp.ttlv.t;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.model.CalendarList;
import com.google.api.services.calendar.model.CalendarListEntry;
import com.google.api.services.calendar.model.Event;
import jp.ttlv.t.google.Calender;
import jp.ttlv.t.google.OAuth2;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.TreeMap;

public class Summary {

    private static final String store_dir = System.getProperty("user.home") + File.separator + ".store";

    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.out.println("usage: java -jar summary.jar [yyyy] [mm] [IDm]");
            System.out.println(" [yyyy].length = 4, [mm].length = 2, [IDm].length = 16");
            return;
        }

        String yyyy = args[0];
        String mm = args[1];
        String IDm = "9999999999999999";
        if (args.length == 3) {
            IDm = args[2];
        }
        if (yyyy.length() != 4 || mm.length() != 2 || IDm.length() != 16) {
            System.out.println("usage: java -jar summary.jar [yyyy] [mm] [IDm]");
            System.out.println(" [yyyy].length = 4, [mm].length = 2, [IDm].length = 16");
            return;
        }
        
        Properties properties = get_store_file(IDm);

        OAuth2 obj = new OAuth2();
        GoogleCredential credential = obj.getGoogleCredential(properties.getProperty("refresh_token"));

        properties.setProperty("access_token", credential.getAccessToken());
        properties.setProperty("refresh_token", credential.getRefreshToken());
        properties.store(new FileOutputStream(store_dir + File.separator + "token." + IDm + ".properties"), "");

        Calender cal = new Calender();
        com.google.api.services.calendar.Calendar client = cal.getCalendarClient(credential);

        String calendar_id = "";
        CalendarList feed = client.calendarList().list().execute();
        for (CalendarListEntry entry : feed.getItems()) {
            if (entry.isPrimary()) {
                calendar_id = entry.getId();
                break;
            }
        }

        String title = "おしごと";
        Calendar calendar = Calendar.getInstance();
        calendar.clear();
        calendar.set(Integer.parseInt(yyyy), Integer.parseInt(mm) - 1, 1, 0, 0, 0);
        calendar.add(Calendar.DAY_OF_MONTH, -1);
        calendar.set(Calendar.DAY_OF_MONTH, 16);
        Date start = calendar.getTime();

        calendar.clear();
        calendar.set(Integer.parseInt(yyyy), Integer.parseInt(mm) - 1, 15, 23, 59, 59);
        Date end = calendar.getTime();
        
        Map<String, List<String>> map = new TreeMap<>();
        {
          Calendar cal2 = Calendar.getInstance();
          cal2.clear();
          cal2.set(Integer.parseInt(yyyy), Integer.parseInt(mm) - 1, 1, 0, 0, 0);
          cal2.add(Calendar.DAY_OF_MONTH, -1);
          cal2.set(Calendar.DAY_OF_MONTH, 16);
          while(true){
            String key = String.format("%04d%02d%02d",
              cal2.get(Calendar.YEAR), cal2.get(Calendar.MONTH) + 1, cal2.get(Calendar.DAY_OF_MONTH));
            
            List<String> tmp = new ArrayList<>();
            tmp.add("9999");
            tmp.add("9999");
            map.put(key, tmp);
            cal2.add(Calendar.DAY_OF_MONTH, 1);
            Date dd = cal2.getTime();
            if(dd.compareTo(end) > 0) break;
          }
        }


        String pageToken = null;
        long s_tm = 0, e_tm = 0;
        do {
            com.google.api.services.calendar.model.Events events = client.events()
                    .list("primary")
                    .setQ(title)
                    .setCalendarId(calendar_id)
                    .setTimeMin(new DateTime(start, TimeZone.getTimeZone("UTC")))
                    .setTimeMax(new DateTime(end, TimeZone.getTimeZone("UTC")))
                    .setPageToken(pageToken)
                    .setOrderBy("updated")
                    .execute();
            List<Event> items = events.getItems();
            for (Event event : items) {
                if(s_tm == 0){
                  s_tm = event.getStart().getDateTime().getValue();
                }
                e_tm = event.getStart().getDateTime().getValue();
                calendar.clear();
                calendar.setTimeInMillis(event.getStart().getDateTime().getValue());
                String key = String.format("%04d%02d%02d",
                        calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH));
                String start_tm = String.format("%02d%02d", calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE));
                calendar.clear();
                calendar.setTimeInMillis(event.getEnd().getDateTime().getValue());
                String end_tm = String.format("%02d%02d", calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE));

                List<String> tmp = map.get(key);
                if (tmp != null) {
                    int n1 = Integer.parseInt(tmp.get(0));
                    int n2 = Integer.parseInt(tmp.get(1));
                    int m1 = Integer.parseInt(start_tm);
                    int m2 = Integer.parseInt(end_tm);
                    if (n1 < m1 && n1 != 9999) {
                        start_tm = tmp.get(0);
                    }
                    if (n2 > m2 && n2 != 9999) {
                        end_tm = tmp.get(1);
                    }
                }
                tmp = new ArrayList<>();
                tmp.add(start_tm);
                tmp.add(end_tm);
                map.put(key, tmp);
            }
            pageToken = events.getNextPageToken();
        } while (pageToken != null);

        String fn = String.format("%s%s.csv", yyyy, mm);
        System.out.println(fn);            
        System.out.println("--- 8< --- 8< --- 8< ---");            
        StringBuilder sb = new StringBuilder();
        Iterator<String> it = map.keySet().iterator();
        while (it.hasNext()) {
            String key = it.next();
            List<String> tmp = map.get(key);
            String val = String.format("%s,,\n", key);
            if (!tmp.get(0).equals("9999") || !tmp.get(1).equals("9999")) {
                val = String.format("%s,%s:%s,%s:%s\n",
                        key,
                        tmp.get(0).substring(0, 2),tmp.get(0).substring(2, 4),
                        tmp.get(1).substring(0, 2),tmp.get(1).substring(2, 4));
            }
            System.out.print(val);            
            sb.append(val);
        }
        file_put_contents(fn, sb.toString());
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
}
