package booking.io;

import booking.model.Booking;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

// Сохранение броней в CSV
public final class CsvSaver {

    public void save(Path file, List<Booking> bookings) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(file)) {
            writer.write("type;id;room;employee;start;end;extra");
            writer.newLine();
            for (Booking b : bookings) {
                writer.write(String.join(";",
                        b.csvType(),
                        String.valueOf(b.getId()),
                        b.getRoom(),
                        b.getEmployee(),
                        b.getStart().toString(),
                        b.getEnd().toString(),
                        b.extraCsv()));
                writer.newLine();
            }
        }
    }
}
