package com.github.rob269;


public class UserAccount extends User {
    private transient final String password;

    public UserAccount(String id, String password) {
        super(id);
        this.password = password;
    }

    public String getPassword() {
        return password;
    }
}
