package ru.kuzma.presentation.animation;

import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import org.junit.jupiter.api.Test;
import ru.kuzma.presentation.model.*;
import ru.kuzma.presentation.scene.DemoSceneFactory;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AnimationRuntimeTest {
    @Test
    void updatesBothChannelsFromSameProgress() {
        SceneData scene = new DemoSceneFactory().create();
        Node shaft = new Node("shaft");
        Node fan = new Node("fan");
        Map<String, Spatial> spatials = new HashMap<>();
        spatials.put("shaft", shaft);
        spatials.put("fan", fan);
        AnimationRuntime runtime = new AnimationRuntime(scene, spatials);
        runtime.toggle("inspection");
        runtime.update(1.4f);
        assertEquals(0.5f, runtime.progress("inspection"), 1e-6);
        assertEquals(0.325f, fan.getLocalTranslation().y, 1e-6);
        assertFalse(shaft.getLocalRotation().isIdentity());
        runtime.toggle("inspection");
        runtime.update(1.4f);
        assertEquals(0, runtime.progress("inspection"), 1e-6);
        assertEquals(0, fan.getLocalTranslation().y, 1e-6);
    }
}
