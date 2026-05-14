package com.example.pickleball.model;

import java.io.Serializable;

/** Sân con bên trong 1 sân lớn (VD: Pickleball 1, Pickleball 2...) */
public class SubCourt implements Serializable {
    private String subCourtId;
    private String courtId;     // sân cha
    private String name;        // "Pickleball 1", "Pickleball 2"...
    private int sortOrder;      // thứ tự hiển thị

    public SubCourt() {}

    public SubCourt(String courtId, String name, int sortOrder) {
        this.courtId   = courtId;
        this.name      = name;
        this.sortOrder = sortOrder;
    }

    public String getSubCourtId()           { return subCourtId; }
    public void setSubCourtId(String v)     { this.subCourtId = v; }

    public String getCourtId()              { return courtId; }
    public void setCourtId(String v)        { this.courtId = v; }

    public String getName()                 { return name; }
    public void setName(String v)           { this.name = v; }

    public int getSortOrder()               { return sortOrder; }
    public void setSortOrder(int v)         { this.sortOrder = v; }
}
