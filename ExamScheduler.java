import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.*;

public class ExamScheduler extends JFrame {

    private JComboBox<String> subjectDropdown;
    private JComboBox<Integer> dayDropdown, monthDropdown, yearDropdown;
    private JTextField timeField;
    private JTable examTable;
    private DefaultTableModel tableModel;
    private JButton deleteButton;

    public ExamScheduler() {
        setTitle("Exam Scheduler - Admin Panel");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        // Input Panel
        JPanel inputPanel = new JPanel(new GridLayout(5, 2, 10, 10));

        JLabel subjectLabel = new JLabel("Subject:");
        subjectDropdown = new JComboBox<>(new String[]{"EMBEDDED SYSTEM", "SOFTWARE ENGINEERING", "COMPUTER GRAPHICS", "PYTHON", "JAVA", "HINDI", "STOCKS", "COST"});
        populateSubjects();

        JLabel dateLabel = new JLabel("Date:");
        dayDropdown = new JComboBox<>(generateNumberArray(1, 31));
        monthDropdown = new JComboBox<>(generateNumberArray(1, 12));
        yearDropdown = new JComboBox<>(generateNumberArray(2020, 2030));

        JPanel datePanel = new JPanel();
        datePanel.add(dayDropdown);
        datePanel.add(monthDropdown);
        datePanel.add(yearDropdown);

        JLabel timeLabel = new JLabel("Time (HH:MM):");
        timeField = new JTextField();

        JButton submitButton = new JButton("Add Exam");
        submitButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String subject = (String) subjectDropdown.getSelectedItem();
                String date = yearDropdown.getSelectedItem() + "-" +
                        String.format("%02d", monthDropdown.getSelectedItem()) + "-" +
                        String.format("%02d", dayDropdown.getSelectedItem());
                String time = timeField.getText();
                addExam(subject, date, time);
                populateExamTable(); // Refresh the table after adding an exam
            }
        });

        deleteButton = new JButton("Delete Exam");
        deleteButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                deleteExam();
            }
        });

        inputPanel.add(subjectLabel);
        inputPanel.add(subjectDropdown);
        inputPanel.add(dateLabel);
        inputPanel.add(datePanel);
        inputPanel.add(timeLabel);
        inputPanel.add(timeField);
        inputPanel.add(submitButton);
        inputPanel.add(deleteButton);

        // Table Panel
        JPanel tablePanel = new JPanel(new BorderLayout());
        tableModel = new DefaultTableModel(new String[]{"ID", "Subject", "Date", "Time"}, 0);
        examTable = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(examTable);
        tablePanel.add(scrollPane, BorderLayout.CENTER);

        // Add panels to the frame
        add(inputPanel, BorderLayout.NORTH);
        add(tablePanel, BorderLayout.CENTER);

        // Populate the table with existing exams
        populateExamTable();

        setLocationRelativeTo(null);
        setVisible(true);
    }

    private Integer[] generateNumberArray(int start, int end) {
        Integer[] numbers = new Integer[end - start + 1];
        for (int i = 0; i < numbers.length; i++) {
            numbers[i] = start + i;
        }
        return numbers;
    }

    private Connection connect() {
        String url = "jdbc:mysql://localhost:3306/exam_scheduler";
        String user = "root";
        String password = "root"; // Replace with your MySQL password

        try {
            return DriverManager.getConnection(url, user, password);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Database Connection Error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
            return null;
        }
    }

    private void populateSubjects() {
        String query = "SELECT DISTINCT subject FROM exams";
        try (Connection conn = connect(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                subjectDropdown.addItem(rs.getString("subject"));
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error fetching subjects: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private void populateExamTable() {
        tableModel.setRowCount(0); // Clear the table
        String query = "SELECT id, subject, exam_date, exam_time FROM exams";
        try (Connection conn = connect(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                int id = rs.getInt("id");
                String subject = rs.getString("subject");
                String date = rs.getString("exam_date");
                String time = rs.getString("exam_time");
                tableModel.addRow(new Object[]{id, subject, date, time});
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error fetching exams: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private void addExam(String subject, String date, String time) {
        if (subject == null || date.isEmpty() || time.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill all fields.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String query = "INSERT INTO exams (subject, exam_date, exam_time) VALUES (?, ?, ?)";

        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, subject);
            pstmt.setString(2, date);
            pstmt.setString(3, time);

            pstmt.executeUpdate();
            JOptionPane.showMessageDialog(this, "Exam added successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);

            // Clear fields
            timeField.setText("");
            dayDropdown.setSelectedIndex(0);
            monthDropdown.setSelectedIndex(0);
            yearDropdown.setSelectedIndex(0);

            // Refresh subjects dropdown
            subjectDropdown.removeAllItems();
            populateSubjects();

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Database Error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private void deleteExam() {
        int selectedRow = examTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select an exam to delete.", "Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int id = (int) tableModel.getValueAt(selectedRow, 0);
        String query = "DELETE FROM exams WHERE id = ?";
        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
            JOptionPane.showMessageDialog(this, "Exam deleted successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
            populateExamTable(); // Refresh the table
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Database Error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        // Show login page first
        LoginPage loginPage = new LoginPage();
        if (loginPage.isAuthenticated()) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    new ExamScheduler();
                }
            });
        } else {
            JOptionPane.showMessageDialog(null, "Login failed. Exiting application.", "Error", JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        }
    }
}

class LoginPage {
    private boolean authenticated = false;

    public LoginPage() {
        JTextField usernameField = new JTextField();
        JPasswordField passwordField = new JPasswordField();
        JPanel panel = new JPanel(new GridLayout(3, 2));
        panel.add(new JLabel("Username:"));
        panel.add(usernameField);
        panel.add(new JLabel("Password:"));
        panel.add(passwordField);

        int result = JOptionPane.showConfirmDialog(null, panel, "Login", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            String username = usernameField.getText();
            String password = new String(passwordField.getPassword());
            if ("admin".equals(username) && "admin123".equals(password)) {
                authenticated = true;
            }
        }
    }

    public boolean isAuthenticated() {
        return authenticated;
    }
}
