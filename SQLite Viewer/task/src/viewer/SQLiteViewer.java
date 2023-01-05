package viewer;

import org.sqlite.SQLiteDataSource;


import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class SQLiteViewer extends JFrame {

    public SQLiteViewer() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(700,
                800);
        setTitle("SQLite Viewer");
        setLayout(null);
        setResizable(false);
        setLocationRelativeTo(null);
        setVisible(true);

        //First initialize the UI components
        initComponents();
    }


    private void initComponents() {
        JTextField textField = new JTextField();
        textField.setBounds(10,
                10,
                580,
                30);
        textField.setName("FileNameTextField");
        textField.setVisible(true);
        add(textField);


        JButton openButton = new JButton("Open");
        openButton.setBounds(600,
                10,
                70,
                30);
        openButton.setName("OpenFileButton");
        openButton.setVisible(true);
        add(openButton);

        //Add a new JComboBox to the JFrame
        JComboBox<String> comboBox = new JComboBox<>();
        comboBox.setBounds(10,
                50,
                660,
                30);
        comboBox.setName("TablesComboBox");
        comboBox.setVisible(true);
        add(comboBox);

        //Add a new JTextArea to the JFrame
        JTextArea textArea = new JTextArea();
        textArea.setBounds(10,
                90,
                560,
                360);
        textArea.setBackground(Color.WHITE);
        textArea.setName("QueryTextArea");
        textArea.setEnabled(false);
        textArea.setVisible(true);
        add(textArea);

        //Add a new JButton to the JFrame
        JButton executeButton = new JButton("Execute");
        executeButton.setBounds(580,
                90,
                90,
                40);
        executeButton.setName("ExecuteQueryButton");
        executeButton.setEnabled(false);
        executeButton.setVisible(true);
        add(executeButton);

        //Add a new JTable to the JFrame
        JTable table = new JTable();
        table.setBounds(10,
                150,
                660,
                360);
        table.setBackground(Color.WHITE);
        table.setName("Table");
        table.setVisible(true);
        add(table);

        //Add openButton action listener
        openButton.addActionListener(e -> doBuildComboBox(textField,
                comboBox));

        //Add combobox listener
        comboBox.addActionListener(e -> doBuildTextArea(comboBox,
                textArea));

        //Add executeButton openButton listener
        executeButton.addActionListener(e -> doBuildTable(textField,
                textArea,
                table));
    }

    private void doBuildComboBox(JTextField textField, JComboBox<String> comboBox) {
        //Get the file name from the text field
        String fileName = textField.getText();


        //Get the project root directory
        String projectRoot = System.getProperty("user.dir");
        System.out.println("Project root: " + projectRoot);
        String dbPath = projectRoot + File.separator + fileName;

        File file = new File(dbPath);

        //If filename doesn't exist or is empty, show a message dialog
        if (fileName == null || !fileName.endsWith(".db") || !file.exists()) {
            //Clear comboBox
            comboBox.removeAllItems();

            disableComponents();
            JOptionPane.showMessageDialog(new Frame(),
                    "Wrong file name!");
            return;
        }
        else {
            enableComponents();
        }
        //Create a connection to the database
        try {
            Connection con = createConnection(fileName);
            //Get all tables in the schema
            List<String> allTables = getAllTables(con);

            if(allTables.isEmpty()) {
                //Clear comboBox
                comboBox.removeAllItems();

                disableComponents();
                JOptionPane.showMessageDialog(new Frame(),
                        "Wrong file name!");
            }
            else {
                enableComponents();
                //Clear prior entries in the combobox
                comboBox.removeAllItems();
                //Add all tables to the combo box
                allTables.forEach(comboBox::addItem);
            }

        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    private void enableComponents() {
        Component[] components = getContentPane().getComponents();
        for (Component component : components) {
            if ("TablesComboBox".equals(component.getName())
                    || "QueryTextArea".equals(component.getName())
                    || "ExecuteQueryButton".equals(component.getName())) {
                component.setEnabled(true);
            }
        }
    }

    private void disableComponents() {
        Component[] components = getContentPane().getComponents();
        for (Component component : components) {
            if ("TablesComboBox".equals(component.getName())
                    || "QueryTextArea".equals(component.getName())
                    || "ExecuteQueryButton".equals(component.getName())) {
                component.setEnabled(false);
            }
        }
    }

    private static void doBuildTextArea(JComboBox<String> comboBox, JTextArea textArea) {
        //Get the selected table
        String selectedTable = (String) comboBox.getSelectedItem();
        //ensure text area uis enabled
        textArea.setEnabled(true);
        //Add a select query to text area
        textArea.setText("SELECT * FROM " + selectedTable + ";");
    }

    private void doBuildTable(JTextField textField, JTextArea textArea, JTable table) {
        //Get the file name from the text field
        String fileName = textField.getText();
        //Get the query from the text area
        String query = textArea.getText();

//        textArea.setEnabled(false); //textArea should be disabled while the table is being built
        //If a user enters an invalid query, show a message dialog
        //query not matches select * from fileName
        if (!query.matches("SELECT\\s\\*\\sFROM\\s\\w+;")) {
//            Component[] components = getContentPane().getComponents();
//            for (Component component : components) {
//                if (component.getName().equals("TablesComboBox")
//                        || component.getName().equals("QueryTextArea")
//                        || component.getName().equals("ExecuteQueryButton")) {
//                    component.setEnabled(false);
//                }
//            }
            JOptionPane.showMessageDialog(new Frame(),
                    "Invalid query");
            return;
        }
//        else {
//            Component[] components = getContentPane().getComponents();
//            for (Component component : components) {
//                if (component.getName().equals("TablesComboBox")
//                        || component.getName().equals("QueryTextArea")
//                        || component.getName().equals("ExecuteQueryButton")) {
//                    component.setEnabled(true);
//                }
//            }
//        }
        //Create a connection to the database
        try {
            Connection con = createConnection(fileName);
            //Execute the query
            ResultSet rs = con.createStatement().executeQuery(query);
            //Create a new table model
            DefaultTableModel model = new DefaultTableModel();
            List<String> columnNames = new ArrayList<>();
            //Add column names to the model
            for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
                columnNames.add(rs.getMetaData().getColumnName(i));
            }
            //Add data to the model
            int row = 0;
            List<List<String>> rows = new ArrayList<>();
            while (rs.next()) {
                List<String> rowData = new ArrayList<>();
                for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
                    rowData.add(rs.getString(i));
                }
                rows.add(rowData);
                row++;
            }
            //Add columns to the model
            columnNames.forEach(model::addColumn);
            //Add rows to the model
            rows.forEach(rowData -> model.addRow(rowData.toArray()));

            //Update the column and row count
            model.setColumnCount(columnNames.size());
            model.setRowCount(row);
            //Set the table model to the table
            table.setModel(model);

            model.addTableModelListener(new CustomListener()); //Adds the TableModelListener

            JScrollPane sp = new JScrollPane(table);
            table.setFillsViewportHeight(true);
            sp.setBackground(Color.WHITE);
            sp.setVisible(true);
            sp.setBounds(10,
                    150,
                    660,
                    360);
            add(sp);
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    private Connection createConnection(String databaseName) throws SQLException {
        //Establish a connection to the SQLite Datasource
        SQLiteDataSource dataSource = new SQLiteDataSource();
        //Set connection url
        dataSource.setUrl("jdbc:sqlite:" + databaseName);
        return dataSource.getConnection();
    }

    private List<String> getAllTables(Connection con) throws SQLException {
        List<String> tables;
        try (Statement stmt = con.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT name FROM main.sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' ORDER BY name");
            tables = new ArrayList<>();
            while (rs.next()) {
                System.out.println(rs.getString(1));
                tables.add(rs.getString("name"));
            }
        }
        return tables;
    }
}

class CustomListener implements TableModelListener {
    @Override
    public void tableChanged(TableModelEvent e) {
        System.out.println("Table Updated!");
    }
}