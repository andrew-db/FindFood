package com.krakenjaws.findfood;

import android.app.Application;

import com.krakenjaws.findfood.models.User;

/**
 * Created by Andrew on 5/8/2019.
 */
public class UserClient extends Application {

    private User user = null;

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
