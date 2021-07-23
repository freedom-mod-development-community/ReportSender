package xyz.fmdc.reportsender;

import javax.net.ssl.HttpsURLConnection;
import javax.swing.*;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.*;

public class Main {
    public static List<Report> reports = new ArrayList<>();
    public static Map<String, Hook> hooks = new HashMap<>();
    protected static Dialog dialog;

    public static void main(String[] args) {
        dialog = new Dialog();
        RSScanner sc = new RSScanner();
        sc.start();
        HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);
    }

    public static void loadData(String line) {
        String[] mes = line.split(" ");
        try {
            switch (mes[0]) {
                case "REPORT":
                    reports.add(new Report(URLDecoder.decode(mes[1], "UTF-8"),
                            URLDecoder.decode(mes[2], "UTF-8")));
                    break;
                case "HOOK":
                    if (!hooks.containsKey(URLDecoder.decode(mes[1], "UTF-8")))
                        hooks.put(URLDecoder.decode(mes[1], "UTF-8"),
                                new Hook(
                                        URLDecoder.decode(mes[1], "UTF-8"),
                                        URLDecoder.decode(mes[2], "UTF-8"),
                                        URLDecoder.decode(mes[3], "UTF-8"),
                                        URLDecoder.decode(mes[4], "UTF-8"),
                                        URLDecoder.decode(mes[5], "UTF-8")));
                    break;
                case "END_HOOKS":
                    updateDestinations();
                    break;
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        updateReport(dialog.getAnonymous(), dialog.getSystemDetails());
    }

    public static void updateReport(boolean hideLevel, boolean hideSystem) {
        if (hideLevel || hideSystem) {
            for (Report report : reports) {
                String[] lines = report.fullReport.split("\n");
                int i = 0;
                int j = 0;
                StringBuilder sb = new StringBuilder();
                for (String line : lines) {
                    boolean append = true;
                    if (hideLevel) {
                        if (i != 0 && line.startsWith("--")) i = 0;
                        if (i == 2 && !line.startsWith("\t")) i = 3;
                        if (i == 1 && line.startsWith("Details:")) ++i;
                        if (i == 0 && line.startsWith("-- Affected level --")) ++i;
                        if (i == 2) append = false;
                    }

                    if (hideSystem) {
                        if (j != 0 && line.startsWith("--")) j = 0;
                        if (j == 0 && line.startsWith("-- System Details --")) ++j;
                        if (j == 1) append = false;
                    }
                    if (append)
                        sb.append(line).append("\n");
                }
                report.editedReport = sb.toString();
            }
        } else {
            reports.forEach(report -> report.editedReport = report.fullReport);
        }
        StringBuilder sb = new StringBuilder();
        sb.append(getDestinations());
        for (int i = 0; i < reports.size(); ++i)
            sb.append("===== REPORT ").append(i).append(" =====\n").append(reports.get(i).editedReport).append("\n");
        dialog.setText(sb.toString());
    }

    public static String getDestinations() {
        StringBuilder sb = new StringBuilder();
        sb.append("===== DESTINATIONS =====\n");
        for (Hook hook : hooks.values())
            sb.append("Name: ").append(hook.name).append("\nDeveloper: ").append(hook.developer)
                    .append("\nDescription: ").append(hook.description).append("\n\n");
        sb.append("===== END DESTINATIONS =====\n\n");
        return sb.toString();
    }

    public static void updateDestinations() {
        dialog.destinationModel.clear();
        for (Hook hook : hooks.values()) {
            JCheckBox checkBox = new JCheckBox(hook.name);
            checkBox.setSelected(true);
            dialog.destinationModel.addElement(checkBox);
        }
    }

    static class RSScanner extends Thread {
        @Override
        public void run() {
            Scanner scanner = new Scanner(System.in);
            String line;
            System.out.println("k");
            while ((line = scanner.nextLine()) != null) {
                Main.loadData(line);
            }
        }
    }
}
