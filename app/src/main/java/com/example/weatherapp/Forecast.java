package com.example.weatherapp;

public class Forecast {
    private String day;
    private String time;
    private String weatherCondition;
    private String temperature;
    private String icon;


    public Forecast(String day, String time, String weatherCondition, String temperature, String icon) {
        this.day = day;
        this.time = time;
        this.weatherCondition = weatherCondition;
        this.temperature = temperature;
        this.icon = icon;
    }

    public String getDay() {
        return day;
    }

    public void setDay(String day) {
        this.day = day;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getWeatherCondition() {
        return weatherCondition;
    }

    public void setWeatherCondition(String weatherCondition) {
        this.weatherCondition = weatherCondition;
    }

    public String getTemperature() {
        return temperature;
    }

    public void setTemperature(String temperature) {
        this.temperature = temperature;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }
}
