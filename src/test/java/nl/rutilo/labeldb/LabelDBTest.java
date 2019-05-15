package nl.rutilo.labeldb;

import org.junit.Test;

import java.io.File;
import java.util.Random;

import static nl.rutilo.util.testsupport.TestUtils.*;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class LabelDBTest {

    @Test public void testPerformance() {
        try(final DeletedWhenClosedFile tmpDir = createTempDir();
            final LabelDB db = new LabelDB(tmpDir)) {

            db.setAutoCommit(false);

            // label count and index count
            final String[] labels = new String[1000];
            final int index_label_count = 20;
            final int find_label_count = 20;
            final int max_index = 100_000;
            final Random rnd = new Random();

            // generate labels
            for(int i=0; i<labels.length; i++) labels[i] = "label-" + i;

            // add labels to indices
            final long addTime = timed("add labels to db", () -> {
                for (int index = 0; index <= max_index; index++) {
                    for (int j = 0; j < index_label_count; j++) {
                        final int labelIndex = rnd.nextInt(labels.length);
                        db.set(labels[labelIndex], index);
                    }
                }
            });

            final long searchTime = timed("query " + find_label_count + " labels", 10, () -> {
               final String[] searchLabels = new String[find_label_count];
               for(int i=0; i<searchLabels.length; i++) searchLabels[i] = labels[rnd.nextInt(labels.length)];
               db.find(String.join(" ", searchLabels));
            });
        }
    }

    @Test public void test() {
        try(final DeletedWhenClosedFile tmpDir = createTempDir();
            final LabelDB db = new LabelDB(tmpDir)) {

            db.setCommitDebounceMs(100);

            assertThat(db.firstUnusedIndex(), is(0));

            db.set(0, 2019_06_01__11_22_33_444L, "a", "b");
            db.set(1, 2019_06_02__11_22_33_444L, "a", "c");
            db.set(2, 2019_06_03__11_22_33_444L, "a", "c");
            db.set(3, 2019_06_04__11_22_33_444L, "a", "d");
            db.set(4, 2019_06_05__11_22_33_444L, "a", "d");
            db.set(5, 2019_06_06__11_22_33_444L, "a", "d");

            assertThat(db.firstUnusedIndex(), is(6));

            assertThat(db.find("a").indices.length, is(6));

            db.remove("a", 0, 1);

            assertThat(db.find("a").indices.length, is(4));

            db.set("a", 0, 1);

            assertThat(db.find("a").indices.length, is(6));

            db.clear(0);

            assertThat(db.find("a").indices.length, is(5));

            db.set(0, 2019_06_01__11_22_33_555L);
            db.set("a", 0, 1, 2);

            assertThat(db.find("a").indices.length, is(6));

            sleep(db.getCommitDebounceMs() );
            sleep(50); // give it time to write

            assertTrue(new File(tmpDir, "name_dates").exists());
            assertTrue(new File(tmpDir, "labels").exists());
            assertTrue(new File(tmpDir, "labels/name_a").exists());
            assertTrue(new File(tmpDir, "labels/name_b").exists());
            assertTrue(new File(tmpDir, "labels/name_c").exists());
            assertTrue(new File(tmpDir, "labels/name_d").exists());

            final LabelDB db2 = new LabelDB(tmpDir);

            assertThat(db2.find("a").indices.length, is(6));
            assertThat(db2.firstUnusedIndex(), is(6));
            db2.close();

            db.setAutoCommit(false);
            db.set("e", 4, 5);
            assertThat(db.find("a d e").indices.length, is(2));

            db.commit();

            final LabelDB db3 = new LabelDB(tmpDir);
            assertThat(db3.find("a d e").indices.length, is(2));
            assertThat(db3.firstUnusedIndex(), is(6));
            db3.close();
        }
    }
}