package com.vikasyadavnsit.cdc.data;

public class SpinnerItem {
    private String label;
    private User value;

    public SpinnerItem(String label, User value) {
        this.label = label;
        this.value = value;
    }

    public String getLabel() {
        return label;
    }

    public User getValue() {
        return value;
    }

    @Override
    public String toString() {
        return label; // This will be used to display in the Spinner
    }
}

