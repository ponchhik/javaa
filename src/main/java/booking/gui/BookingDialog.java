package booking.gui;

import booking.model.Booking;
import booking.model.RecurringBooking;
import booking.model.SingleBooking;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.time.format.TextStyle;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

//Диалог добавления / редактирования брони
public class BookingDialog extends JDialog {
    private static final Locale RU = Locale.forLanguageTag("ru");

    private final JComboBox<String> typeBox = new JComboBox<>(new String[]{"Обычная", "Регулярная"});
    private final JTextField roomField = new JTextField(20);
    private final JTextField employeeField = new JTextField(20);
    private final JTextField startField = new JTextField(20);
    private final JTextField endField = new JTextField(20);
    private final JTextField commentField = new JTextField(20);
    private final EnumMap<DayOfWeek, JCheckBox> dayBoxes = new EnumMap<>(DayOfWeek.class);

    private final int id;
    private Booking result;

    public BookingDialog(Frame owner, Booking existing, int newId) {
        super(owner, existing == null ? "Добавить бронь" : "Изменить бронь", true);
        this.id = existing == null ? newId : existing.getId();

        JPanel daysPanel = new JPanel(new GridLayout(1, 7));
        for (DayOfWeek d : DayOfWeek.values()) {
            JCheckBox box = new JCheckBox(d.getDisplayName(TextStyle.SHORT, RU));
            dayBoxes.put(d, box);
            daysPanel.add(box);
        }

        JPanel form = new JPanel(new GridLayout(0, 2, 5, 5));
        form.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        form.add(new JLabel("Тип:"));                          form.add(typeBox);
        form.add(new JLabel("Комната:"));                      form.add(roomField);
        form.add(new JLabel("Сотрудник:"));                    form.add(employeeField);
        form.add(new JLabel("Начало (гггг-мм-дд чч:мм):"));    form.add(startField);
        form.add(new JLabel("Конец (гггг-мм-дд чч:мм):"));     form.add(endField);
        form.add(new JLabel("Комментарий (обычная):"));        form.add(commentField);
        form.add(new JLabel("Дни недели (регулярная):"));      form.add(daysPanel);

        typeBox.addActionListener(e -> updateEnabled());

        if (existing != null) {
            fill(existing);
        } else {
            LocalDateTime next = LocalDateTime.now().withMinute(0).withSecond(0).withNano(0).plusHours(1);
            startField.setText(next.format(BookingTableModel.FMT));
            endField.setText(next.plusHours(1).format(BookingTableModel.FMT));
        }
        updateEnabled();

        JButton okButton = new JButton("OK");
        JButton cancelButton = new JButton("Отмена");
        okButton.addActionListener(e -> onOk());
        cancelButton.addActionListener(e -> dispose());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(okButton);
        buttons.add(cancelButton);

        add(form, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);
        pack();
        setLocationRelativeTo(owner);
    }

    public Booking getResult() {
        return result;
    }

    private void fill(Booking b) {
        roomField.setText(b.getRoom());
        employeeField.setText(b.getEmployee());
        startField.setText(b.getStart().format(BookingTableModel.FMT));
        endField.setText(b.getEnd().format(BookingTableModel.FMT));
        if (b instanceof RecurringBooking r) {
            typeBox.setSelectedIndex(1);
            for (DayOfWeek d : r.getDays()) dayBoxes.get(d).setSelected(true);
        } else if (b instanceof SingleBooking s) {
            typeBox.setSelectedIndex(0);
            commentField.setText(s.getComment());
        }
        typeBox.setEnabled(false); // тип существующей брони менять нельзя
    }

    private void updateEnabled() {
        boolean recurring = typeBox.getSelectedIndex() == 1;
        commentField.setEnabled(!recurring);
        dayBoxes.values().forEach(box -> box.setEnabled(recurring));
    }

    private void onOk() {
        LocalDateTime start;
        LocalDateTime end;
        try {
            start = LocalDateTime.parse(startField.getText().trim(), BookingTableModel.FMT);
            end = LocalDateTime.parse(endField.getText().trim(), BookingTableModel.FMT);
        } catch (DateTimeParseException ex) {
            showError("Неверный формат даты. Ожидается: гггг-мм-дд чч:мм\nНапример: 2026-10-05 09:30");
            return;
        }

        String room = roomField.getText().trim();
        String employee = employeeField.getText().trim();
        Booking created;
        List<String> errors;

        if (typeBox.getSelectedIndex() == 1) {
            Set<DayOfWeek> days = EnumSet.noneOf(DayOfWeek.class);
            dayBoxes.forEach((day, box) -> { if (box.isSelected()) days.add(day); });
            RecurringBooking r = new RecurringBooking(id, room, employee, start, end, days);
            errors = r.validate();
            created = r;
        } else {
            SingleBooking s = new SingleBooking(id, room, employee, start, end, commentField.getText().trim());
            errors = s.validate();
            created = s;
        }

        if (!errors.isEmpty()) {
            showError(String.join("\n", errors));
            return; // диалог остаётся открытым
        }
        result = created;
        dispose();
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Ошибка", JOptionPane.ERROR_MESSAGE);
    }
}
