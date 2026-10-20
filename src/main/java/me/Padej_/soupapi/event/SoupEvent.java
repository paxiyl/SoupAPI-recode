package me.Padej_.soupapi.event;

import me.Padej_.soupapi.compat.SoupLog;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * A minimal event with the same {@code register} shape as Fabric API's {@code Event}.
 *
 * <p>The port does not depend on Fabric API (see the note in build.gradle), so the handful of
 * events the mod used are re-created here and fired from the mod's own mixins.
 *
 * <p>Listeners are invoked in registration order and a throwing listener is logged and skipped
 * rather than allowed to abort the frame, which matters because these fire from inside the render
 * loop.
 */
public final class SoupEvent<T> {

    private final List<Consumer<T>> listeners = new ArrayList<Consumer<T>>();
    private final String name;

    public SoupEvent(String name) {
        this.name = name;
    }

    public void register(Consumer<T> listener) {
        listeners.add(listener);
    }

    public void invoke(T context) {
        // Indexed loop: listeners are only added at init, and this runs every frame.
        for (int i = 0; i < listeners.size(); i++) {
            try {
                listeners.get(i).accept(context);
            } catch (Throwable t) {
                SoupLog.error("Listener on event '" + name + "' threw", t);
            }
        }
    }
}
