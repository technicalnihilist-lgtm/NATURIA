package com.naturia.app.model;

import java.util.ArrayList;
import java.util.List;

public class Mix {
    public long id;
    public String name;
    public long createdAt;
    public List<MixLayer> layers = new ArrayList<>();

    public Mix(String name) {
        this.name = name;
    }
}
