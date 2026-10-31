package eu.northsoft.bettermob.model;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ModelRiderTest {
    private final UUID first = UUID.randomUUID();
    private final UUID second = UUID.randomUUID();

    @Test
    void ridersAreFilteredBySeatIgnoringCaseAndDeduplicated() {
        List<ModelRider> riders = List.of(new ModelRider(first, "Driver"), new ModelRider(second, "seat2"), new ModelRider(first, "driver"));
        assertEquals(List.of(first), ModelRider.select(riders, "DRIVER"));
        assertEquals(List.of(first, second), ModelRider.select(riders, null));
        assertEquals(List.of(first, second), ModelRider.select(riders, " "));
        assertEquals(List.of(), ModelRider.select(riders, "none"));
    }

    @Test
    void aRiderWithoutASeatNameOnlyMatchesAnAnySeatQuery() {
        List<ModelRider> riders = List.of(new ModelRider(first, null));
        assertEquals(List.of(first), ModelRider.select(riders, null));
        assertEquals(List.of(), ModelRider.select(riders, "driver"));
    }
}
