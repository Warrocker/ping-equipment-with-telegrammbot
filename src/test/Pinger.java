package test;

import org.telegram.telegrambots.TelegramApiException;
import org.telegram.telegrambots.TelegramBotsApi;
import org.telegram.telegrambots.api.methods.SendMessage;
import org.telegram.telegrambots.api.objects.Message;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
/*
 Написано одним классом для простоты копирования и наглядности
 */
public class Pinger implements Runnable {
    // Массив с адрессами хостов
    private static ArrayList<String> addresses = new ArrayList<>();
    // Мапа с адресс/IP
    private static HashMap<String, String> addressIp =  new HashMap<>();
    // Список недоступных хостов
    private static ArrayList<String> unreachableHosts = new ArrayList<>();
    private Pinger(){
        Thread t = new Thread(this, "NewThread");
        t.start();
    }
    private static Connection getDBConnection(){
        Connection dbConnection = null;
        try {
            //Ищем драйвер
            Class.forName("com.mysql.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.out.println(e.getMessage());
        }
        try {
            //Подключаемся к базе драйвер:база:адресс:порт(стандартный порт 3306)/имя_базы?useUnicode=true&characterEncoding=utf-8, логин, пароль
            dbConnection = DriverManager.getConnection("<Your_database>");

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return dbConnection;
    }
    private static void selectFromDB(){
        // Формируем запрос к базе где хранятся наши IP адреса и физические адреса
        String selectTableSQL = "<Command>";
        Connection dbConnection = null;
        Statement statement;
        try {
            // Получаем соеденение
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
                //Ежели есть соеденение закрываем его
                if (dbConnection != null) {
                    dbConnection.close();
                }
            }catch (SQLException e){
                System.out.println("Cant close");
            }
        }
    }
    public static void main(String[] args) throws  IOException {
        // Инициализируем телеграмм бота и регистрируем его
        TelegramBotsApi telegramBotsApi = new TelegramBotsApi();
        try {
            telegramBotsApi.registerBot(new SimpleBot());
        } catch (TelegramApiException e) {
            // e.printStackTrace();
        }
        // Создаем новый поток для подключения к базе и пинга на хосты
        new Pinger();


    }

    @SuppressWarnings("InfiniteLoopStatement")
    @Override
    public void run() {
        // Инициализируем бота
        SimpleBot sb = new SimpleBot();
        // Мною сделан бесконечный цикл ибо
        while (true){
            try {
                selectFromDB();
            }catch (Exception e){
                try {
                    //Ждем минуту и пробуем переподключиться
                    Thread.sleep(60000);
                    selectFromDB();
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
            }
            // маркер, ежели меняется на false значит не все хорошо с хостами
            boolean marker = true;
            for (String addressFromArray : addresses) {
                String ipAddress = addressIp.get(addressFromArray);
                InetAddress inet = null;
                String hostMessage;
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
                    if (inet != null && inet.isReachable(5000)) {
                        //Если наш хост до этого падал, пишем что он поднялся
                        if(unreachableHosts.contains(addressFromArray)){
                            unreachableHosts.remove(addressFromArray);
                            System.out.println(unreachableHosts.size());
                            hostMessage = "Host " + addressFromArray + " is UP\n";
                            SendMessage sendMessage = new SendMessage();
                            sendMessage.enableMarkdown(true);
                            /*
                        ID чата в котором будем это слать получается методом message.getChatId().toString()
                         чуть ниже видно просто выводите результат метода в чат и получаете ID
                         */
                            sendMessage.setChatId("<ChatId>");
                            sendMessage.setText(hostMessage);
                            try {
                                // шлем в телеграммстер
                                sb.sendMessage(sendMessage);
                            } catch (TelegramApiException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    else {
                        // Если уже известно что хост недоступен сообщение об этом не пишем
                        if (!unreachableHosts.contains(addressFromArray)) {
                            unreachableHosts.add(addressFromArray);
                            marker = false;
                            // Генерируем сообщение о недоступности
                            hostMessage = "Host " + addressFromArray + " is NOT reachable\n";
                            SendMessage sendMessage = new SendMessage();
                            sendMessage.enableMarkdown(true);
                        /*
                        ID чата в котором будем это слать получается методом message.getChatId().toString()
                         чуть ниже видно просто выводите результат метода в чат и получаете ID
                         */
                            sendMessage.setChatId("<ChatId>");
                            sendMessage.setText(hostMessage);
                            try {
                                // шлем в телеграммстер
                                sb.sendMessage(sendMessage);
                            } catch (TelegramApiException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if(marker) {
                //Все хорошо
                System.out.println("All is UP");
            }
            try {
                // Повторяем цикл каждые 5 секунд
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                System.out.println("Interrupted");
            }

        }
    }
}
// Класс телеграмм бота
class SimpleBot extends TelegramLongPollingBot{
    //Сюда имя бота
    @Override
    public String getBotUsername() {
        return "<botname>";
    }
    //Сюда токен бота
    @Override
    public String getBotToken() {
        return "<TOKEN>";
    }
    // Обработчик
    @Override
    public void onUpdateReceived(Update update) {
        Message message = update.getMessage();
        if (message != null && message.hasText()) {
            if (message.getText().contains("/help"))
                sendMsg(message, "Я работаю, все хорошо");
            else
                sendMsg(message, "Я не знаю что ответить на это");
        }
    }
    // Метод для переписки с ботом
    private void sendMsg(Message message, String text) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(true);
        sendMessage.setChatId(message.getChatId().toString());
        sendMessage.setReplayToMessageId(message.getMessageId());
        sendMessage.setText(text);
        try {
            sendMessage(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}