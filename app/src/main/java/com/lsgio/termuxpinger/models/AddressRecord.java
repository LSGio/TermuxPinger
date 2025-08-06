package com.lsgio.termuxpinger.models;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

public class AddressRecord {
    private String label;
    private String address;
    private List<String> history;

    public AddressRecord(String label, String address) {
        setLabel(label);
        setAddress(address);
        setEmptyHistory();
    }

    public String getLabel() {
        return label;
    }

    public String getAddress() {
        return address;
    }

    public List<String> getHistory() {
        return history;
    }

    public void setLabel(String label) {
        if (label == null) {
            throw new NullPointerException("label cannot be null!");

        }
        this.label = label.trim();
    }

    public void setAddress(String address) {
        if (address == null) {
            throw new NullPointerException("address cannot be null!");

        }
        this.address = address.trim();
    }

    public void addToHistory(String result) {
        if (result == null) {
            throw new NullPointerException("result cannot be null!");
        }
        history.add(result.trim());
    }

    public void setEmptyHistory() {
        this.history = new ArrayList<>();
    }

    // Serialize a list of IpAddressRecord to JSON string
    public static String toJsonList(List<AddressRecord> list) {
        JSONArray arr = new JSONArray();
        for (AddressRecord rec : list) {
            org.json.JSONObject obj = new org.json.JSONObject();
            try {
                obj.put("label", rec.getLabel());
                obj.put("address", rec.getAddress());
                obj.put("history", new JSONArray(rec.getHistory()));
            } catch (JSONException e) {
                // ignore or log
            }
            arr.put(obj);
        }
        return arr.toString();
    }

    // Deserialize a JSON string to a list of IpAddressRecord
    public static List<AddressRecord> fromJsonString(String json) {
        List<AddressRecord> list = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                org.json.JSONObject obj = arr.getJSONObject(i);
                String label = obj.optString("label", "");
                String address = obj.optString("address", "");
                List<String> history = new ArrayList<>();
                JSONArray histArr = obj.optJSONArray("history");
                if (histArr != null) {
                    for (int j = 0; j < histArr.length(); j++) {
                        history.add(histArr.optString(j, ""));
                    }
                }
                AddressRecord rec = new AddressRecord(label, address);
                rec.setEmptyHistory();
                for (String h : history) rec.addToHistory(h);
                list.add(rec);
            }
        } catch (JSONException e) {
            // ignore or log
        }
        return list;
    }
}