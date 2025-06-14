package main.java.com.example.birthdayapp;

import main.java.com.example.birthdayapp.db.DatabaseUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.sql.*;
import java.time.LocalDate;
import java.util.Comparator;

public class MainController {

    @FXML private TextField searchField;
    @FXML private TableView<Birthday> birthdayTable;
    @FXML private TableColumn<Birthday, String> nameColumn;
    @FXML private TableColumn<Birthday, LocalDate> birthdayColumn;
    @FXML private TextField nameField;
    @FXML private DatePicker birthdayPicker;

    private final ObservableList<Birthday> birthdayList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        birthdayColumn.setCellValueFactory(new PropertyValueFactory<>("birthday"));
        birthdayTable.setItems(birthdayList);
        loadBirthdays();
        checkForTodayBirthdays();

        // Add listener to table selection
        birthdayTable.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> showBirthdayDetails(newValue));
    }

    private void loadBirthdays() {
        birthdayList.clear();
        String query = "SELECT * FROM classmates";
        try (Connection conn = DatabaseUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                birthdayList.add(new Birthday(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getDate("birthday").toLocalDate()
                ));
            }
            sortBirthdays();
        } catch (SQLException e) {
            showAlert("ডাটাবেস ত্রুটি", "জন্মদিন লোড করা যায়নি।");
        }
    }

    private void sortBirthdays() {
        birthdayList.sort(Comparator.comparing(birthday -> birthday.getBirthday().withYear(LocalDate.now().getYear())));
    }

    private void checkForTodayBirthdays() {
        LocalDate today = LocalDate.now();
        StringBuilder todayBirthdays = new StringBuilder();
        for (Birthday birthday : birthdayList) {
            if (birthday.getBirthday().getMonth() == today.getMonth() &&
                birthday.getBirthday().getDayOfMonth() == today.getDayOfMonth()) {
                todayBirthdays.append(birthday.getName()).append("\n");
            }
        }

        if (todayBirthdays.length() > 0) {
            showAlert("আজকের জন্মদিন!", "আজকে যাদের জন্মদিন:\n" + todayBirthdays);
        }
    }

    @FXML
    private void handleAdd() {
        String name = nameField.getText();
        LocalDate birthday = birthdayPicker.getValue();

        if (name.isEmpty() || birthday == null) {
            showAlert("ইনপুট ত্রুটি", "অনুগ্রহ করে নাম এবং জন্মদিন পূরণ করুন।");
            return;
        }

        String query = "INSERT INTO classmates (name, birthday) VALUES (?, ?)";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, name);
            pstmt.setDate(2, Date.valueOf(birthday));
            pstmt.executeUpdate();
            loadBirthdays();
            clearFields();
        } catch (SQLException e) {
            showAlert("ডাটাবেস ত্রুটি", "নতুন জন্মদিন যোগ করা যায়নি।");
        }
    }

    @FXML
    private void handleUpdate() {
        Birthday selectedBirthday = birthdayTable.getSelectionModel().getSelectedItem();
        if (selectedBirthday == null) {
            showAlert("কিছু নির্বাচন করুন", "আপডেট করার জন্য একটি জন্মদিন নির্বাচন করুন।");
            return;
        }

        String name = nameField.getText();
        LocalDate birthday = birthdayPicker.getValue();

        if (name.isEmpty() || birthday == null) {
            showAlert("ইনপুট ত্রুটি", "অনুগ্রহ করে নাম এবং জন্মদিন পূরণ করুন।");
            return;
        }

        String query = "UPDATE classmates SET name = ?, birthday = ? WHERE id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, name);
            pstmt.setDate(2, Date.valueOf(birthday));
            pstmt.setInt(3, selectedBirthday.getId());
            pstmt.executeUpdate();
            loadBirthdays();
            clearFields();
        } catch (SQLException e) {
            showAlert("ডাটাবেস ত্রুটি", "জন্মদিন আপডেট করা যায়নি।");
        }
    }

    @FXML
    private void handleDelete() {
        Birthday selectedBirthday = birthdayTable.getSelectionModel().getSelectedItem();
        if (selectedBirthday == null) {
            showAlert("কিছু নির্বাচন করুন", "মুছে ফেলার জন্য একটি জন্মদিন নির্বাচন করুন।");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("নিশ্চিত করুন");
        alert.setHeaderText("জন্মদিন মুছে ফেলুন");
        alert.setContentText("আপনি কি নিশ্চিতভাবে এই জন্মদিনটি মুছে ফেলতে চান?");

        if (alert.showAndWait().get() == ButtonType.OK) {
            String query = "DELETE FROM classmates WHERE id = ?";
            try (Connection conn = DatabaseUtil.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setInt(1, selectedBirthday.getId());
                pstmt.executeUpdate();
                loadBirthdays();
                clearFields();
            } catch (SQLException e) {
                showAlert("ডাটাবেস ত্রুটি", "জন্মদিন মুছে ফেলা যায়নি।");
            }
        }
    }

    @FXML
    private void handleSearch() {
        String searchText = searchField.getText().toLowerCase();
        ObservableList<Birthday> filteredList = FXCollections.observableArrayList();

        for (Birthday birthday : birthdayList) {
            if (birthday.getName().toLowerCase().contains(searchText) ||
                String.valueOf(birthday.getBirthday().getMonthValue()).contains(searchText)) {
                filteredList.add(birthday);
            }
        }
        birthdayTable.setItems(filteredList);
    }

    private void showBirthdayDetails(Birthday birthday) {
        if (birthday != null) {
            nameField.setText(birthday.getName());
            birthdayPicker.setValue(birthday.getBirthday());
        } else {
            clearFields();
        }
    }

    private void clearFields() {
        nameField.clear();
        birthdayPicker.setValue(null);
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}