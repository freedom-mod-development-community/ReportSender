package xyz.fmdc.reportsender;

public class Report {
    public final String fileName;
    public final String fullReport;
    public String editedReport;

    public Report(String fileName, String fullReport) {
        this.fileName = fileName;
        this.fullReport = fullReport;
    }
}
