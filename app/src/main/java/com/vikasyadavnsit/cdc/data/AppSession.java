package com.vikasyadavnsit.cdc.data;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AppSession {
    private long startTime;
    private long endTime;
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());

    @Override
    public String toString() {
        return "[ O: " + dateFormat.format(new Date(startTime)) + "  C: " + dateFormat.format(new Date(endTime)) + " ]\n";
    }
}
