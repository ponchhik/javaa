package booking.io;

import booking.model.Booking;
import booking.model.Editable;
import booking.model.PastBooking;
import booking.model.RecurringBooking;
import booking.model.SingleBooking;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class CsvLoader {

    public record LoadResult(List<Booking> bookings, List<String> errors) { }

    public LoadResult load(Path file) throws IOException {
        List<Booking> result = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        Set<Integer> usedIds = new HashSet<>();

        try (BufferedReader reader = Files.newBufferedReader(file)) {
            reader.readLine(); // строка-заголовок
            String line;
            int lineNo = 1;
            while ((line = reader.readLine()) != null) {
                lineNo++;
                if (line.isBlank()) continue;
                try {
                    Booking b = parse(line);
                    List<String> problems = b instanceof Editable e ? e.validate() : b.validateCommon();
                    if (!problems.isEmpty()) {
                        throw new IllegalArgumentException(String.join("; ", problems));
                    }
                    if (!usedIds.add(b.getId())) {
                        throw new IllegalArgumentException("повторяющийся id " + b.getId());
                    }
                    result.add(b);
                } catch (NumberFormatException ex) {
                    errors.add("Строка " + lineNo + ": неверный числовой формат");
                } catch (DateTimeParseException ex) {
                    errors.add("Строка " + lineNo + ": неверный формат даты/времени");
                } catch (IllegalArgumentException ex) {
                    errors.add("Строка " + lineNo + ": " + ex.getMessage());
                }
            }
        }
        return new LoadResult(result, errors);
    }

    //Разбор одной строки
    private Booking parse(String line) {
        String[] p = line.split(";", -1);
        if (p.length != 7) {
            throw new IllegalArgumentException("неверное число полей (" + p.length + " вместо 7)");
        }
        String type = p[0].trim();
        int id = Integer.parseInt(p[1].trim());
        String room = p[2].trim();
        String employee = p[3].trim();
        LocalDateTime start = LocalDateTime.parse(p[4].trim());
        LocalDateTime end = LocalDateTime.parse(p[5].trim());
        String extra = p[6].trim();

        return switch (type) {
            case "PAST" -> new PastBooking(id, room, employee, start, end);
            case "SINGLE" -> new SingleBooking(id, room, employee, start, end, extra);
            case "RECURRING" -> new RecurringBooking(id, room, employee, start, end, parseDays(extra));
            default -> throw new IllegalArgumentException("неизвестный тип «" + type + "»");
        };
    }

    private static Set<DayOfWeek> parseDays(String text) {
        Set<DayOfWeek> days = EnumSet.noneOf(DayOfWeek.class);
        if (text.isBlank()) return days;
        for (String s : text.split(",")) {
            try {
                days.add(DayOfWeek.valueOf(s.trim().toUpperCase()));
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException("неизвестный день недели «" + s.trim() + "»");
            }
        }
        return days;
    }
}
