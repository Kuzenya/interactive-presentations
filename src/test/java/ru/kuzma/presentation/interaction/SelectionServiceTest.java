package ru.kuzma.presentation.interaction;

import com.jme3.material.Material;
import com.jme3.math.Ray;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Box;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class SelectionServiceTest {
    @Test
    void choosesNearestTaggedGeometry() {
        Node root = new Node("root");
        Geometry far = box("far", 0);
        Geometry near = box("near", 2);
        far.setUserData(SelectionService.OBJECT_ID, "far");
        near.setUserData(SelectionService.OBJECT_ID, "near");
        root.attachChild(far);
        root.attachChild(near);
        root.updateGeometricState();

        Optional<SelectionService.Hit> hit = new SelectionService().select(root,
                new Ray(new Vector3f(0, 0, 5), new Vector3f(0, 0, -1)));
        assertTrue(hit.isPresent());
        assertEquals("near", hit.get().objectId());
        assertEquals(2.5f, hit.get().distance(), 1e-4);
    }

    @Test
    void ignoresUntaggedGeometry() {
        Node root = new Node("root");
        root.attachChild(box("floor", 0));
        root.updateGeometricState();
        assertTrue(new SelectionService().select(root,
                new Ray(new Vector3f(0, 0, 5), new Vector3f(0, 0, -1))).isEmpty());
    }

    private Geometry box(String name, float z) {
        Geometry geometry = new Geometry(name, new Box(0.5f, 0.5f, 0.5f));
        geometry.setLocalTranslation(0, 0, z);
        geometry.setMaterial(new Material());
        return geometry;
    }
}
