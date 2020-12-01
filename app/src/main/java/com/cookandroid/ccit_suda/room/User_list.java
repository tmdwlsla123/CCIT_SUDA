package com.cookandroid.ccit_suda.room;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.io.Serializable;

@Entity(tableName = "user_list")
public class User_list implements Serializable {
    @PrimaryKey(autoGenerate = true)
    private Integer F_idx;
    @ColumnInfo( name =  "user_name")
    private String user_name;

    public User_list(Integer F_idx, String user_name) {
        F_idx = F_idx;
        this.user_name = user_name;
    }

    public Integer getF_idx() {
        return F_idx;
    }

    public void setF_idx(Integer F_idx) {
        F_idx = F_idx;
    }

    public String getUser_name() {
        return user_name;
    }

    public void setUser_name(String user_name) {
        this.user_name = user_name;
    }




}