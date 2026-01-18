package sma;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Utils {
    private static final String LOG_FILE = "sma_log.txt";
    private static final Object lock = new Object();
    private static final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static void log(String line) {

        String ts = LocalDateTime.now().format(fmt);
        String out = String.format("[%s] %s", ts, line);
        System.out.println(out);

        synchronized (lock) {
            try (FileWriter fw = new FileWriter(LOG_FILE, true); PrintWriter pw = new PrintWriter(fw)) {
                pw.println(out);
            } catch (IOException e) { e.printStackTrace(); }
        }
    }
}
