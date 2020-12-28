package metrics;

import com.scale.check_out.infrastructure.metrics.BusinessMetricsInMemory;
import com.scale.check_out.domain.metrics.BusinessMetrics;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class BusinessMetricsTest {

    /**
     * Return empty after finishing first shopping cart and immediately getting metrics (before collect time has passed).
     */
    @Test
    public void metricListIsEmptyWhenGettingMetricsBefore1SecondHasPast() {
        BusinessMetrics bm = new BusinessMetricsInMemory();
        bm.newShoppingCartStarted("TEST1", 10L);
        bm.finishShoppingCart(15L, 20L, true, false);
        bm.newShoppingCartStarted("TEST1", 10L);
        bm.finishShoppingCart(30L, 42L, false, true);
        assertEquals(0, bm.pullAllFinishedShoppingCarts().length);
    }

    /**
     * Return 1 item after finishing first shopping cart and waiting for 1 sec to get metrics (after collect time has passed).
     */
    @Test
    public void metricListContains1ItemWhenGettingMetricsAfter1SecondHasPast() throws InterruptedException {
        BusinessMetrics bm = new BusinessMetricsInMemory();
        bm.newShoppingCartStarted("TEST2", 10L);
        bm.finishShoppingCart(15L, 20L, true, false);
        bm.newShoppingCartStarted("TEST2", 10L);
        bm.finishShoppingCart(30L, 42L, false, true);
        Thread.sleep(1000);
        var items = bm.pullAllFinishedShoppingCarts();
        assertEquals(1, items.length);
        assertThat(items[0].getNumberOfCustomers(), is(equalTo(10L)));
        assertThat(items[0].getShoppingCartFinished(), is(equalTo(2L)));
        assertThat(items[0].getServiceTimeInMs(), is(equalTo(22L)));
        assertThat(items[0].getWaitingTimeInMs(), is(equalTo(31L)));
        assertThat(items[0].getTransactionsPerSecond(), is(equalTo(2L)));
    }

    @Test
    public void metricListContains1ItemWithCorrectSummary() throws InterruptedException {
        BusinessMetrics bm = new BusinessMetricsInMemory();
        bm.newShoppingCartStarted("TEST3", 10L);
        bm.finishShoppingCart(15L, 20L, true, false);
        bm.newShoppingCartStarted("TEST3", 10L);
        bm.finishShoppingCart(15L, 20L, false, false);
        Thread.sleep(1000);
        bm.newShoppingCartStarted("TEST3", 10L);
        bm.finishShoppingCart(30L, 42L, false, true);
        Thread.sleep(950);
        var items = bm.pullAllFinishedShoppingCarts();
        assertEquals(1, items.length);
        assertThat(items[0].getNumberOfCustomers(), is(equalTo(10L)));
        assertThat(items[0].getShoppingCartFinished(), is(equalTo(2L)));
        assertThat(items[0].getServiceTimeInMs(), is(equalTo(15L)));
        assertThat(items[0].getTransactionsPerSecond(), is(equalTo(2L)));
    }

    @Test
    public void metricListContains3SummaryItemsAfter3Seconds() throws InterruptedException {
        BusinessMetrics bm = new BusinessMetricsInMemory();
        // Start session and insert 2 items in the 1st second
        bm.newShoppingCartStarted("TEST4", 10L);
        bm.finishShoppingCart(15L, 20L, true, false);
        bm.newShoppingCartStarted("TEST4", 10L);
        bm.finishShoppingCart(15L, 20L, false, false);
        Thread.sleep(1000);
        // Leave the 2nd second empty
        bm.newShoppingCartStarted("TEST4", 10L);
        Thread.sleep(1000);
        // Add 1 event in the 3rd second
        bm.finishShoppingCart(300L, 330L, false, true);
        Thread.sleep(1000);
        var items = bm.pullAllFinishedShoppingCarts();
        assertEquals(3, items.length);

        // Check 2nd item
        assertThat(items[1].getNumberOfCustomers(), is(equalTo(10L)));
        assertThat(items[1].getShoppingCartFinished(), is(equalTo(0L)));
        assertNull(items[1].getServiceTimeInMs());
        assertThat(items[1].getTransactionsPerSecond(), is(equalTo(0L)));

        // Check 3rd item
        assertThat(items[2].getNumberOfCustomers(), is(equalTo(10L)));
        assertThat(items[2].getShoppingCartFinished(), is(equalTo(1L)));
        assertThat(items[2].getServiceTimeInMs(), is(equalTo(300L)));
        assertThat(items[2].getTransactionsPerSecond(), is(equalTo(1L)));
    }

    /**
     * More complex scenario
     */
    @Test
    public void summariesAreGeneratedCorrectlyFor5Seconds() throws InterruptedException {
        BusinessMetrics bm = new BusinessMetricsInMemory();

        // Start session and insert 6 items in the 1st second
        bm.newShoppingCartStarted("TEST5", 10L);
        Thread.sleep(200);
        bm.finishShoppingCart(200L, 250L, true, false);
        bm.newShoppingCartStarted("TEST5", 10L);
        Thread.sleep(100);
        bm.finishShoppingCart(100L, 120L, false, false);
        bm.newShoppingCartStarted("TEST5", 10L);
        Thread.sleep(200);
        bm.finishShoppingCart(200L, 220L, false, false);    // 500ms so far

        // Partial query should return empty
        var query1 = bm.pullAllFinishedShoppingCarts();
        assertEquals(0, query1.length);

        bm.newShoppingCartStarted("TEST5", 10L);
        Thread.sleep(250);
        bm.finishShoppingCart(250L, 300L, false, false);

        bm.newShoppingCartStarted("TEST5", 10L);
        Thread.sleep(50);                            // 800ms so far
        bm.finishShoppingCart(50L, 72L, false, false);

        bm.newShoppingCartStarted("TEST5", 10L);
        Thread.sleep(100);                           // 900ms so far
        bm.finishShoppingCart(100L, 130L, false, false);

        Thread.sleep(500);                          // 1.4 seconds so far

        bm.newShoppingCartStarted("TEST5", 10L);
        // 2 events in the 2nd second
        bm.finishShoppingCart(500L, 550L, false, false);

        bm.newShoppingCartStarted("TEST5", 10L);
        Thread.sleep(300);                          // 1.7 seconds so far
        bm.finishShoppingCart(300L, 310L, false, false);

        Thread.sleep(300);  // 2 seconds so far

        var query2 = bm.pullAllFinishedShoppingCarts();
        assertEquals(2, query2.length);

        assertThat(query2[0].getNumberOfCustomers(), is(equalTo(10L)));
        assertThat(query2[0].getShoppingCartFinished(), is(equalTo(6L)));
        assertThat(query2[0].getServiceTimeInMs(), is(equalTo(150L)));
        assertThat(query2[0].getTransactionsPerSecond(), is(equalTo(6L)));

        // Check 2nd item
        assertThat(query2[1].getNumberOfCustomers(), is(equalTo(10L)));
        assertThat(query2[1].getShoppingCartFinished(), is(equalTo(2L)));
        assertThat(query2[1].getServiceTimeInMs(), is(equalTo(400L)));
        assertThat(query2[1].getTransactionsPerSecond(), is(equalTo(2L)));

        // Leave 3rd second with no events
        Thread.sleep(1000);

        // And the 4rd second with 1 unfinished transaction
        bm.newShoppingCartStarted("TEST5", 10L);
        Thread.sleep(1000);
        bm.finishShoppingCart(1000L, 310L, false, false);

        var query3 = bm.pullAllFinishedShoppingCarts();

        // 3rd second had no transactions finished, but there were also no pending transactions - simply put, nothing happened
        assertEquals(2, query3.length);
        assertThat(query3[0].getNumberOfCustomers(), is(equalTo(10L)));
        assertNull(query3[0].getShoppingCartFinished());
        assertNull(query3[0].getServiceTimeInMs());
        assertNull(query3[0].getTransactionsPerSecond());

        // 4rd second had no transactions finished, but there was 1 pending transaction - which didn't finish
        assertThat(query3[1].getNumberOfCustomers(), is(equalTo(10L)));
        assertThat(query3[1].getShoppingCartFinished(), is(equalTo(0L)));
        assertNull(query3[1].getServiceTimeInMs());
        assertThat(query3[1].getTransactionsPerSecond(), is(equalTo(0L)));

        // Wait half a second and query again for partial data, which should return nothing
        Thread.sleep(500);  // 3.5 seconds so far

        var query4 = bm.pullAllFinishedShoppingCarts();
        assertEquals(0, query4.length);

        bm.newShoppingCartStarted("TEST5", 10L);
        bm.finishShoppingCart(300L, 311L, false, true);

        assertEquals(0, bm.pullAllFinishedShoppingCarts().length);
        assertEquals(0, bm.pullAllFinishedShoppingCarts().length);
        assertEquals(0, bm.pullAllFinishedShoppingCarts().length);

        Thread.sleep(500);

        var query5 = bm.pullAllFinishedShoppingCarts();
        assertEquals(1, query5.length);

        assertThat(query5[0].getNumberOfCustomers(), is(equalTo(10L)));
        assertThat(query5[0].getShoppingCartFinished(), is(equalTo(1L)));
        assertThat(query5[0].getServiceTimeInMs(), is(equalTo(300L)));
        assertThat(query5[0].getTransactionsPerSecond(), is(equalTo(1L)));

    }

}
