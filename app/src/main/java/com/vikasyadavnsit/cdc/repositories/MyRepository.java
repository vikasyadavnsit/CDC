package com.vikasyadavnsit.cdc.repositories;

import android.util.Log;

import javax.inject.Inject;


public class MyRepository {

    @Inject
    public MyRepository() {
        // Repository initialization
    }

    public void doSomething() {
        Log.d("MyRepository", "doSomething");
    }
}

