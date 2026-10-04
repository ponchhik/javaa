package booking.gui;

import booking.model.Booking;

import javax.swing.table.AbstractTableModel;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

//Модель таблицы: хранит список броней и отдаёт их таблице JTable
public class BookingTableModel extends AbstractTableModel {
    public static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private static final String[] COLUMNS =
            {"ID", "Тип", "Комната", "Сотрудник", "Начало", "Конец", "Дополнительно"};

    private final List<Booking> data = new ArrayList<>();

    @Override public int getRowCount() { return data.size(); }
    @Override public int getColumnCount() { return COLUMNS.length; }
    @Override public String getColumnName(int column) { return COLUMNS[column]; }

    @Override
    public Object getValueAt(int row, int column) {
        Booking b = data.get(row);
        return switch (column) {
            case 0 -> b.getId();
            case 1 -> b.typeTitle();
            case 2 -> b.getRoom();
            case 3 -> b.getEmployee();
            case 4 -> b.getStart().format(FMT);
            case 5 -> b.getEnd().format(FMT);
            case 6 -> b.extraText();
            default -> "";
        };
    }

    public Booking getBooking(int row) {
        return data.get(row);
    }

    public List<Booking> getAll() {
        return Collections.unmodifiableList(data);
    }

    public void setAll(List<Booking> bookings) {
        data.clear();
        data.addAll(bookings);
        fireTableDataChanged();
    }

    public void add(Booking b) {
        data.add(b);
        fireTableRowsInserted(data.size() - 1, data.size() - 1);
    }

    public void replace(int row, Booking b) {
        data.set(row, b);
        fireTableRowsUpdated(row, row);
    }
}
