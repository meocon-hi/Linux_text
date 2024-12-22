package comon;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface ATMRemote extends Remote {
    boolean authenticateUser(String username, String password) throws RemoteException;
    void syncLogin(String username) throws RemoteException;
    boolean logoutUser(String username) throws RemoteException;
    void syncLogout(String username) throws RemoteException;
    boolean transferMoney(String fromUser, String toUser, double amount) throws RemoteException;
    boolean depositMoney(String username, double amount) throws RemoteException;
    boolean withdrawMoney(String username, double amount) throws RemoteException;
    double getBalance(String username) throws RemoteException;
    List<String> getTransactionHistory(String username) throws RemoteException;

    // Phương thức đồng bộ hóa dữ liệu giữa các server
    boolean syncTransfer(String fromUser, String toUser, double amount) throws RemoteException;
    boolean syncDeposit(String username, double amount) throws RemoteException;
    boolean syncWithdraw(String username, double amount) throws RemoteException;
}
