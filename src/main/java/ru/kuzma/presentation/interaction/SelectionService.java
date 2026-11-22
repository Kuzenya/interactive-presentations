package ru.kuzma.presentation.interaction;

import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;
import com.jme3.math.Ray;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;

import java.util.Optional;

/** Выбирает ближайший объект, у которого в Spatial или предке задано userData sceneObjectId. */
public final class SelectionService {
    public static final String OBJECT_ID = "sceneObjectId";
    public record Hit(String objectId, float distance, Vector3f contactPoint) { }

    public Optional<Hit> select(Node sceneRoot, Ray ray) {
        CollisionResults results = new CollisionResults();
        sceneRoot.collideWith(ray, results);
        CollisionResult nearest = null;
        String nearestId = null;
        for (CollisionResult result : results) {
            String id = findObjectId(result.getGeometry());
            if (id != null && (nearest == null || result.getDistance() < nearest.getDistance())) {
                nearest = result;
                nearestId = id;
            }
        }
        if (nearest == null) return Optional.empty();
        return Optional.of(new Hit(nearestId, nearest.getDistance(), nearest.getContactPoint()));
    }

    private String findObjectId(Spatial spatial) {
        Spatial current = spatial;
        while (current != null) {
            Object value = current.getUserData(OBJECT_ID);
            if (value instanceof String id) return id;
            current = current.getParent();
        }
        return null;
    }
}
