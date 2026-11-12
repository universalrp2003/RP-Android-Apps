package com.universalrp.cleansweep.data

import android.content.Context
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class NetworkSplitDayTest {
    private val c: Context get() = RuntimeEnvironment.getApplication()
    private val p get() = c.getSharedPreferences(NetworkSplit.PREFS, 0)
    @Before fun seedYesterday() { p.edit().clear().putString(NetworkSplit.KEY_DAY, LocalDate.now().minusDays(1).toString())
        .putLong(NetworkSplit.KEY_4G_TODAY, 800L).putLong(NetworkSplit.KEY_5G_TODAY, 1200L)
        .putLong(NetworkSplit.KEY_LAST_CUMULATIVE, 2000L).putLong(NetworkSplit.KEY_COUNTING_SINCE_MS, 1L).commit() }
    @Test fun readFourGResetsWithoutServiceOrTraffic() { assertEquals(0L, NetworkSplit.fourGBytes(c));assertEquals(LocalDate.now().toString(),p.getString(NetworkSplit.KEY_DAY,"")) }
    @Test fun readFiveGResetsWithoutService() { assertEquals(0L,NetworkSplit.fiveGBytes(c)) }
    @Test fun statusCannotShowYesterday() { val s=NetworkSplit.status(c,1024.0);assertEquals(0.0,s.fourGUsedMb,0.0);assertEquals(0.0,s.fiveGUsedMb,0.0) }
    @Test fun staleBaselineIsNeverCarriedIntoToday() { assertTrue(NetworkSplit.countingSinceMs(c)>1);assertEquals(-1L,NetworkSplit.lastCumulative(c));assertTrue(p.getLong(NetworkSplit.KEY_COUNTING_SINCE_MS,0)>1) }
    @Test fun firstNewBaselineDoesNotBackfillHistory() { NetworkSplit.setLastCumulative(c,9000);assertEquals(9000L,NetworkSplit.lastCumulative(c));assertEquals(0L,NetworkSplit.fourGBytes(c)) }
    @Test fun incrementStartsNewDayRatherThanAddingToYesterday() { NetworkSplit.addDelta(c,30,true);assertEquals(30L,NetworkSplit.fiveGBytes(c));assertEquals(0L,NetworkSplit.fourGBytes(c)) }
    @Test fun repeatedReadsKeepTodaysSamplesAndBaseline() { NetworkSplit.resetDayIfNeeded(c);NetworkSplit.addDelta(c,42,false);NetworkSplit.setLastCumulative(c,55);repeat(3){NetworkSplit.status(c,1024.0)};assertEquals(42L,NetworkSplit.fourGBytes(c));assertEquals(55L,NetworkSplit.lastCumulative(c)) }
    @Test fun futureDayOrZoneChangeAlsoInvalidates() { p.edit().putString(NetworkSplit.KEY_DAY,LocalDate.now().plusDays(1).toString()).commit();assertEquals(0L,NetworkSplit.fourGBytes(c)) }
}
