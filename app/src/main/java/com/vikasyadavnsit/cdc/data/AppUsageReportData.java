package com.vikasyadavnsit.cdc.data;

import java.util.ArrayList;
import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AppUsageReportData {

    private String packageName;
    private int openCount;
    private long lastOpenTime;
    private long totalTimeUsed;

    @Builder.Default
    private List<AppSession> sessions = new ArrayList<>();

    public void incrementOpenCount() {
        openCount++;
    }

    public void addUsageTime(long timeUsed) {
        totalTimeUsed += timeUsed;
    }

    public void addSession(AppSession session) {
        sessions.add(session);
    }

    @Override
    public String toString() {
        String toStr = "Package: " + packageName + "\nOpened: " + openCount + " times" + "\nTotal Time Used: " + formatDuration(totalTimeUsed) + "\nSessions:\n";
        for(AppSession session : sessions){
            toStr += session.toString();
        }
        return toStr;
    }

    private static String formatDuration(long milliseconds) {
        long totalSeconds = milliseconds / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        return String.format("%02dHours %02dMinutes %02dSeconds", hours, minutes, seconds);
    }
}
