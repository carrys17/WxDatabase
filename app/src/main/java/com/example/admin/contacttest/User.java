package com.example.admin.contacttest;

/**
 * Created by admin on 2017/10/27.
 */

public class User {
    private String userName;
    private String alias;
    private String nickName;

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getNickName() {
        return nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    @Override
    public String toString() {
        return "userName = "+userName +", alias = "+alias +", nickname = "+nickName;
    }
}
