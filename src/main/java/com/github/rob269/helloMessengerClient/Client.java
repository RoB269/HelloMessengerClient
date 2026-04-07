package com.github.rob269.helloMessengerClient;

public class Client {
    private static boolean isLogin = false;
    private static User user = null;

    public static void login(String username, String password) {
        user = new User(username, password);
        isLogin = true;
    }

    public static boolean isLogin() {
        return isLogin;
    }


    public static String getPassword() {
        return user.getPassword();
    }

    public static String getUsername() {
        return user.getUsername();
    }
}
