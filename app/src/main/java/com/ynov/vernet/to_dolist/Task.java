package com.ynov.vernet.to_dolist;

import java.util.Date;

public class Task {
    private String id, description, name;
    private Date date;

    public Task(String id, String description, String name, Date date) {
        this.id = id;
        this.description = description;
        this.name = name;
        this.date = date;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }
}
