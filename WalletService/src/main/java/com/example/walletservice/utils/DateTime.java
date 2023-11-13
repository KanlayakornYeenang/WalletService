package com.example.walletservice.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DateTime {
    public static String[] getDateAndTime(String inputString) {
        // Define the input and output format
        DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSS");
        DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss a");

        // Parse the input string
        LocalDateTime dateTime = LocalDateTime.parse(inputString, inputFormatter);

        // Format the date and time in AM/PM format
        String formattedDateAndTime = dateTime.format(outputFormatter);

        // Split date and time
        return formattedDateAndTime.split(" ");
    }
}
