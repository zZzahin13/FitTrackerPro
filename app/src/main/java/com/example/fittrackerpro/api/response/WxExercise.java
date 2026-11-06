package com.example.fittrackerpro.api.response;

public class WxExercise {
    public String id;
    public String name;
    public String model;           // "male" or "female"
    public String counterpartId;   // the same movement with the other model
    public WxMedia media;          // null when the media limit is reached
}