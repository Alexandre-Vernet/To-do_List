package com.ynov.vernet.to_dolist;

import java.util.Date;

public class Task {
    private String id, description, creator;
    private Date date;

    public Task(String id, String description, String creator, Date date) {
        this.id = id;
        this.description = description;
        this.creator = creator;
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
        return creator;
    }

    public void setName(String creator) {
        this.creator = creator;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }
}
