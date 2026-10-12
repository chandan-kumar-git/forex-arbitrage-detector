package com.arbitrage.feed;

import com.arbitrage.TestSnapshots;
import com.arbitrage.model.RateSnapshot;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Checks the text protocol: what is encoded can be decoded, and broken lines are rejected. */
class FeedProtocolTest {

    private static final String GOOD_LINE = "SNAPSHOT 1760261111000 USD,EUR,GBP "
            + "1.00000000,0.92500000,0.78661000,1.07892000,1.00000000,0.85634000,1.26873000,1.16543000,1.00000000";

    @Test
    void encodeThenDecodeGivesTheSameSnapshot() throws FeedException {
        RateSnapshot original = TestSnapshots.reference();

        RateSnapshot copy = FeedProtocol.decode(FeedProtocol.encode(original));

        assertEquals(original.codes(), copy.codes());
        assertEquals(original.timestamp(), copy.timestamp());
        for (int i = 0; i < original.size(); i++) {
            for (int j = 0; j < original.size(); j++) {
                assertEquals(original.rate(i, j), copy.rate(i, j), 1e-8);
            }
        }
    }

    @Test
    void decodesTheExampleOfTheSpec() throws FeedException {
        RateSnapshot snapshot = FeedProtocol.decode(GOOD_LINE);

        assertEquals(3, snapshot.size());
        assertEquals("EUR", snapshot.code(1));
        assertEquals(1760261111000L, snapshot.timestamp());
        assertEquals(0.925, snapshot.rate(0, 1), 1e-9);
    }

    @Test
    void encodedLineHasExactlyFourParts() {
        String line = FeedProtocol.encode(TestSnapshots.reference());

        assertEquals(4, line.split(" ").length);
        assertTrue(line.startsWith("SNAPSHOT 0 USD,EUR,GBP,INR "));
    }

    @Test
    void handshakeLines() {
        assertEquals("HELLO desk-1", FeedProtocol.hello("desk-1"));
        assertEquals("WELCOME RateFeedServer 1000", FeedProtocol.welcome("RateFeedServer", 1000));
    }

    @Test
    void wrongKeywordIsRejected() {
        assertBad(GOOD_LINE.replace("SNAPSHOT", "SNAPSHOOT"));
    }

    @Test
    void wrongNumberOfRatesIsRejected() {
        assertBad(GOOD_LINE.substring(0, GOOD_LINE.lastIndexOf(',')));
    }

    @Test
    void negativeRateIsRejected() {
        assertBad(GOOD_LINE.replace("0.92500000", "-0.92500000"));
    }

    @Test
    void badCodeIsRejected() {
        assertBad(GOOD_LINE.replace("EUR", "eur"));
        assertBad(GOOD_LINE.replace("EUR", "EURO"));
    }

    @Test
    void otherBrokenLinesAreRejected() {
        assertBad("SNAPSHOT 1760261111000 USD,EUR,GBP");                 // a part is missing
        assertBad(GOOD_LINE.replace("1760261111000", "yesterday"));      // timestamp is not a number
        assertBad(GOOD_LINE.replace("1.00000000,0.925", "1.50000000,0.925"));   // diagonal is not 1
        assertBad("SNAPSHOT 1 USD,EUR 1.0,0.9,1.1,1.0");                 // fewer than 3 currencies
    }

    private void assertBad(String line) {
        FeedException e = assertThrows(FeedException.class, () -> FeedProtocol.decode(line));
        assertTrue(e.getMessage().startsWith("Bad snapshot: "), e.getMessage());
    }
}
