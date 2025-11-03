package com.example.newEcom.model;

public enum PaymentMethod {

    MOMO("MoMo", "🟣"),
    ZALOPAY("ZaloPay", "🔵");
    private final String displayName;
    private final String icon;

    PaymentMethod(String displayName, String icon) {
        this.displayName = displayName;
        this.icon = icon;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getIcon() {
        return icon;
    }
}
