package nl.rutilo.labeldb.util;

import nl.rutilo.util.testsupport.TestUtils;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;

import static java.time.temporal.ChronoField.*;
import static nl.rutilo.labeldb.util.HumanReadableTime.*;
import static nl.rutilo.labeldb.util.Utils.sleep;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class HumanReadableTimeTest {

    @Test public void testHumanReadableDates() {
        final int           date1    = 2017_09_30;
        final LocalDateTime localDt1 = toLocalDateTime(date1);
        assertThat(localDt1.get(YEAR), is(2017));
        assertThat(localDt1.get(MONTH_OF_YEAR), is(9));
        assertThat(localDt1.get(DAY_OF_MONTH), is(30));

        final long datetime2 = 1832_12_24_19_23_55_999L;
        final LocalDateTime localDt2 = toLocalDateTime(datetime2);
        assertThat(localDt2.get(YEAR), is(1832));
        assertThat(localDt2.get(MONTH_OF_YEAR), is(12));
        assertThat(localDt2.get(DAY_OF_MONTH), is(24));
        assertThat(localDt2.get(HOUR_OF_DAY), is(19));
        assertThat(localDt2.get(MINUTE_OF_HOUR), is(23));
        assertThat(localDt2.get(SECOND_OF_MINUTE), is(55));
        assertThat(getDateOf(datetime2), is(1832_12_24));

        final int nowD = getDate();
        assertThat(getDateOf(toLocalDateTime(nowD)), is(nowD));
        final long nowDt = getDateTime();
        assertThat(getDateTimeOf(toLocalDateTime(nowDt)), is(nowDt));
    }
    @Test public void testHumanReadableTimeFromFile() throws IOException {
        try(final TestUtils.DeletedWhenClosedFile tmpFile = TestUtils.createTempFile()) {
            final long datetime = getDateTime();
            sleep(100);
            Utils.writeStringToFile(tmpFile, "foobar");
            final long fileDatetime = getDateTimeOf(tmpFile);
            final long diff = toMsSinceEpoch(fileDatetime) - toMsSinceEpoch(datetime);
            assertThat("diff=" + diff, diff >= 100 && diff < 500, is(true));
        }

        // Non-existing file
        assertThat(getDateTimeOf(new File("non-existing")), is(-1L));
    }
    @Test public void testSinceEpoch() {
        final long hrDt = 2019_04_29__14_03_55_789L;
        final long sec = toSecondsSinceEpoch(hrDt);
        final long ms = toMsSinceEpoch(hrDt);
        assertThat(ms/1000, is(sec));
    }
    @Test public void testParts() {
        final long hrDt = 2019_04_29__14_03_55_789L;

        assertThat(yearsOf(hrDt),   is(2019));
        assertThat(monthsOf(hrDt),  is(4));
        assertThat(daysOf(hrDt),    is(29));
        assertThat(hoursOf(hrDt),   is(14));
        assertThat(minutesOf(hrDt), is(3));
        assertThat(secondsOf(hrDt), is(55));
        assertThat(msOf(hrDt),      is(789));

        assertThat(fromParts(new int[] { 2019, 6, 30, 12, 34, 56, 777 }), is(2019_06_30__12_34_56_777L));
    }
    @Test public void testToDateTime() {
        assertThat(toDateTime(2019, 2, 3, 4, 5, 6, 7), is(2019_02_03__04_05_06_007L));
    }
    @Test public void testDaysInMonth() {
        assertThat(daysInMonth(2001, 2), is(28));
        assertThat(daysInMonth(2001_02_01__00_00_00_000L), is(28));
        assertThat(daysInMonth(2000, 1), is(31));
        assertThat(daysInMonth(2000, 2), is(29));
        assertThat(daysInMonth(2000, 3), is(31));
        assertThat(daysInMonth(2000, 4), is(30));
        assertThat(daysInMonth(2000, 5), is(31));
        assertThat(daysInMonth(2000, 6), is(30));
        assertThat(daysInMonth(2000, 7), is(31));
        assertThat(daysInMonth(2000, 8), is(31));
        assertThat(daysInMonth(2000, 9), is(30));
        assertThat(daysInMonth(2000, 10), is(31));
        assertThat(daysInMonth(2000, 11), is(30));
        assertThat(daysInMonth(2000, 12), is(31));
    }
    @Test public void testGetSetOf() {
        assertThat(     msOf(2019_02_03__04_05_06_007L), is(7));
        assertThat(secondsOf(2019_02_03__04_05_06_007L), is(6));
        assertThat(minutesOf(2019_02_03__04_05_06_007L), is(5));
        assertThat(  hoursOf(2019_02_03__04_05_06_007L), is(4));
        assertThat(   daysOf(2019_02_03__04_05_06_007L), is(3));
        assertThat( monthsOf(2019_02_03__04_05_06_007L), is(2));
        assertThat(  yearsOf(2019_02_03__04_05_06_007L), is(2019));

        assertThat(  yearsOf(  setYears(2000_01_01__00_00_00_000L, 2019)), is(2019));
        assertThat( monthsOf( setMonths(2000_01_01__00_00_00_000L,   11)), is(11));
        assertThat(   daysOf(   setDays(2000_01_01__00_00_00_000L,   23)), is(23));
        assertThat(  hoursOf(  setHours(2000_01_01__00_00_00_000L,   14)), is(14));
        assertThat(minutesOf(setMinutes(2000_01_01__00_00_00_000L,   45)), is(45));
        assertThat(secondsOf(setSeconds(2000_01_01__00_00_00_000L,   56)), is(56));
        assertThat(     msOf(     setMs(2000_01_01__00_00_00_000L,  789)), is(789));
    }
    @Test public void testConvertLocalDateTime() {
        final long t1 = getDateTime();
        sleep(100);
        assertThat(getDateTimeOf(toLocalDateTime(t1)), is(t1));
    }
}
