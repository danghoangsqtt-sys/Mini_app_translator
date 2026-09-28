package com.bluetooth.communicator;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.nio.charset.StandardCharsets;
import org.junit.Test;

public class InboundMessageAssemblerTest {
    @Test
    public void orderedFragmentsCompleteLegacyPayload() {
        InboundMessageAssembler assembler = new InboundMessageAssembler(32, 4, 2, 1000);

        assertEquals(InboundMessageAssembler.Status.ACCEPTED,
                assembler.accept("id", 0, false, bytes("mhello "), 0).status);
        InboundMessageAssembler.Result result =
                assembler.accept("id", 1, true, bytes("world"), 1);

        assertEquals(InboundMessageAssembler.Status.COMPLETE, result.status);
        assertArrayEquals(bytes("mhello world"), result.payload);
        assertEquals(0, assembler.inFlightCount());
    }

    @Test
    public void repeatedLastFragmentIsAcknowledgedWithoutDuplication() {
        InboundMessageAssembler assembler = new InboundMessageAssembler(32, 4, 2, 1000);
        assembler.accept("id", 0, false, bytes("mone"), 0);

        assertEquals(InboundMessageAssembler.Status.ACCEPTED,
                assembler.accept("id", 0, false, bytes("mone"), 1).status);
        InboundMessageAssembler.Result result =
                assembler.accept("id", 1, true, bytes("two"), 2);

        assertArrayEquals(bytes("monetwo"), result.payload);
    }

    @Test
    public void outOfOrderFragmentDropsPartialAssembly() {
        InboundMessageAssembler assembler = new InboundMessageAssembler(32, 4, 2, 1000);
        assembler.accept("id", 0, false, bytes("mone"), 0);

        assertEquals(InboundMessageAssembler.Status.REJECTED,
                assembler.accept("id", 2, true, bytes("three"), 1).status);
        assertEquals(0, assembler.inFlightCount());
        assertEquals(InboundMessageAssembler.Status.COMPLETE,
                assembler.accept("id", 0, true, bytes("mretry"), 2).status);
    }

    @Test
    public void sizeAndFragmentLimitsReleaseState() {
        InboundMessageAssembler assembler = new InboundMessageAssembler(6, 2, 2, 1000);
        assembler.accept("size", 0, false, bytes("1234"), 0);
        assertEquals(InboundMessageAssembler.Status.REJECTED,
                assembler.accept("size", 1, true, bytes("567"), 1).status);

        assembler.accept("parts", 0, false, bytes("1"), 2);
        assembler.accept("parts", 1, false, bytes("2"), 3);
        assertEquals(InboundMessageAssembler.Status.REJECTED,
                assembler.accept("parts", 2, true, bytes("3"), 4).status);
        assertEquals(0, assembler.inFlightCount());
    }

    @Test
    public void inFlightLimitAndTimeoutBoundMissingFinalFrames() {
        InboundMessageAssembler assembler = new InboundMessageAssembler(32, 4, 2, 10);
        assembler.accept("a", 0, false, bytes("ma"), 0);
        assembler.accept("b", 0, false, bytes("mb"), 0);
        assertEquals(InboundMessageAssembler.Status.REJECTED,
                assembler.accept("c", 0, false, bytes("mc"), 1).status);

        assembler.expire(10);
        assertEquals(0, assembler.inFlightCount());
        assertEquals(InboundMessageAssembler.Status.COMPLETE,
                assembler.accept("c", 0, true, bytes("mc"), 10).status);
    }

    @Test
    public void malformedInitialInputIsRejected() {
        InboundMessageAssembler assembler = new InboundMessageAssembler(32, 4, 2, 1000);
        assertEquals(InboundMessageAssembler.Status.REJECTED,
                assembler.accept("id", 1, true, bytes("mdata"), 0).status);
        assertEquals(InboundMessageAssembler.Status.REJECTED,
                assembler.accept("", 0, true, bytes("mdata"), 0).status);
        assertEquals(InboundMessageAssembler.Status.REJECTED,
                assembler.accept("id", 0, true, new byte[0], 0).status);
        assertNull(InboundMessageAssembler.Result.accepted().payload);
    }

    private static byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }
}
