package com.example;

import java.io.StringReader;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Set;

import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Period;
import net.fortuna.ical4j.model.component.CalendarComponent;

public class App {
    public static void main(String[] args) throws Exception {
        ZoneId timezone = ZoneId.of("Australia/Sydney");

        String ics = "BEGIN:VCALENDAR\n" +
                "BEGIN:VEVENT\n" +
                "DTSTART;TZID=Australia/Sydney:20240101T140000\n" +
                "DTEND;TZID=Australia/Sydney:20240101T200000\n" +
                "RRULE:FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR,SA,SU\n" +
                "END:VEVENT\n" +
                "END:VCALENDAR";
        StringReader reader = new StringReader(ics);
        CalendarBuilder builder = new CalendarBuilder();
        Calendar calendar = builder.build(reader);

        ZonedDateTime from = ZonedDateTime.of(2024, 11, 1, 0, 0, 0, 0, timezone);
        ZonedDateTime to = ZonedDateTime.of(2024, 11, 2, 0, 0, 0, 0, timezone);
        Period<ZonedDateTime> period = new Period<ZonedDateTime>(from, to);

        for (CalendarComponent component : calendar.getComponents("VEVENT")) {
            Set<Period<ZonedDateTime>> recurrences = component.calculateRecurrenceSet(period);

            for (Period<ZonedDateTime> recurrence : recurrences) {
                System.out.println("Australia/Sydney: " + recurrence.getStart());
                System.out.println("UTC:              " + recurrence.getStart().toInstant());
            }
        }
    }
}
