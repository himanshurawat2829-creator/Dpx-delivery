package com.dpx.model;

/**
 * Delivery model class.
 * Represents a single courier delivery record.
 */
public class Delivery {

    private String id;
    private String senderName;
    private String recipientName;
    private String address;
    private double weight;
    private String status;

    // ── Constructors ──────────────────────────────────────────────────────────

    public Delivery() {}

    public Delivery(String id, String senderName, String recipientName,
                    String address, double weight) {
        this.id            = id;
        this.senderName    = senderName;
        this.recipientName = recipientName;
        this.address       = address;
        this.weight        = weight;
        this.status        = "Pending";
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public String getId()                      { return id; }
    public void   setId(String id)             { this.id = id; }

    public String getSenderName()              { return senderName; }
    public void   setSenderName(String n)      { this.senderName = n; }

    public String getRecipientName()           { return recipientName; }
    public void   setRecipientName(String n)   { this.recipientName = n; }

    public String getAddress()                 { return address; }
    public void   setAddress(String address)   { this.address = address; }

    public double getWeight()                  { return weight; }
    public void   setWeight(double weight)     { this.weight = weight; }

    public String getStatus()                  { return status; }
    public void   setStatus(String status)     { this.status = status; }
}
