package com.unigent.machines.homesurve1.processor.objectmemory;

import org.dizitart.no2.objects.Id;

public class ObjectIdCounter {

    @Id
    private int type;
    private int counter;

    public ObjectIdCounter() {
    }

    public ObjectIdCounter(int type, int counter) {
        this.type = type;
        this.counter = counter;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getCounter() {
        return counter;
    }

    public void setCounter(int counter) {
        this.counter = counter;
    }
}
