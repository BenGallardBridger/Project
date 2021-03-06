package com.example.medicationreminderapplication;
import android.content.Context;
import android.security.keystore.KeyGenParameterSpec;
import androidx.security.crypto.EncryptedFile;
import androidx.security.crypto.MasterKeys;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

public class DataController {

    private static RequestQueue reqQueue;
    private static DataController instance = null;
    static ArrayList<Medication> MedicationList;
    private LocalDateTime nextMedDateTime;
    private ArrayList<Medication> nextMedList;
    Context context;

//Creates an instance of the Data controller and starts data reading
    public static DataController getInstance(Context context, RequestQueue requestQueue){
        if (instance == null){
            instance = new DataController(context,requestQueue);
        }
        return instance;
    }

    public static DataController getInstance() {
        return instance;
    }

    private DataController(Context context, RequestQueue requestQueue){
        this.context = context;
        //Instantiate Medication list
        MedicationList = new ArrayList<Medication>();
        nextMedDateTime = LocalDateTime.MAX;
        nextMedList = new ArrayList<>();
        //Instantiate request queue
        reqQueue = requestQueue;
        reqQueue.start();
        //Collect Data
        CollectData();
    }
//Reads Data from the file and decrypts it
    void CollectData(){
        KeyGenParameterSpec keyGenParameterSpec = MasterKeys.AES256_GCM_SPEC;
        String mainKeyAlias = null;
        try {
            mainKeyAlias = MasterKeys.getOrCreate(keyGenParameterSpec);
            String fileToRead = "MedicationInfo.txt";
            EncryptedFile encryptedFile = new EncryptedFile.Builder(
                    new File(context.getFilesDir(), fileToRead),
                    context,
                    mainKeyAlias,
                    EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
            ).build();

            InputStream inputStream = encryptedFile.openFileInput();
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            int nextByte = inputStream.read();
            while (nextByte != -1) {
                byteArrayOutputStream.write(nextByte);
                nextByte = inputStream.read();
            }

            byte[] plaintext = byteArrayOutputStream.toByteArray();
            String fileContent = new String(plaintext, StandardCharsets.UTF_8);
            Log.e("FIF", fileContent);
            String[] lines = fileContent.split("\n");
            int currentMedType = 0;
            for (String line: lines
                 ) {
                switch (line) {
                    case "EveryXDay":
                        currentMedType = 0;
                        break;
                    case "SpecificDay":
                        currentMedType = 1;
                        break;
                    case "Weekly":
                        currentMedType = 2;
                        break;
                    case "Monthly":
                        currentMedType = 3;
                        break;
                    case "NEXTMEDS":
                        currentMedType = -1;
                        break;
                    default:
                        switch (currentMedType) {
                            case -1: {
                                String[] meds = line.split("/");
                                nextMedList.clear();
                                for (String medCombo : meds
                                ) {
                                    int index = medIndex(medCombo);
                                    if (index != -1) {
                                        nextMedList.add(MedicationList.get(index));
                                    }
                                }
                                break;
                            }
                            case 0: {
                                String[] parts = line.split("/");
                                String Name = parts[0];
                                String strength = parts[1];
                                int NumLeft = Integer.parseInt(parts[2]);
                                String type = parts[3];
                                Boolean WithFood;
                                if (parts[4].equals("0")) {
                                    WithFood = Boolean.FALSE;
                                } else {
                                    WithFood = Boolean.TRUE;
                                }
                                String takenAtTime = parts[5].substring(1, parts[5].length() - 1);
                                String[] takenAtTemp = takenAtTime.split(", ");
                                ArrayList<LocalTime> TakenAt = new ArrayList<>();
                                for (String time : takenAtTemp
                                ) {
                                    if (time.length() != 0) {
                                        TakenAt.add(LocalTime.parse(time));
                                    }
                                }
                                int numOfDays = Integer.parseInt(parts[6]);
                                LocalDate StartDate = LocalDate.parse(parts[7]);
                                EveryXDaysMedication med;
                                if (parts.length == 9) {
                                    HashMap<String, Boolean> prevTakenAt = new HashMap<>();
                                    String[] keyValuePairs = parts[8].substring(1, parts[8].length() - 1).split(",");
                                    for (String keyValuePair : keyValuePairs
                                    ) {
                                        String[] pair = keyValuePair.split("=");
                                        prevTakenAt.put(pair[0].replace(" ", ""), Boolean.valueOf(pair[1]));
                                    }

                                    med = new EveryXDaysMedication(Name, strength, NumLeft, type, WithFood, TakenAt, numOfDays, StartDate, prevTakenAt);
                                } else {
                                    med = new EveryXDaysMedication(Name, strength, NumLeft, type, WithFood, TakenAt, numOfDays, StartDate);
                                }
                                newMedFromFile(med);
                                break;
                            }
                            case 1: {
                                String[] parts = line.split("/");
                                String Name = parts[0];
                                String strength = parts[1];
                                int NumLeft = Integer.parseInt(parts[2]);
                                String type = parts[3];
                                Boolean WithFood;
                                if (parts[4] == "0") {
                                    WithFood = Boolean.FALSE;
                                } else {
                                    WithFood = Boolean.TRUE;
                                }
                                String takenAtTime = parts[5].substring(2, parts[5].length() - 2);
                                String[] takenAtTemp = takenAtTime.split("], \\[");
                                ArrayList<ArrayList<LocalTime>> TakenAt = new ArrayList<>();
                                for (String times : takenAtTemp
                                ) {
                                    String[] tempTimes = times.split(", ");
                                    ArrayList<LocalTime> tempLocalTimes = new ArrayList<>();
                                    for (String time : tempTimes
                                    ) {
                                        tempLocalTimes.add(LocalTime.parse(time));
                                    }
                                    TakenAt.add(tempLocalTimes);
                                }
                                SpecificDayMedication med;
                                if (parts.length == 7) {
                                    HashMap<String, Boolean> prevTakenAt = new HashMap<>();
                                    String[] keyValuePairs = parts[6].substring(1, parts[6].length() - 1).split(",");
                                    for (String keyValuePair : keyValuePairs
                                    ) {
                                        String[] pair = keyValuePair.split("=");
                                        prevTakenAt.put(pair[0].replace(" ", ""), Boolean.valueOf(pair[1]));
                                    }
                                    med = new SpecificDayMedication(Name, strength, NumLeft, type, WithFood, TakenAt, prevTakenAt);
                                } else {
                                    med = new SpecificDayMedication(Name, strength, NumLeft, type, WithFood, TakenAt);
                                }
                                newMedFromFile(med);
                                break;
                            }
                            case 2: {
                                String[] parts = line.split("/");
                                String Name = parts[0];
                                String strength = parts[1];
                                int NumLeft = Integer.parseInt(parts[2]);
                                String type = parts[3];
                                Boolean WithFood;
                                if (parts[4] == "0") {
                                    WithFood = Boolean.FALSE;
                                } else {
                                    WithFood = Boolean.TRUE;
                                }
                                String takenAtTime = parts[5].substring(1, parts[5].length() - 1);
                                String[] takenAtTemp = takenAtTime.split(", ");
                                ArrayList<LocalTime> TakenAt = new ArrayList<>();
                                for (String time : takenAtTemp
                                ) {
                                    TakenAt.add(LocalTime.parse(time));
                                }
                                String Day = parts[6];
                                WeeklyMedication med;
                                if (parts.length == 8) {
                                    HashMap<String, Boolean> prevTakenAt = new HashMap<>();
                                    String[] keyValuePairs = parts[7].substring(1, parts[7].length() - 1).split(",");
                                    for (String keyValuePair : keyValuePairs
                                    ) {
                                        String[] pair = keyValuePair.split("=");
                                        prevTakenAt.put(pair[0].replace(" ", ""), Boolean.valueOf(pair[1]));
                                    }
                                    med = new WeeklyMedication(Name, strength, NumLeft, type, WithFood, TakenAt, Day, prevTakenAt);
                                } else {
                                    med = new WeeklyMedication(Name, strength, NumLeft, type, WithFood, TakenAt, Day);
                                }
                                newMedFromFile(med);
                                break;
                            }
                            case 3: {
                                String[] parts = line.split("/");
                                String Name = parts[0];
                                String strength = parts[1];
                                int NumLeft = Integer.parseInt(parts[2]);
                                String type = parts[3];
                                Boolean WithFood;
                                if (parts[4] == "0") {
                                    WithFood = Boolean.FALSE;
                                } else {
                                    WithFood = Boolean.TRUE;
                                }
                                int dayOfMonth = Integer.parseInt(parts[5]);
                                MonthlyMedication med;
                                if (parts.length == 8) {
                                    HashMap<String, Boolean> prevTakenAt = new HashMap<>();
                                    String[] keyValuePairs = parts[7].substring(1, parts[7].length() - 1).split(",");
                                    for (String keyValuePair : keyValuePairs
                                    ) {
                                        String[] pair = keyValuePair.split("=");
                                        prevTakenAt.put(pair[0].replace(" ", ""), Boolean.valueOf(pair[1]));
                                    }
                                    med = new MonthlyMedication(Name, strength, NumLeft, type, WithFood, dayOfMonth, prevTakenAt);
                                } else {
                                    med = new MonthlyMedication(Name, strength, NumLeft, type, WithFood, dayOfMonth);
                                }
                                newMedFromFile(med);
                                break;
                            }
                        }
                        break;
                }
            }
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
//Checks for which medications are meant to be taken next
    void NextMeds(){
        //Get current time
        LocalDateTime current = LocalDateTime.now().plusMinutes(1);
        //Check Daily Medications
        LocalDate nextDate = LocalDate.MAX;
        ArrayList<Medication> nextMonthlyMeds = new ArrayList<>();
        LocalDateTime nextDateTime = LocalDateTime.MAX;
        ArrayList<Medication> nextMeds = new ArrayList<>();
        for (Medication med: MedicationList
             ) {
            if (med instanceof EveryXDaysMedication){
                LocalDate previousDate = ((EveryXDaysMedication) med).startDate;
                ArrayList<LocalTime> previousTimes = ((EveryXDaysMedication) med).times;
                for (LocalTime time: previousTimes
                     ) {
                    LocalDateTime previousDateTime = previousDate.atTime(time);
                    while (previousDateTime.isBefore(current)){
                        previousDateTime = previousDateTime.plusDays(((EveryXDaysMedication) med).numberOfDays);
                    }
                    Log.e(previousDateTime.toString(), "");
                    if (previousDateTime.toLocalDate().isBefore(nextDate)){
                        nextMeds.clear();
                        nextMonthlyMeds.clear();
                        nextMeds.add(med);
                        nextDateTime = previousDateTime;
                        nextDate = previousDateTime.toLocalDate();
                    }
                    else if (nextDateTime.isAfter(previousDateTime)){
                        nextDateTime = previousDateTime;
                        nextMeds.clear();
                        nextMeds.add(med);
                    }
                    else if (nextDateTime.isEqual(previousDateTime)){
                        nextMeds.add(med);
                    }
                }

            }
            else if (med instanceof SpecificDayMedication){
                ArrayList<ArrayList<LocalTime>> DayToTimes = ((SpecificDayMedication) med).Times;
                DayOfWeek now = current.getDayOfWeek();
                int Limit = now.getValue()-1;
                for (int i = 0; i < 7; i++) {
                    int currentInd = i+Limit;
                    if (currentInd > 6){
                        currentInd -= 7;
                    }
                    ArrayList<LocalTime> Times = DayToTimes.get(currentInd);
                    for (LocalTime time: Times
                         ) {
                        LocalDate tempDate = current.plusDays(currentInd).toLocalDate();
                        LocalDateTime tempDateTime = tempDate.atTime(time);
                        if (tempDateTime.toLocalDate().isBefore(nextDate) && tempDateTime.isAfter(current)){
                            nextMeds.clear();
                            nextMonthlyMeds.clear();
                            nextMeds.add(med);
                            nextDateTime = tempDateTime;
                            nextDate = tempDateTime.toLocalDate();
                        }
                        else if (tempDateTime.isBefore(nextDateTime) && tempDateTime.isAfter(current)){
                            nextMeds.clear();
                            nextMeds.add(med);
                            nextDateTime = tempDateTime;
                        }
                        else if (tempDateTime.isEqual(nextDateTime)){
                            nextMeds.add(med);
                        }
                    }
                }
            }
            else if (med instanceof WeeklyMedication){
                String day = ((WeeklyMedication) med).Day;
                ArrayList<LocalTime> times = ((WeeklyMedication) med).times;
                DayOfWeek dayOfWeek = DayOfWeek.valueOf(day.toUpperCase(Locale.ROOT));

                DayOfWeek now = current.getDayOfWeek();

                int daysUntil;
                if (dayOfWeek.getValue()> now.getValue()){
                    daysUntil = dayOfWeek.getValue()- now.getValue();
                }
                else{
                    daysUntil = 7-(now.getValue()-dayOfWeek.getValue());
                }

                LocalDate tempDate = current.toLocalDate();
                for (LocalTime time: times
                     ) {
                    LocalDateTime tempDateTime = tempDate.atTime(time);

                    if (tempDateTime.toLocalDate().isBefore(nextDate)){
                        nextMeds.clear();
                        nextMonthlyMeds.clear();
                        nextMeds.add(med);
                        nextDateTime = tempDateTime;
                        nextDate = tempDateTime.toLocalDate();
                    }
                    else if (tempDateTime.isBefore(nextDateTime)){
                        nextMeds.clear();
                        nextMeds.add(med);
                        nextDateTime = tempDateTime;
                    }
                    else if (tempDateTime.isEqual(nextDateTime)){
                        nextMeds.add(med);
                    }
                }
            }
            else if (med instanceof MonthlyMedication){
                int dayOfMonth = ((MonthlyMedication) med).dayOfMonth;
                LocalDate currentDate = current.toLocalDate();
                int lastDayOfMonth = currentDate.lengthOfMonth();
                if (dayOfMonth > lastDayOfMonth){
                    dayOfMonth = lastDayOfMonth;
                }
                if (currentDate.getDayOfMonth() < dayOfMonth){
                    LocalDate tempDate = LocalDate.of(currentDate.getYear(), currentDate.getMonthValue(), dayOfMonth);
                    if (tempDate.isBefore(nextDate)){
                        nextMonthlyMeds.clear();
                        nextMonthlyMeds.add(med);
                        nextDate = tempDate;
                        nextDateTime = tempDate.atTime(LocalTime.NOON);
                    }

                }
            }
        }
        nextMeds.addAll(nextMonthlyMeds);
        nextMedList=nextMeds;
        nextMedDateTime = nextDateTime;
        Log.e("nextMeds", nextMedList.toString());
        writeToFile();
    }

    public ArrayList<Medication> getNextMedList() {
        return nextMedList;
    }

    public LocalDateTime getNextMedDateTime() {
        return nextMedDateTime;
    }

//Write Back To File
    public void writeToFile(){
        KeyGenParameterSpec keyGenParameterSpec = MasterKeys.AES256_GCM_SPEC;
        try {
            String mainKeyAlis = MasterKeys.getOrCreate(keyGenParameterSpec);
            String fileToWrite = "MedicationInfo.txt";
            File file = new File(context.getFilesDir(), fileToWrite);
            if (file.exists()){
                file.delete();
            }
            EncryptedFile encryptedFile = new EncryptedFile.Builder(
                    new File(context.getFilesDir(),fileToWrite),
                    context,
                    mainKeyAlis,
                    EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB).build();
            String fileString = "";
            for (Medication med: MedicationList
                 ) {
                if (med instanceof EveryXDaysMedication){
                    fileString = fileString.concat("EveryXDay\n");
                    fileString = fileString.concat(med.name);
                    fileString = fileString.concat("/");
                    fileString = fileString.concat(med.Strength);
                    fileString = fileString.concat("/");
                    fileString = fileString.concat(String.valueOf(med.numLeft));
                    fileString = fileString.concat("/");
                    fileString = fileString.concat(med.Type);
                    fileString = fileString.concat("/");
                    if (med.withFood){
                        fileString = fileString.concat("1");
                    }
                    else{
                        fileString = fileString.concat("0");
                    }
                    fileString = fileString.concat("/");
                    fileString = fileString.concat(((EveryXDaysMedication) med).times.toString());
                    fileString = fileString.concat("/");
                    fileString = fileString.concat(String.valueOf(((EveryXDaysMedication) med).numberOfDays));
                    fileString = fileString.concat("/");
                    fileString = fileString.concat(((EveryXDaysMedication) med).startDate.toString());
                    if (!med.prevTakenAt.isEmpty()){
                        fileString = fileString.concat("/");
                        fileString = fileString.concat(med.prevTakenAt.toString());
                    }
                }
                else if (med instanceof SpecificDayMedication){
                    fileString = fileString.concat("SpecificDay\n");
                    fileString = fileString.concat(med.name);
                    fileString = fileString.concat("/");
                    fileString = fileString.concat(med.Strength);
                    fileString = fileString.concat("/");
                    fileString = fileString.concat(String.valueOf(med.numLeft));
                    fileString = fileString.concat("/");
                    fileString = fileString.concat(med.Type);
                    fileString = fileString.concat("/");
                    if (med.withFood){
                        fileString = fileString.concat("1");
                    }
                    else{
                        fileString = fileString.concat("0");
                    }
                    fileString = fileString.concat("/");
                    fileString = fileString.concat(((SpecificDayMedication) med).Times.toString());
                    if (!med.prevTakenAt.isEmpty()){
                        fileString = fileString.concat("/");
                        fileString = fileString.concat(med.prevTakenAt.toString());
                    }
                }
                else if (med instanceof WeeklyMedication){
                    fileString = fileString.concat("Weekly\n");
                    fileString = fileString.concat(med.name);
                    fileString = fileString.concat("/");
                    fileString = fileString.concat(med.Strength);
                    fileString = fileString.concat("/");
                    fileString = fileString.concat(String.valueOf(med.numLeft));
                    fileString = fileString.concat("/");
                    fileString = fileString.concat(med.Type);
                    fileString = fileString.concat("/");
                    if (med.withFood){
                        fileString = fileString.concat("1");
                    }
                    else{
                        fileString = fileString.concat("0");
                    }
                    fileString = fileString.concat("/");
                    fileString = fileString.concat(((WeeklyMedication) med).times.toString());
                    fileString = fileString.concat("/");
                    fileString = fileString.concat(((WeeklyMedication) med).Day);
                    if (!med.prevTakenAt.isEmpty()){
                        fileString = fileString.concat("/");
                        fileString = fileString.concat(med.prevTakenAt.toString());
                    }
                }
                else if (med instanceof MonthlyMedication){
                    fileString = fileString.concat("Monthly\n");
                    fileString = fileString.concat(med.name);
                    fileString = fileString.concat("/");
                    fileString = fileString.concat(med.Strength);
                    fileString = fileString.concat("/");
                    fileString = fileString.concat(String.valueOf(med.numLeft));
                    fileString = fileString.concat("/");
                    fileString = fileString.concat(med.Type);
                    fileString = fileString.concat("/");
                    if (med.withFood){
                        fileString = fileString.concat("1");
                    }
                    else{
                        fileString = fileString.concat("0");
                    }
                    fileString = fileString.concat("/");
                    fileString = fileString.concat(String.valueOf(((MonthlyMedication) med).dayOfMonth));
                    if (!med.prevTakenAt.isEmpty()){
                        fileString = fileString.concat("/");
                        fileString = fileString.concat(med.prevTakenAt.toString());
                    }
                }
                fileString = fileString.concat("\n");
            }
            fileString = fileString.concat("NEXTMEDS\n");
            for (Medication med: nextMedList
                 ) {
                fileString = fileString.concat(med.toString());
                fileString = fileString.concat("/");
            }
            Log.e("1",String.valueOf(nextMedList.size()));
            byte[] fileContent = fileString.getBytes(StandardCharsets.UTF_8);
            OutputStream outputStream = encryptedFile.openFileOutput();
            outputStream.write(fileContent);
            outputStream.flush();
            outputStream.close();
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
//Adds new medications
    void newMed(Medication med){
        int index = medIndex(med.toString());
        if (index==-1){MedicationList.add(med);}
        else {MedicationList.get(index).numLeft += med.numLeft;}
        NextMeds();
        writeToFile();
    }
//New Med from read
    void newMedFromFile(Medication med){
        int index = medIndex(med.toString());
        if (index==-1){MedicationList.add(med);}
        else {MedicationList.get(index).numLeft += med.numLeft;}
        writeToFile();
    }
//Get medication information from API
    void fromAPI(String GTIN, final VolleyCallBack callBack){
        String url = "https://ampoule.herokuapp.com/gtin/"+GTIN; //URL for the API
        //Request for the API
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) { //When the API responds
                        try {
                            JSONObject jsonObj = response.getJSONObject("data"); //Retrieve data
                            callBack.onSuccess(jsonObj); //Send data
                        } catch (JSONException e) {
                            callBack.onFail(); //If no medication
                        }
                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // TODO: Handle error

                    }
                });
        reqQueue.add(jsonObjectRequest); // Add request to the request queue
    }
//Get All Current Medications
    ArrayList<Medication> medications(){
        return MedicationList;
    }
//Get MedicationIndex from name/strength combo
    int medIndex(String medNameStr){
        for (int index = 0; index<MedicationList.size(); index++){
            if (MedicationList.get(index).toString().equals(medNameStr)){
                return index;
            }
        }
        return -1;
    }
//Adds a taken Time for a medication
    void medTaken(Medication medication, LocalDateTime localDateTime){
        int index = medIndex(medication.toString());
        Medication med = MedicationList.get(index);
        med.prevTakenAt.put(localDateTime.toString(), Boolean.TRUE);
        MedicationList.set(index, med);
        NextMeds();
        writeToFile();
    }

//Get all medications taken on a specified day
    ArrayList<Medication> medicationsOn(int year, int month, int day){
        ArrayList<Medication> meds = new ArrayList<>();
        for (Medication med: MedicationList
             ) {
            try{
                for (String key: med.prevTakenAt.keySet()
                     ) {
                    if (LocalDateTime.parse(key).toLocalDate().equals(LocalDate.of(year,month+1,day))){
                        meds.add(med);
                    }
                }
            }
            catch (Exception ignored){
            }
        }
        return meds;
    }
}
