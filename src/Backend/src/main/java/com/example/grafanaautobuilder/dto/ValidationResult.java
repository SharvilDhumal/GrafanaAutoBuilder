package com.example.grafanaautobuilder.dto;

public class ValidationResult {
    private int row;
    private String title;
    private boolean ok;
    private String message;

    public ValidationResult() {}

    public ValidationResult(int row, String title, boolean ok, String message) {
        this.row = row;
        this.title = title;
        this.ok = ok;
        this.message = message;
    }

    public static ValidationResult ok(int row, String title) {
        return new ValidationResult(row, title, true, "OK");
    }

    public static ValidationResult error(int row, String title, String message) {
        return new ValidationResult(row, title, false, message);
    }

    public int getRow() { return row; }
    public void setRow(int row) { this.row = row; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public boolean isOk() { return ok; }
    public void setOk(boolean ok) { this.ok = ok; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
