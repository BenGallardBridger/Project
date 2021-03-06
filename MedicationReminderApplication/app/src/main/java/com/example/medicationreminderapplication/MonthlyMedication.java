package com.example.medicationreminderapplication;
import java.util.Map;

public class MonthlyMedication extends Medication {
    public int dayOfMonth;

    public MonthlyMedication(String Name, String strength, int NumLeft, String type, Boolean WithFood, int dayOfMonth, Map<String, Boolean> PrevTakenAt) {
        super(Name, strength, NumLeft, type, WithFood, PrevTakenAt);
        this.dayOfMonth = dayOfMonth;
    }

    public MonthlyMedication(String Name, String strength, int NumLeft, String type, Boolean WithFood, int dayOfMonth) {
        super(Name, strength, NumLeft, type, WithFood);
        this.dayOfMonth = dayOfMonth;
    }
}
