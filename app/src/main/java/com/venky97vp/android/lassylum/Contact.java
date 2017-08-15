package com.venky97vp.android.lassylum;

/**
 * Created by venky on 30-04-2017.
 */

public class Contact {
    String name,number;

    public Contact(String name, String number) {
        this.name = name;
        this.number = number;
    }

    public String getName() {

        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }
}
