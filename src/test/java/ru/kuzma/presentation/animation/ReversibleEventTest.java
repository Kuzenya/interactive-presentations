package ru.kuzma.presentation.animation;

import org.junit.jupiter.api.Test;
import ru.kuzma.presentation.model.AnimationChannel;
import ru.kuzma.presentation.model.AnimationSpec;
import ru.kuzma.presentation.model.EventData;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ReversibleEventTest {
    private ReversibleEvent event() {
        EventData data = new EventData("e", "owner", "Event");
        data.setAnimations(List.of(new AnimationSpec("a", AnimationChannel.ROTATE_Y, 6, 2),
                new AnimationSpec("b", AnimationChannel.TRANSLATE_Y, 1, 4)));
        return new ReversibleEvent(data);
    }

    @Test
    void advancesAndClampsAtEnd() {
        ReversibleEvent event = event();
        event.toggle();
        assertEquals(0.5f, event.advance(2), 1e-6);
        assertEquals(1f, event.advance(100), 1e-6);
        assertFalse(event.isRunning());
    }

    @Test
    void reversalDoesNotChangeProgressOrCauseJump() {
        ReversibleEvent event = event();
        event.toggle();
        event.advance(1.6f);
        float before = event.getProgress();
        int oldDirection = event.getDirection();
        event.toggle();
        assertEquals(before, event.getProgress());
        assertEquals(-oldDirection, event.getDirection());
        assertTrue(event.advance(0.8f) < before);
    }

    @Test
    void repeatedActivationAtEndStartsBackward() {
        ReversibleEvent event = event();
        event.toggle();
        event.advance(5);
        event.toggle();
        assertEquals(-1, event.getDirection());
        assertEquals(1, event.getProgress());
        event.advance(2);
        assertEquals(0.5f, event.getProgress(), 1e-6);
    }

    @Test
    void clipTimeUsesNormalizedProgressAndOwnDuration() {
        ReversibleEvent event = event();
        AnimationSpec spec = event.getDefinition().getAnimations().get(1);
        event.toggle();
        event.advance(2);
        assertEquals(2f, event.clipTime(spec), 1e-6);
        assertEquals(0.5f, event.offset(spec), 1e-6);
    }
}
