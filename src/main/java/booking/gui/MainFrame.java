package booking.gui;

import booking.io.CsvFileException;
import booking.io.CsvLoader;
import booking.io.CsvSaver;
import booking.model.Booking;
import booking.model.Editable;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.io.IOException;

//Главное окно: таблица броней и четыре кнопки
public class MainFrame extends JFrame {
    private final BookingTableModel model = new BookingTableModel();
    private final JTable table = new JTable(model);

    private final JButton loadButton = new JButton("Загрузить из CSV");
    private final JButton saveButton = new JButton("Сохранить в CSV");
    private final JButton addButton = new JButton("Добавить");
    private final JButton editButton = new JButton("Изменить");

    private final CsvLoader loader = new CsvLoader();
    private final CsvSaver saver = new CsvSaver();

    public MainFrame() {
        super("Бронирование переговорных");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getSelectionModel().addListSelectionListener(e -> updateEditButton());

        loadButton.addActionListener(e -> onLoad());
        saveButton.addActionListener(e -> onSave());
        addButton.addActionListener(e -> onAdd());
        editButton.addActionListener(e -> onEdit());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttons.add(loadButton);
        buttons.add(saveButton);
        buttons.add(addButton);
        buttons.add(editButton);

        add(buttons, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);

        updateEditButton();
        setSize(950, 450);
        setLocationRelativeTo(null);
    }
//Изменить доступна только если выбрана строка и её объект реализует Editable.
    private void updateEditButton() {
        int row = table.getSelectedRow();
        boolean canEdit = row >= 0 && model.getBooking(row) instanceof Editable;
        editButton.setEnabled(canEdit);
    }

    private void onLoad() {
        JFileChooser chooser = new JFileChooser(".");
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try {
            CsvLoader.LoadResult r = loader.load(chooser.getSelectedFile().toPath());
            model.setAll(r.bookings());
            updateEditButton();

            if (r.errors().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Загружено записей: " + r.bookings().size(),
                        "Загрузка завершена", JOptionPane.INFORMATION_MESSAGE);
            } else {
                // есть пропущенные строки — показываем коды и причины
                new LoadErrorsDialog(this, r.bookings().size(), r.errors()).setVisible(true);
            }
        } catch (CsvFileException ex) {
            showError("[" + ex.getCode() + "] " + ex.getCode().getTitle() + ":\n" + ex.getMessage());
        }
    }

    private void onSave() {
        JFileChooser chooser = new JFileChooser(".");
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try {
            saver.save(chooser.getSelectedFile().toPath(), model.getAll());
            JOptionPane.showMessageDialog(this, "Сохранено записей: " + model.getRowCount(),
                    "Сохранение", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            showError("Не удалось сохранить файл:\n" + ex.getMessage());
        }
    }

    // Через добавить создаются только редактируемые типы
    private void onAdd() {
        BookingDialog dialog = new BookingDialog(this, null, nextId());
        dialog.setVisible(true);
        Booking created = dialog.getResult();
        if (created == null || hasConflict(created)) return;
        model.add(created);
    }

    private void onEdit() {
        int row = table.getSelectedRow();
        if (row < 0) return;
        Booking old = model.getBooking(row);
        if (!(old instanceof Editable)) {
            showError("Эту запись нельзя изменять");
            return;
        }
        BookingDialog dialog = new BookingDialog(this, old, old.getId());
        dialog.setVisible(true);
        Booking changed = dialog.getResult();
        if (changed == null || hasConflict(changed)) return;
        model.replace(row, changed);
    }

    private int nextId() {
        return model.getAll().stream().mapToInt(Booking::getId).max().orElse(0) + 1;
    }

    //Проверка пересечения по времени с другой бронью
    private boolean hasConflict(Booking candidate) {
        for (Booking other : model.getAll()) {
            if (other.getId() != candidate.getId() && other.overlaps(candidate)) {
                showError("Конфликт: комната «" + candidate.getRoom() + "» уже занята\n" + other);
                return true;
            }
        }
        return false;
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Ошибка", JOptionPane.ERROR_MESSAGE);
    }
}
