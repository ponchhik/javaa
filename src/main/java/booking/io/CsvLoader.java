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
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class CsvLoader {
    private static final int FIELD_COUNT = 7;

    public record LoadResult(List<Booking> bookings, List<CsvLoadException> errors) {

        public Map<LoadErrorCode, Long> errorsByCode() {
            Map<LoadErrorCode, Long> map = new EnumMap<>(LoadErrorCode.class);
            for (CsvLoadException e : errors) {
                map.merge(e.getCode(), 1L, Long::sum);
            }
            return map;
        }
    }

    public LoadResult load(Path file) throws CsvFileException {
        List<Booking> result = new ArrayList<>();
        List<CsvLoadException> errors = new ArrayList<>();
        Set<Integer> usedIds = new HashSet<>();

        try (BufferedReader reader = Files.newBufferedReader(file)) {
            reader.readLine(); // строка-заголовок
            String line;
            int lineNo = 1;
            while ((line = reader.readLine()) != null) {
                lineNo++;
                if (line.isBlank()) continue;
                try {
                    result.add(parseAndCheck(line, lineNo, usedIds));
                } catch (CsvLoadException ex) {
                    errors.add(ex);
                }
            }
        } catch (IOException ex) {
            throw new CsvFileException(ex);
        }
        return new LoadResult(result, errors);
    }

    private Booking parseAndCheck(String line, int lineNo, Set<Integer> usedIds) throws CsvLoadException {
        Booking b = parse(line, lineNo);

        List<String> problems = b instanceof Editable e ? e.validate() : b.validateCommon();
        if (!problems.isEmpty()) {
            throw new CsvValidationException(LoadErrorCode.VALIDATION_FAILED, lineNo, problems);
        }
        if (!usedIds.add(b.getId())) {
            throw new CsvValidationException(LoadErrorCode.DUPLICATE_ID, lineNo,
                    List.of("повторяющийся id " + b.getId()));
        }
        return b;
    }

    // Разбор одной строки
    private Booking parse(String line, int lineNo) throws CsvFormatException {
        String[] p = line.split(";", -1);
        if (p.length != FIELD_COUNT) {
            throw new CsvFormatException(LoadErrorCode.WRONG_FIELD_COUNT, lineNo,
                    "неверное число полей (" + p.length + " вместо " + FIELD_COUNT + ")");
        }
        String type = p[0].trim();
        int id = parseId(p[1], lineNo);
        String room = p[2].trim();
        String employee = p[3].trim();
        LocalDateTime start = parseDateTime(p[4], "начала", lineNo);
        LocalDateTime end = parseDateTime(p[5], "конца", lineNo);
        String extra = p[6].trim();

        return switch (type) {
            case "PAST" -> new PastBooking(id, room, employee, start, end);
            case "SINGLE" -> new SingleBooking(id, room, employee, start, end, extra);
            case "RECURRING" -> new RecurringBooking(id, room, employee, start, end, parseDays(extra, lineNo));
            default -> throw new CsvFormatException(LoadErrorCode.UNKNOWN_TYPE, lineNo,
                    "неизвестный тип «" + type + "»");
        };
    }

    private static int parseId(String text, int lineNo) throws CsvFormatException {
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException ex) {
            throw new CsvFormatException(LoadErrorCode.BAD_NUMBER, lineNo,
                    "id «" + text.trim() + "» не является целым числом", ex);
        }
    }

    private static LocalDateTime parseDateTime(String text, String what, int lineNo) throws CsvFormatException {
        try {
            return LocalDateTime.parse(text.trim());
        } catch (DateTimeParseException ex) {
            throw new CsvFormatException(LoadErrorCode.BAD_DATE_TIME, lineNo,
                    "неверная дата/время " + what + " «" + text.trim() + "»", ex);
        }
    }

    private static Set<DayOfWeek> parseDays(String text, int lineNo) throws CsvFormatException {
        Set<DayOfWeek> days = EnumSet.noneOf(DayOfWeek.class);
        if (text.isBlank()) return days;
        for (String s : text.split(",")) {
            try {
                days.add(DayOfWeek.valueOf(s.trim().toUpperCase()));
            } catch (IllegalArgumentException ex) {
                throw new CsvFormatException(LoadErrorCode.BAD_DAY_OF_WEEK, lineNo,
                        "неизвестный день недели «" + s.trim() + "»", ex);
            }
        }
        return days;
    }
}
