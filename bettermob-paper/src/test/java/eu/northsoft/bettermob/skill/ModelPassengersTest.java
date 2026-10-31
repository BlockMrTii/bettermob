package eu.northsoft.bettermob.skill;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModelPassengersTest {
    private final UUID owner = UUID.randomUUID();
    private final UUID first = UUID.randomUUID();
    private final UUID second = UUID.randomUUID();

    @Test
    void ridersAreFoundBySeatCaseInsensitivelyOrAll() {
        ModelPassengers passengers = new ModelPassengers();
        passengers.add(owner, "Driver", first);
        passengers.add(owner, "seat2", second);
        assertEquals(List.of(first), passengers.riders(owner, "driver"));
        assertEquals(2, passengers.riders(owner, null).size());
        assertEquals(owner, passengers.ownerOf(first));
        assertTrue(passengers.riders(owner, "none").isEmpty());
    }

    @Test
    void aRiderMovingSeatsOrLeavingIsForgotten() {
        ModelPassengers passengers = new ModelPassengers();
        passengers.add(owner, "driver", first);
        passengers.add(owner, "seat2", first);
        assertTrue(passengers.riders(owner, "driver").isEmpty());
        assertEquals(List.of(first), passengers.riders(owner, "seat2"));
        passengers.remove(first);
        assertNull(passengers.ownerOf(first));
        assertTrue(passengers.riders(owner, null).isEmpty());
    }

    @Test
    void clearingAnOwnerReturnsAndForgetsItsRiders() {
        ModelPassengers passengers = new ModelPassengers();
        passengers.add(owner, "driver", first);
        passengers.add(owner, "seat2", second);
        assertEquals(2, passengers.clear(owner).size());
        assertNull(passengers.ownerOf(second));
        assertTrue(passengers.riders(owner, null).isEmpty());
    }
}
