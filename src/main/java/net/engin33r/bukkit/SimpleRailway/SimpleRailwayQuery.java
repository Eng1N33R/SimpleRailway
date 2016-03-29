package net.engin33r.bukkit.SimpleRailway;

import com.j256.ormlite.dao.Dao;

import java.sql.SQLException;
import java.util.UUID;

public class SimpleRailwayQuery {
    private final Dao<?, ?> dao;
    public SimpleRailwayQuery(Dao<?, ?> dao) {
        this.dao = dao;
    }

    public SimpleRailwaySpawner getSpawnerByName(String name) throws SQLException {
        return ((SimpleRailwaySpawner) this.dao.queryBuilder().where().eq("name", name).queryForFirst());
    }

    public SimpleRailwayDespawner getDespawnerByName(String name) throws SQLException {
        return ((SimpleRailwayDespawner) this.dao.queryBuilder().where().eq("name", name).queryForFirst());
    }

    public SimpleRailwayCart getCartByUUID(UUID id) throws SQLException {
        return ((SimpleRailwayCart) this.dao.queryBuilder().where().eq("id", id).queryForFirst());
    }
}
