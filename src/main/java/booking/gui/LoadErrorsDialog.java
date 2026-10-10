package booking.gui;

import booking.io.CsvLoadException;
import booking.io.LoadErrorCode;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

// Сводка по кодам ошибок и таблица всех пропущенных строк
public class LoadErrorsDialog extends JDialog {

    public LoadErrorsDialog(Frame owner, int loaded, List<CsvLoadException> errors) {
        super(owner, "Загрузка завершена с ошибками", true);

        // Сводка: сколько ошибок какого кода
        Map<LoadErrorCode, Integer> counts = new EnumMap<>(LoadErrorCode.class);
        for (CsvLoadException e : errors) {
            counts.merge(e.getCode(), 1, Integer::sum);
        }
        StringBuilder html = new StringBuilder("<html>Загружено записей: " + loaded
                + "<br>Пропущено строк: " + errors.size() + "<br>");
        counts.forEach((code, n) -> html.append("&nbsp;&nbsp;• ")
                .append(code).append(" — ").append(code.getTitle()).append(": ").append(n).append("<br>"));
        html.append("</html>");

        // Таблица: строка / код / описание (ячейки не редактируются)
        DefaultTableModel tableModel = new DefaultTableModel(new String[]{"Строка", "Код ошибки", "Описание"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        for (CsvLoadException e : errors) {
            tableModel.addRow(new Object[]{e.getLineNo(), e.getCode().name(), e.getMessage()});
        }
        JTable table = new JTable(tableModel);
        table.getColumnModel().getColumn(0).setMaxWidth(70);
        table.getColumnModel().getColumn(1).setPreferredWidth(160);
        table.getColumnModel().getColumn(2).setPreferredWidth(420);

        JLabel summary = new JLabel(html.toString());
        summary.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));

        JButton ok = new JButton("OK");
        ok.addActionListener(e -> dispose());
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.add(ok);

        add(summary, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(ok);

        setSize(720, 400);
        setLocationRelativeTo(owner);
    }
}
