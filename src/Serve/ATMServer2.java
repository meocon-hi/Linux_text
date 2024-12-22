package Serve;

import comon.ATMRemote;
import comon.DatabaseUtil;

import javax.swing.*;
import java.awt.*;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ATMServer2 extends UnicastRemoteObject implements ATMRemote {
    private DefaultListModel<String> clientListModel;
    private JTextArea logArea;
    private ATMRemote otherServer; // Server thứ nhất để đồng bộ dữ liệu
    private String otherServerIP;
    private int otherServerPort;

    // GUI Components for Server Configuration
    private JFrame configFrame;
    private JTextField ipField;
    private JTextField portField;
    private JButton connectButton;

    protected ATMServer2() throws RemoteException {
        super();
        setupConfigUI(); // Bật giao diện cấu hình
    }

    // Thiết lập giao diện cấu hình cho Server
    private void setupConfigUI() {
        configFrame = new JFrame("ATM Server2 Configuration");
        configFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        configFrame.setSize(400, 200);
        configFrame.setLayout(new GridBagLayout());
        configFrame.setLocationRelativeTo(null);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel ipLabel = new JLabel("Other Server IP:");
        gbc.gridx = 0;
        gbc.gridy = 0;
        configFrame.add(ipLabel, gbc);

        ipField = new JTextField();
        gbc.gridx = 1;
        gbc.gridy = 0;
        configFrame.add(ipField, gbc);

        JLabel portLabel = new JLabel("Other Server Port:");
        gbc.gridx = 0;
        gbc.gridy = 1;
        configFrame.add(portLabel, gbc);

        portField = new JTextField();
        gbc.gridx = 1;
        gbc.gridy = 1;
        configFrame.add(portField, gbc);

        connectButton = new JButton("Connect and Start Server");
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        configFrame.add(connectButton, gbc);

        connectButton.addActionListener(e -> {
            otherServerIP = ipField.getText().trim();
            String portText = portField.getText().trim();
            if (otherServerIP.isEmpty() || portText.isEmpty()) {
                JOptionPane.showMessageDialog(configFrame, "Vui lòng nhập đầy đủ địa chỉ IP và cổng.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }
            try {
                otherServerPort = Integer.parseInt(portText);
                connectToOtherServer();
                startServer();
                setupServerUI();
                configFrame.dispose();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(configFrame, "Cổng phải là số.", "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        });

        configFrame.setVisible(true);
    }

    // Kết nối tới server thứ nhất để đồng bộ dữ liệu
    private void connectToOtherServer() {
        new Thread(() -> {
            try {
                String url = "rmi://" + otherServerIP + ":" + otherServerPort + "/ATMServer";
                otherServer = (ATMRemote) Naming.lookup(url);
                logArea.append("Đã kết nối tới server thứ nhất tại " + otherServerIP + ":" + otherServerPort + "\n");
            } catch (Exception e) {
                logArea.append("Không thể kết nối tới server thứ nhất: " + e.getMessage() + "\n");
            }
        }).start();
    }

    // Khởi động Server RMI
    private void startServer() {
        new Thread(() -> {
            try {
                // Tạo RMI Registry trên cổng 1239
                Registry registry = LocateRegistry.createRegistry(1239);
                registry.rebind("ATMServer", this);
                logArea.append("Server RMI đã được đăng ký tại rmi://localhost:1239/ATMServer\n");
            } catch (RemoteException e) {
                logArea.append("Lỗi khi tạo RMI Registry: " + e.getMessage() + "\n");
                e.printStackTrace();
            }
        }).start();
    }

    // Thiết lập giao diện GUI cho Server sau khi đã cấu hình
    private void setupServerUI() {
        JFrame frame = new JFrame("ATM Server2");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 500);
        frame.setLayout(new BorderLayout());

        // Log Area
        logArea = new JTextArea();
        logArea.setEditable(false);
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(BorderFactory.createTitledBorder("Server Log"));
        frame.add(logScroll, BorderLayout.CENTER);

        // Client List
        clientListModel = new DefaultListModel<>();
        JList<String> clientList = new JList<>(clientListModel);
        JScrollPane clientScroll = new JScrollPane(clientList);
        clientScroll.setBorder(BorderFactory.createTitledBorder("Connected Clients"));
        clientScroll.setPreferredSize(new Dimension(200, 0));
        frame.add(clientScroll, BorderLayout.EAST);

        frame.setVisible(true);
    }

    // Xác thực người dùng
    @Override
    public boolean authenticateUser(String username, String password) throws RemoteException {
        logArea.append("Đang xác thực người dùng: " + username + "\n");
        try (Connection conn = DatabaseUtil.getConnection()) {
            PreparedStatement ps = conn.prepareStatement("SELECT * FROM users WHERE username = ? AND password = ?");
            ps.setString(1, username);
            ps.setString(2, password);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                // Check if user is already logged in
                if (rs.getInt("is_logged_in") == 1) {
                    logArea.append("Người dùng " + username + " đã đăng nhập ở nơi khác.\n");
                    return false; // User already logged in
                }

                // Update the login status
                PreparedStatement updateStmt = conn.prepareStatement("UPDATE users SET is_logged_in = 1 WHERE username = ?");
                updateStmt.setString(1, username);
                updateStmt.executeUpdate();

                logArea.append("Người dùng " + username + " đã đăng nhập thành công.\n");
                // Sync with other server
                if (otherServer != null) {
                    otherServer.syncLogin(username);
                }
                return true; // Authentication successful
            } else {
                logArea.append("Đăng nhập thất bại cho người dùng " + username + ".\n");
                return false; // Authentication failed
            }
        } catch (SQLException e) {
            logArea.append("Lỗi khi xác thực người dùng: " + e.getMessage() + "\n");
            e.printStackTrace();
            return false;
        }
    }

    // Đồng bộ hóa đăng nhập
    @Override
    public void syncLogin(String username) throws RemoteException {
        try (Connection conn = DatabaseUtil.getConnection()) {
            PreparedStatement updateStmt = conn.prepareStatement("UPDATE users SET is_logged_in = 1 WHERE username = ?");
            updateStmt.setString(1, username);
            updateStmt.executeUpdate();
            logArea.append("Đã đồng bộ hóa đăng nhập cho người dùng: " + username + "\n");
        } catch (SQLException e) {
            logArea.append("Lỗi khi đồng bộ hóa đăng nhập: " + e.getMessage() + "\n");
            e.printStackTrace();
        }
    }

    // Đăng xuất
    @Override
    public boolean logoutUser(String username) throws RemoteException {
        logArea.append("Người dùng " + username + " đang đăng xuất.\n");
        try (Connection conn = DatabaseUtil.getConnection()) {
            PreparedStatement ps = conn.prepareStatement("UPDATE users SET is_logged_in = 0 WHERE username = ?");
            ps.setString(1, username);
            ps.executeUpdate();
            logArea.append("Người dùng " + username + " đã đăng xuất thành công.\n");
            // Sync with other server
            if (otherServer != null) {
                otherServer.syncLogout(username);
            }
            return true; // Logout successful
        } catch (SQLException e) {
            logArea.append("Lỗi khi đăng xuất người dùng: " + e.getMessage() + "\n");
            e.printStackTrace();
            return false;
        }
    }

    // Đồng bộ hóa đăng xuất
    @Override
    public void syncLogout(String username) throws RemoteException {
        try (Connection conn = DatabaseUtil.getConnection()) {
            PreparedStatement updateStmt = conn.prepareStatement("UPDATE users SET is_logged_in = 0 WHERE username = ?");
            updateStmt.setString(1, username);
            updateStmt.executeUpdate();
            logArea.append("Đã đồng bộ hóa đăng xuất cho người dùng: " + username + "\n");
        } catch (SQLException e) {
            logArea.append("Lỗi khi đồng bộ hóa đăng xuất: " + e.getMessage() + "\n");
            e.printStackTrace();
        }
    }

    // Chuyển tiền
    @Override
    public synchronized boolean transferMoney(String fromUser, String toUser, double amount) throws RemoteException {
        logArea.append("Đang chuyển tiền từ " + fromUser + " đến " + toUser + " số tiền: " + amount + "\n");
        boolean success = processTransfer(fromUser, toUser, amount);
        if (success && otherServer != null) {
            // Đồng bộ giao dịch với server thứ nhất
            boolean syncSuccess = otherServer.syncTransfer(fromUser, toUser, amount);
            if (syncSuccess) {
                logArea.append("Đã đồng bộ giao dịch chuyển tiền tới server thứ nhất.\n");
            } else {
                logArea.append("Đồng bộ giao dịch chuyển tiền tới server thứ nhất thất bại.\n");
            }
        }
        return success;
    }

    private boolean processTransfer(String fromUser, String toUser, double amount) {
        try (Connection conn = DatabaseUtil.getConnection()) {
            conn.setAutoCommit(false);

            // Khóa tài khoản người gửi
            PreparedStatement lockStmt = conn.prepareStatement("SELECT balance FROM users WHERE username = ? FOR UPDATE");
            lockStmt.setString(1, fromUser);
            ResultSet rs = lockStmt.executeQuery();
            if (!rs.next()) {
                logArea.append("Tài khoản người gửi không tồn tại.\n");
                conn.rollback();
                return false;
            }
            double balance = rs.getDouble("balance");
            if (balance < amount) {
                logArea.append("Người dùng " + fromUser + " không đủ số dư.\n");
                conn.rollback();
                return false;
            }

            // Trừ tiền từ người gửi
            PreparedStatement deductStmt = conn.prepareStatement("UPDATE users SET balance = balance - ? WHERE username = ?");
            deductStmt.setDouble(1, amount);
            deductStmt.setString(2, fromUser);
            deductStmt.executeUpdate();

            // Cộng tiền cho người nhận
            PreparedStatement addStmt = conn.prepareStatement("UPDATE users SET balance = balance + ? WHERE username = ?");
            addStmt.setDouble(1, amount);
            addStmt.setString(2, toUser);
            int rowsAffected = addStmt.executeUpdate();
            if (rowsAffected == 0) {
                logArea.append("Tài khoản người nhận không tồn tại.\n");
                conn.rollback();
                return false;
            }

            // Ghi log giao dịch
            recordTransaction(conn, fromUser, "TRANSFER_OUT", amount);
            recordTransaction(conn, toUser, "TRANSFER_IN", amount);

            conn.commit();
            logArea.append("Chuyển tiền thành công.\n");
            return true;
        } catch (SQLException e) {
            logArea.append("Lỗi khi chuyển tiền: " + e.getMessage() + "\n");
            e.printStackTrace();
            return false;
        }
    }

    // Đồng bộ giao dịch chuyển tiền từ server thứ nhất
    @Override
    public boolean syncTransfer(String fromUser, String toUser, double amount) throws RemoteException {
        logArea.append("Đồng bộ chuyển tiền từ " + fromUser + " đến " + toUser + " số tiền: " + amount + "\n");
        return processTransfer(fromUser, toUser, amount);
    }

    // Nạp tiền
    @Override
    public boolean depositMoney(String username, double amount) throws RemoteException {
        logArea.append("Đang nạp tiền vào tài khoản " + username + " số tiền: " + amount + "\n");
        boolean success = processDeposit(username, amount);
        if (success && otherServer != null) {
            // Đồng bộ giao dịch với server thứ nhất
            boolean syncSuccess = otherServer.syncDeposit(username, amount);
            if (syncSuccess) {
                logArea.append("Đã đồng bộ giao dịch nạp tiền tới server thứ nhất.\n");
            } else {
                logArea.append("Đồng bộ giao dịch nạp tiền tới server thứ nhất thất bại.\n");
            }
        }
        return success;
    }

    private boolean processDeposit(String username, double amount) {
        try (Connection conn = DatabaseUtil.getConnection()) {
            PreparedStatement ps = conn.prepareStatement("UPDATE users SET balance = balance + ? WHERE username = ?");
            ps.setDouble(1, amount);
            ps.setString(2, username);
            int rowsAffected = ps.executeUpdate();
            if (rowsAffected == 0) {
                logArea.append("Tài khoản không tồn tại.\n");
                return false;
            }
            // Ghi log giao dịch
            recordTransaction(conn, username, "DEPOSIT", amount);
            logArea.append("Nạp tiền thành công.\n");
            return true;
        } catch (SQLException e) {
            logArea.append("Lỗi khi nạp tiền: " + e.getMessage() + "\n");
            e.printStackTrace();
            return false;
        }
    }

    // Đồng bộ giao dịch nạp tiền từ server thứ nhất
    @Override
    public boolean syncDeposit(String username, double amount) throws RemoteException {
        logArea.append("Đồng bộ nạp tiền vào tài khoản " + username + " số tiền: " + amount + "\n");
        return processDeposit(username, amount);
    }

    // Rút tiền
    @Override
    public boolean withdrawMoney(String username, double amount) throws RemoteException {
        logArea.append("Đang rút tiền từ tài khoản " + username + " số tiền: " + amount + "\n");
        boolean success = processWithdraw(username, amount);
        if (success && otherServer != null) {
            // Đồng bộ giao dịch với server thứ nhất
            boolean syncSuccess = otherServer.syncWithdraw(username, amount);
            if (syncSuccess) {
                logArea.append("Đã đồng bộ giao dịch rút tiền tới server thứ nhất.\n");
            } else {
                logArea.append("Đồng bộ giao dịch rút tiền tới server thứ nhất thất bại.\n");
            }
        }
        return success;
    }

    private boolean processWithdraw(String username, double amount) {
        try (Connection conn = DatabaseUtil.getConnection()) {
            conn.setAutoCommit(false);

            // Khóa tài khoản
            PreparedStatement lockStmt = conn.prepareStatement("SELECT balance FROM users WHERE username = ? FOR UPDATE");
            lockStmt.setString(1, username);
            ResultSet rs = lockStmt.executeQuery();
            if (!rs.next()) {
                logArea.append("Tài khoản không tồn tại.\n");
                conn.rollback();
                return false;
            }
            double balance = rs.getDouble("balance");
            if (balance < amount) {
                logArea.append("Không đủ số dư để rút tiền.\n");
                conn.rollback();
                return false;
            }

            // Trừ tiền từ tài khoản
            PreparedStatement ps = conn.prepareStatement("UPDATE users SET balance = balance - ? WHERE username = ?");
            ps.setDouble(1, amount);
            ps.setString(2, username);
            ps.executeUpdate();

            // Ghi log giao dịch
            recordTransaction(conn, username, "WITHDRAW", amount);

            conn.commit();
            logArea.append("Rút tiền thành công.\n");
            return true;
        } catch (SQLException e) {
            logArea.append("Lỗi khi rút tiền: " + e.getMessage() + "\n");
            e.printStackTrace();
            return false;
        }
    }

    // Đồng bộ giao dịch rút tiền từ server thứ nhất
    @Override
    public boolean syncWithdraw(String username, double amount) throws RemoteException {
        logArea.append("Đồng bộ rút tiền từ tài khoản " + username + " số tiền: " + amount + "\n");
        return processWithdraw(username, amount);
    }

    // Lấy số dư
    @Override
    public double getBalance(String username) throws RemoteException {
        logArea.append("Đang lấy số dư cho tài khoản: " + username + "\n");
        try (Connection conn = DatabaseUtil.getConnection()) {
            PreparedStatement ps = conn.prepareStatement("SELECT balance FROM users WHERE username = ?");
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                double balance = rs.getDouble("balance");
                logArea.append("Số dư của " + username + " là: " + balance + "\n");
                return balance;
            }
        } catch (SQLException e) {
            logArea.append("Lỗi khi lấy số dư: " + e.getMessage() + "\n");
            e.printStackTrace();
        }
        return 0;
    }

    // Lấy lịch sử giao dịch
    @Override
    public List<String> getTransactionHistory(String username) throws RemoteException {
        logArea.append("Đang lấy lịch sử giao dịch cho tài khoản: " + username + "\n");
        List<String> history = new ArrayList<>();
        try (Connection conn = DatabaseUtil.getConnection()) {
            PreparedStatement ps = conn.prepareStatement(
                "SELECT type, amount, date FROM transactions WHERE user_id = (SELECT id FROM users WHERE username = ?) ORDER BY date DESC");
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String transaction = rs.getString("type") + ": " + rs.getDouble("amount") + " on " + rs.getTimestamp("date");
                history.add(transaction);
            }
            logArea.append("Đã lấy xong lịch sử giao dịch.\n");
        } catch (SQLException e) {
            logArea.append("Lỗi khi lấy lịch sử giao dịch: " + e.getMessage() + "\n");
            e.printStackTrace();
        }
        return history;
    }

    // Ghi log giao dịch
    private void recordTransaction(Connection conn, String username, String type, double amount) throws SQLException {
        PreparedStatement ps = conn.prepareStatement(
            "INSERT INTO transactions (user_id, type, amount, date) VALUES ((SELECT id FROM users WHERE username = ?), ?, ?, NOW())");
        ps.setString(1, username);
        ps.setString(2, type);
        ps.setDouble(3, amount);
        ps.executeUpdate();
    }

    public static void main(String[] args) {
        try {
            new ATMServer2();
            System.out.println("ATM Server2 đã được khởi động và đang chờ cấu hình.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
