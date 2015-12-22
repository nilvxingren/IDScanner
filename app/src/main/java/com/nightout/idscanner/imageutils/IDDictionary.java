package com.nightout.idscanner.imageutils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by benreyhani on 15-12-20.
 */
public class IDDictionary {
    public static final String FIRST_NAME_KEY = "first_name";
    public static final String LAST_NAME_KEY = "last_name";
    public static final String ID_EXPIRY_DATE_KEY = "id_expiry_date";
    public static final String GENDER_KEY = "gender";
    public static final String BIRTH_DATE_KEY = "birth_date";

    // In order of what comes first ON driver's license
    public static final String[] ID_EXPIRY_TRIGGERS = {"DBA"};
    public static final String[] LAST_NAME_TRIGGERS = {"DCS"};
    public static final String[] FIRST_NAME_TRIGGERS = {"DCT"};
    public static final String[] BIRTH_DAY_TRIGGERS = {"DBB"};
    public static final String[] GENDER_TRIGGERS = {"DBC"};

    // Assume ON drinking age for now
    public static final int ACTIVATED_DRINKING_AGE = 19;

    private static final Map<String, ArrayList<String>> BARCODE_ID_DICTIONARY;

    static {
        Map<String, ArrayList<String>> idDict = new LinkedHashMap<>();

        idDict.put(ID_EXPIRY_DATE_KEY, getAttributeKeys(ID_EXPIRY_TRIGGERS));
        idDict.put(LAST_NAME_KEY, getAttributeKeys(LAST_NAME_TRIGGERS));
        idDict.put(FIRST_NAME_KEY, getAttributeKeys(FIRST_NAME_TRIGGERS));
        idDict.put(BIRTH_DATE_KEY, getAttributeKeys(BIRTH_DAY_TRIGGERS));
        idDict.put(GENDER_KEY, getAttributeKeys(GENDER_TRIGGERS));

        BARCODE_ID_DICTIONARY = Collections.unmodifiableMap(idDict);
    }

    private static ArrayList<String> getAttributeKeys(String [] triggerArray){
        return new ArrayList<>(Arrays.asList(triggerArray));
    }

    public static Map<String, ArrayList<String>> getBarcodeIdDictionary() {
        return BARCODE_ID_DICTIONARY;
    }
}
