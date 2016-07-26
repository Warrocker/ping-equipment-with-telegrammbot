package test;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;

public class Main implements Runnable {
    private static ArrayList<String> addresses = new ArrayList<>();
    private static HashMap<String, String> addressIp =  new HashMap<>();
    private Main(){
    Thread t = new Thread(this, "NewThread");
        t.start();
    }
    private static Connection getDBConnection(){
        Connection dbConnection = null;
        try {
            Class.forName("com.mysql.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.out.println(e.getMessage());
        }
        try {
            dbConnection = DriverManager.getConnection("jdbc:mysql://91.192.217.210:3306/nag?useUnicode=true&characterEncoding=utf-8", "root","realnet_f");

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return dbConnection;
    }
    private static void selectFromDB(){
        String selectTableSQL = "SELECT alias AS 'address', address AS 'ip' FROM `Kievcfg` WHERE address NOT LIKE '%#' ORDER BY alias ASC";
        Connection dbConnection = null;
        Statement statement;
        try {
            dbConnection = getDBConnection();
            statement = dbConnection.createStatement();

            // выбираем данные с БД
            ResultSet rs = statement.executeQuery(selectTableSQL);

            // И если что то было получено то цикл while сработает
            while (rs.next()) {
                String address = rs.getString("address");
                String ip = rs.getString("ip");
                addresses.add(address);
                addressIp.put(address, ip);
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }finally {
            try {
                if (dbConnection != null) {
                    dbConnection.close();
                }
            }catch (SQLException e){
                System.out.println("Cant close");
            }
        }
    }
    public static void main(String[] args) throws  IOException {
        new Main();
    }

    @SuppressWarnings("InfiniteLoopStatement")
    @Override

    public void run() {
        while (true){
            selectFromDB();
            for (String addressFromArray : addresses) {
                String ipAddress = addressIp.get(addressFromArray);
                InetAddress inet = null;
                try {
                    inet = InetAddress.getByName(ipAddress);
                } catch (UnknownHostException e) {
                    e.printStackTrace();

                }
                try {
                    inet = InetAddress.getByName(ipAddress);
                } catch (UnknownHostException e) {
                    System.out.println("incorrect hostname");
                }
                try {
                    System.out.print((inet != null && inet.isReachable(5000)) ? "" : "Host" + addressFromArray + "is NOT reachable\n");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            System.out.println("All hosts UP");
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                System.out.println("Interrupted");
            }

        }
    }
}
