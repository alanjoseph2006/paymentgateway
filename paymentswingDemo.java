import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

// -------------------- Abstract Base Class --------------------
abstract class Payment {
    protected double amount;
    protected String paymentID;
    protected String date;

    public Payment() {}

    public Payment(double amount, String paymentID, String date) {
        this.amount = amount;
        this.paymentID = paymentID;
        this.date = date;
    }

    public abstract String processPayment();
}

// -------------------- Subclasses --------------------
class CreditCardPayment extends Payment {
    private String cardNumber, cardHolder;

    public CreditCardPayment(String paymentID, double amount, String date, String cardNumber, String cardHolder) {
        super(amount, paymentID, date);
        this.cardNumber = cardNumber;
        this.cardHolder = cardHolder;
    }

    @Override
    public String processPayment() {
        return "Processing Credit Card Payment...\nPayment ID: " + paymentID + "\nAmount: ₹" + amount +
                "\nDate: " + date + "\nCard: " + cardNumber + " (" + cardHolder + ")\n✅ Processed successfully!\n";
    }
}

class PayPalPayment extends Payment {
    private String email;

    public PayPalPayment(String paymentID, double amount, String date, String email) {
        super(amount, paymentID, date);
        this.email = email;
    }

    @Override
    public String processPayment() {
        return "Processing PayPal Payment...\nPayment ID: " + paymentID + "\nAmount: ₹" + amount +
                "\nDate: " + date + "\nEmail: " + email + "\n✅ Processed successfully!\n";
    }
}

class BankTransferPayment extends Payment {
    private String bankName, accountNumber;

    public BankTransferPayment(String paymentID, double amount, String date, String bankName, String accountNumber) {
        super(amount, paymentID, date);
        this.bankName = bankName;
        this.accountNumber = accountNumber;
    }

    @Override
    public String processPayment() {
        return "Processing Bank Transfer...\nPayment ID: " + paymentID + "\nAmount: ₹" + amount +
                "\nDate: " + date + "\nBank: " + bankName + "\nAccount: " + accountNumber + "\n✅ Processed successfully!\n";
    }
}

// -------------------- Processor Class --------------------
class PaymentProcessor {
    private final List<Payment> payments = new ArrayList<>();
    private final Connection conn;

    public PaymentProcessor(Connection conn) {
        this.conn = conn;
    }

    public void addPayment(Payment payment, String type, String extra1, String extra2) {
        payments.add(payment);
        saveToDatabase(type, payment.paymentID, payment.amount, payment.date, extra1, extra2);
    }

    private void saveToDatabase(String type, String paymentID, double amount, String date, String extra1, String extra2) {
        try {
            String query = "INSERT INTO payments(payment_type, payment_id, amount, date, extra1, extra2) VALUES (?, ?, ?, ?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, type);
            stmt.setString(2, paymentID);
            stmt.setDouble(3, amount);
            stmt.setString(4, date);
            stmt.setString(5, extra1);
            stmt.setString(6, extra2);
            stmt.executeUpdate();
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public String processAllPayments() {
        StringBuilder sb = new StringBuilder("=== Processing All Payments ===\n");
        for (Payment p : payments) {
            sb.append(p.processPayment()).append("----------------------------------\n");
        }
        return sb.toString();
    }
}

// -------------------- Main Swing GUI --------------------
public class PaymentSwingDemo extends JFrame {
    private JComboBox<String> paymentType;
    private JTextField txtPaymentID, txtAmount, txtDate, txtExtra1, txtExtra2;
    private JLabel lblExtra1, lblExtra2;
    private JTextArea outputArea;
    private PaymentProcessor processor;

    public PaymentSwingDemo() {
        setTitle("Payment System");
        setSize(600, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        Connection conn = connectDatabase();
        processor = new PaymentProcessor(conn);

        JPanel panel = new JPanel(new GridLayout(8, 2, 5, 5));
        panel.setBorder(BorderFactory.createTitledBorder("Enter Payment Details"));

        paymentType = new JComboBox<>(new String[]{"Credit Card", "PayPal", "Bank Transfer"});
        txtPaymentID = new JTextField();
        txtAmount = new JTextField();
        txtDate = new JTextField();
        txtExtra1 = new JTextField();
        txtExtra2 = new JTextField();
        lblExtra1 = new JLabel("Card Number:");
        lblExtra2 = new JLabel("Card Holder:");

        panel.add(new JLabel("Payment Type:"));
        panel.add(paymentType);
        panel.add(new JLabel("Payment ID:"));
        panel.add(txtPaymentID);
        panel.add(new JLabel("Amount:"));
        panel.add(txtAmount);
        panel.add(new JLabel("Date:"));
        panel.add(txtDate);
        panel.add(lblExtra1);
        panel.add(txtExtra1);
        panel.add(lblExtra2);
        panel.add(txtExtra2);

        JButton btnAdd = new JButton("Add Payment");
        JButton btnProcess = new JButton("Process All Payments");

        panel.add(btnAdd);
        panel.add(btnProcess);

        outputArea = new JTextArea();
        outputArea.setEditable(false);
        JScrollPane scroll = new JScrollPane(outputArea);

        add(panel, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);

        paymentType.addActionListener(e -> updateExtraFields());
        btnAdd.addActionListener(e -> addPayment());
        btnProcess.addActionListener(e -> processPayments());

        setVisible(true);
    }

    private Connection connectDatabase() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection conn = DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/paymentdb",
                    "root",
                    "Anugrah@2013" // Replace with your MySQL password
            );
            JOptionPane.showMessageDialog(this, "✅ Database Connected Successfully!");
            return conn;
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "❌ Database Connection Failed: " + e.getMessage());
            return null;
        }
    }

    private void updateExtraFields() {
        String type = (String) paymentType.getSelectedItem();
        if (type.equals("Credit Card")) {
            lblExtra1.setText("Card Number:");
            lblExtra2.setText("Card Holder:");
            lblExtra2.setVisible(true);
            txtExtra2.setVisible(true);
        } else if (type.equals("PayPal")) {
            lblExtra1.setText("PayPal Email:");
            lblExtra2.setVisible(false);
            txtExtra2.setVisible(false);
        } else {
            lblExtra1.setText("Bank Name:");
            lblExtra2.setText("Account Number:");
            lblExtra2.setVisible(true);
            txtExtra2.setVisible(true);
        }
    }

    private void addPayment() {
        try {
            String type = (String) paymentType.getSelectedItem();
            String id = txtPaymentID.getText();
            double amt = Double.parseDouble(txtAmount.getText());
            String date = txtDate.getText();
            String ex1 = txtExtra1.getText();
            String ex2 = txtExtra2.getText();

            Payment p = switch (type) {
                case "Credit Card" -> new CreditCardPayment(id, amt, date, ex1, ex2);
                case "PayPal" -> new PayPalPayment(id, amt, date, ex1);
                default -> new BankTransferPayment(id, amt, date, ex1, ex2);
            };

            processor.addPayment(p, type, ex1, ex2);
            JOptionPane.showMessageDialog(this, "✅ " + type + " Payment Added Successfully!");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "⚠️ Error Adding Payment: " + ex.getMessage());
        }
    }

    private void processPayments() {
        String result = processor.processAllPayments();
        outputArea.setText(result);
        JOptionPane.showMessageDialog(this, "✅ All Payments Processed Successfully!");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(PaymentSwingDemo::new);
    }
}
