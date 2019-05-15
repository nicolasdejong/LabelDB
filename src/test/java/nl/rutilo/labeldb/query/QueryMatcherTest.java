package nl.rutilo.labeldb.query;

import nl.rutilo.labeldb.Bits;
import nl.rutilo.labeldb.Longs;
import nl.rutilo.labeldb.query.QueryMatcher.MatchResults;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;
import static org.junit.Assert.assertThat;

public class QueryMatcherTest {
    private Map<String, Bits> labels;
    private Longs dates;
    private QueryMatcher matcher;

    @Before public void setup() {
        labels = new HashMap<>();
        dates = new Longs();

        addResource( 1, 2018_01_08__01_12_13_111L               );
        addResource( 2, 2018_01_12__02_22_23_222L, "a"          );
        addResource( 3, 2018_01_16__03_32_33_333L,     "b"      );
        addResource( 4, 2018_01_20__04_42_43_444L,           "c");
        addResource( 5, 2018_01_24__05_52_53_555L, "a", "b"     );
        addResource( 6, 2018_01_28__06_56_56_666L, "a",      "c");
        addResource( 7, 2018_02_07__07_57_57_777L,      "b", "c");
        addResource( 8, 2018_03_08__08_58_58_888L, "a", "b", "c");

        addResource(11, 2019_01_08__01_12_13_111L               );
        addResource(12, 2019_01_12__02_22_23_222L, "a"          );
        addResource(13, 2019_01_16__03_32_33_333L,     "b"      );
        addResource(14, 2019_01_20__04_42_43_444L,           "c");
        addResource(15, 2019_01_24__05_52_53_555L, "a", "b"     );
        addResource(16, 2019_01_28__06_56_56_666L, "a",      "c");
        addResource(17, 2019_02_07__07_57_57_777L,      "b", "c");
        addResource(18, 2019_01_08__08_58_58_888L, "a", "b", "c");

        addResource(99, 2020_03_01__11_22_33_999L,               "d");

        matcher = new QueryMatcher(labels, dates);
    }

    private void addResource(int id, long date, String... labelNames) {
        dates.set(id, date);
        for(final String label : labelNames) {
            if (!labels.containsKey(label)) labels.put(label, new Bits());
            labels.get(label).set(id, true);
        }
    }
    private int[] match(String query) {
        return new QueryMatcher(labels, dates).match(query).getIndices();
    }

    @Test public void testMatch() {
        assertThat(match("()"), isAll(1, 2, 3, 4, 5, 6, 7, 8, 11, 12, 13, 14, 15, 16, 17, 18, 99));
        assertThat(match("@3, @6, @11"), isAll(3, 6, 11));

        assertThat(match("<2019"), isAll(1, 2, 3, 4, 5, 6, 7, 8 ));
        assertThat(match(">2018"), isAll(11, 12, 13, 14, 15, 16, 17, 18, 99 ));

        assertThat(match("a"),       isAll(2, 5, 6, 8,  12, 15, 16, 18 ));
        assertThat(match("a <2019"), isAll(2, 5, 6, 8 ));

        assertThat(match("a b"),       isAll(5, 8,  15, 18 ));
        assertThat(match("a b <2019"), isAll(5, 8 ));

        assertThat(match("a, b"), isAll(2, 3, 5, 6, 7, 8,  12, 13, 15, 16, 17, 18 ));
        assertThat(match("(a, b) <2019"), isAll(2, 3, 5, 6, 7, 8 ));
        assertThat(match("(a, b) <2018.2"), isAll(2, 3, 5, 6 ));

        assertThat(match("!(a, b)"), isAll(0, 1, 4, 9, 10, 11, 14 ));
        assertThat(match("!(a, b) <2019"), isAll(1, 4 ));
        assertThat(match("!(a, b) >=2018"), isAll(1, 4, 11, 14 ));

        assertThat(match("(a AND (b OR c))"), isAll( 5, 6, 8, 15, 16, 18));

        assertThat(match("@unlabeled"), isAll( 1, 11));
    }

    @Test public void testGetMatchResults() {
        final MatchResults r1 = matcher.getMatchResultsFor("(a AND (b OR c)) OR d");
        assertThat(r1.indices, isAll(5, 6, 8, 15, 16, 18, 99));
        assertThat(r1.resultCountPerLabel.size(), is(4));
        assertThat(r1.resultCountPerLabel.get("a"), is(6));
        assertThat(r1.resultCountPerLabel.get("b"), is(4));
        assertThat(r1.resultCountPerLabel.get("c"), is(4));
        assertThat(r1.resultCountPerLabel.get("d"), is(1));
    }

    private static Matcher<int[]> isAll(int... values) {
        return is(values);
    }
}