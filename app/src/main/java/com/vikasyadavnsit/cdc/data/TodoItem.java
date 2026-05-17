package com.vikasyadavnsit.cdc.data;

import java.util.UUID;

public class TodoItem {
    public String id;
    public String title;
    public boolean done;
    public long createdAt;
    public long updatedAt;

    public TodoItem() {}

    public TodoItem(String title) {
        this.id = UUID.randomUUID().toString();
        this.title = title.trim();
        this.done = false;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }
}
