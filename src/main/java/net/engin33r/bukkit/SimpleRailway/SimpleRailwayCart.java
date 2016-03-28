package net.engin33r.bukkit.SimpleRailway;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "sr_carts")
public class SimpleRailwayCart {
    @DatabaseField(id = true)
    private int id;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
}
