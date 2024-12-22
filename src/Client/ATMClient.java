package Client;

import comon.ATMRemote;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.rmi.Naming;
import java.util.List;

public class ATMClient {
    private ATMRemote atmRemote;
    private String username;
    private double balance; // Thêm biến để lưu trữ số dư

    private JFrame connectionFrame;
    private JFrame loginFrame;
    private JFrame transactionFrame;

    private JLabel balanceLabel; // Hiển thị số dư
    private JLabel usernameLabel; // Hiển thị tên tài khoản

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ATMClient().showConnectionUI());
    }

    // Hiển thị giao diện kết nối server
    private void showConnectionUI() {
        connectionFrame = new JFrame("ATM Client - Kết nối Server");
        connectionFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        connectionFrame.setSize(400, 200);
        connectionFrame.setLayout(new GridBagLayout());
        connectionFrame.setLocationRelativeTo(null);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel ipLabel = new JLabel("Server IP:");
        gbc.gridx = 0;
        gbc.gridy = 0;
        connectionFrame.add(ipLabel, gbc);

        JTextField ipField = new JTextField("localhost");
        gbc.gridx = 1;
        gbc.gridy = 0;
        connectionFrame.add(ipField, gbc);

        JLabel portLabel = new JLabel("Server Port:");
        gbc.gridx = 0;
        gbc.gridy = 1;
        connectionFrame.add(portLabel, gbc);

        JTextField portField = new JTextField("1238");
        gbc.gridx = 1;
        gbc.gridy = 1;
        connectionFrame.add(portField, gbc);

        JButton connectButton = new JButton("Connect");
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        connectionFrame.add(connectButton, gbc);

        connectButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String serverIP = ipField.getText().trim();
                String portText = portField.getText().trim();
                if (serverIP.isEmpty() || portText.isEmpty()) {
                    JOptionPane.showMessageDialog(connectionFrame, "Vui lòng nhập đầy đủ IP và Port.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                int serverPort;
                try {
                    serverPort = Integer.parseInt(portText);
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(connectionFrame, "Port phải là số.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                try {
                    String url = "rmi://" + serverIP + ":" + serverPort + "/ATMServer";
                    atmRemote = (ATMRemote) Naming.lookup(url);
                    JOptionPane.showMessageDialog(connectionFrame, "Kết nối thành công tới server!");
                    connectionFrame.dispose();
                    showLoginUI();
                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(connectionFrame, "Không thể kết nối tới server: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        connectionFrame.setVisible(true);
    }

    // Hiển thị giao diện đăng nhập
    private void showLoginUI() {
        loginFrame = new JFrame("ATM Client - Đăng nhập");
        loginFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        loginFrame.setSize(500, 300);
        loginFrame.setLayout(new GridBagLayout());
        loginFrame.setLocationRelativeTo(null);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel userLabel = new JLabel("Tên đăng nhập:");
        gbc.gridx = 0;
        gbc.gridy = 0;
        loginFrame.add(userLabel, gbc);

        JTextField userField = new JTextField(20);
        gbc.gridx = 1;
        gbc.gridy = 0;
        loginFrame.add(userField, gbc);

        JLabel passLabel = new JLabel("Mật khẩu:");
        gbc.gridx = 0;
        gbc.gridy = 1;
        loginFrame.add(passLabel, gbc);

        JPasswordField passField = new JPasswordField(20);
        gbc.gridx = 1;
        gbc.gridy = 1;
        loginFrame.add(passField, gbc);

        JButton loginButton = new JButton("Đăng nhập");
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        loginFrame.add(loginButton, gbc);

        loginButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String uname = userField.getText().trim();
                String pwd = new String(passField.getPassword()).trim();
                if (uname.isEmpty() || pwd.isEmpty()) {
                    JOptionPane.showMessageDialog(loginFrame, "Vui lòng nhập đầy đủ tên đăng nhập và mật khẩu.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                try {
                    if (atmRemote.authenticateUser(uname, pwd)) {
                        JOptionPane.showMessageDialog(loginFrame, "Đăng nhập thành công!");
                        username = uname;
                        balance = atmRemote.getBalance(username); // Lấy số dư sau khi đăng nhập thành công
                        loginFrame.dispose();
                        showTransactionUI();
                    } else {
                        JOptionPane.showMessageDialog(loginFrame, "Đăng nhập thất bại! Kiểm tra lại thông tin.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(loginFrame, "Lỗi khi đăng nhập: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        loginFrame.setVisible(true);
    }

    // Hiển thị giao diện giao dịch
    private void showTransactionUI() {
        transactionFrame = new JFrame("ATM Client - Giao dịch");
        transactionFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        transactionFrame.setSize(600, 400);
        transactionFrame.setLayout(new BorderLayout());
        transactionFrame.setLocationRelativeTo(null);

        // Panel chứa thông tin người dùng
        JPanel userInfoPanel = new JPanel();
        userInfoPanel.setLayout(new GridLayout(2, 1));
        userInfoPanel.setBorder(BorderFactory.createTitledBorder("Thông tin người dùng"));

        usernameLabel = new JLabel("Tài khoản: " + username);
        balanceLabel = new JLabel("Số dư: " + balance + " VND");

        userInfoPanel.add(usernameLabel);
        userInfoPanel.add(balanceLabel);

        transactionFrame.add(userInfoPanel, BorderLayout.NORTH);

        // Button Panel
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridLayout(5, 1, 10, 10));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JButton depositButton = new JButton("Nạp tiền");
        JButton withdrawButton = new JButton("Rút tiền");
        JButton transferButton = new JButton("Chuyển tiền");
        JButton historyButton = new JButton("Lịch sử giao dịch");

     // Example button to log out
        JButton logoutButton = new JButton("Đăng xuất");
        buttonPanel.add(logoutButton);
        logoutButton.addActionListener(e -> {
            try {
                atmRemote.logoutUser(username); // Call logout on server
                JOptionPane.showMessageDialog(transactionFrame, "Đã đăng xuất thành công.");
                transactionFrame.dispose();
                showLoginUI(); // Show login screen again
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(transactionFrame, "Lỗi khi đăng xuất: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        });

        buttonPanel.add(depositButton);
        buttonPanel.add(withdrawButton);
        buttonPanel.add(transferButton);
        buttonPanel.add(historyButton);

        transactionFrame.add(buttonPanel, BorderLayout.CENTER);

        // Nút nạp tiền
        depositButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String amountStr = JOptionPane.showInputDialog(transactionFrame, "Nhập số tiền cần nạp:");
                if (amountStr != null) {
                    try {
                        double amount = Double.parseDouble(amountStr);
                        if (atmRemote.depositMoney(username, amount)) {
                            balance += amount;
                            balanceLabel.setText("Số dư: " + balance + " VND");
                            JOptionPane.showMessageDialog(transactionFrame, "Nạp tiền thành công: " + amount + " VND");
                        } else {
                            JOptionPane.showMessageDialog(transactionFrame, "Nạp tiền thất bại.");
                        }
                    } catch (NumberFormatException ex) {
                        JOptionPane.showMessageDialog(transactionFrame, "Vui lòng nhập số tiền hợp lệ.");
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        JOptionPane.showMessageDialog(transactionFrame, "Lỗi khi nạp tiền: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        });

        // Nút rút tiền
        withdrawButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String amountStr = JOptionPane.showInputDialog(transactionFrame, "Nhập số tiền cần rút:");
                if (amountStr != null) {
                    try {
                        double amount = Double.parseDouble(amountStr);
                        if (atmRemote.withdrawMoney(username, amount)) {
                            balance -= amount;
                            balanceLabel.setText("Số dư: " + balance + " VND");
                            JOptionPane.showMessageDialog(transactionFrame, "Rút tiền thành công: " + amount + " VND");
                        } else {
                            JOptionPane.showMessageDialog(transactionFrame, "Rút tiền thất bại.");
                        }
                    } catch (NumberFormatException ex) {
                        JOptionPane.showMessageDialog(transactionFrame, "Vui lòng nhập số tiền hợp lệ.");
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        JOptionPane.showMessageDialog(transactionFrame, "Lỗi khi rút tiền: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        });

        // Nút chuyển tiền
        transferButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String recipient = JOptionPane.showInputDialog(transactionFrame, "Nhập tài khoản người nhận:");
                if (recipient != null) {
                    String amountStr = JOptionPane.showInputDialog(transactionFrame, "Nhập số tiền cần chuyển:");
                    if (amountStr != null) {
                        try {
                            double amount = Double.parseDouble(amountStr);
                            if (atmRemote.transferMoney(username, recipient, amount)) {
                                balance -= amount;
                                balanceLabel.setText("Số dư: " + balance + " VND");
                                JOptionPane.showMessageDialog(transactionFrame, "Chuyển tiền thành công: " + amount + " VND đến " + recipient);
                            } else {
                                JOptionPane.showMessageDialog(transactionFrame, "Chuyển tiền thất bại.");
                            }
                        } catch (NumberFormatException ex) {
                            JOptionPane.showMessageDialog(transactionFrame, "Vui lòng nhập số tiền hợp lệ.");
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            JOptionPane.showMessageDialog(transactionFrame, "Lỗi khi chuyển tiền: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                }
            }
        });

        // Nút lịch sử giao dịch
        historyButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    List<String> history = atmRemote.getTransactionHistory(username);
                    StringBuilder sb = new StringBuilder();
                    for (String record : history) {
                        sb.append(record).append("\n");
                    }
                    JOptionPane.showMessageDialog(transactionFrame, sb.toString(), "Lịch sử giao dịch", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(transactionFrame, "Lỗi khi lấy lịch sử giao dịch: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        transactionFrame.setVisible(true);
    }
}
