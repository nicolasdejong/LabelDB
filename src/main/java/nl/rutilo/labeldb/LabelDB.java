package nl.rutilo.labeldb;

import nl.rutilo.labeldb.query.QueryMatcher;
import nl.rutilo.labeldb.query.QueryMatcher.MatchResults;
import nl.rutilo.labeldb.util.FunctionalReadWriteLock;
import nl.rutilo.labeldb.util.Utils;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static nl.rutilo.labeldb.util.Utils.waitOn;

/** Thread safe persistent label index with search */
public class LabelDB implements AutoCloseable {
    private static final int    DEFAULT_WRITE_DEBOUNCE_TIME_MS = 2 * 1000;
    private static final String WRITE_THREAD_NAME = "LabelDBWriter";
    private static final int    WRITE_THREAD_PRIORITY = Thread.NORM_PRIORITY;
    private static final String LABELS_DIR_NAME = "labels";
    private static final String DATES_NAME = "dates";

    private final Map<String, Bits> labelBits = new HashMap<>();
    private final Longs dates;
    private final File labelsDir;

    private final FunctionalReadWriteLock lock = new FunctionalReadWriteLock();
    private final Set<String> alteredLabels = new HashSet<>();
    private final boolean[] datesChanged = { false };
    private final Thread writeThread;

    private int writeDebounceTime = DEFAULT_WRITE_DEBOUNCE_TIME_MS;
    private boolean autoCommit = true;
    private boolean stopped = false;
    private long lastWriteTime = 0;


    public LabelDB(File dir) {
        this.labelsDir = new File(dir, LABELS_DIR_NAME);

        dates = new Longs(DATES_NAME, dir);
        final File[] labelDirs = labelsDir.listFiles();
        if(labelDirs != null) for(final File labelDir : labelDirs) {
            final String label = Utils.filenameToName(labelDir.getName());
            labelBits.put(label, new Bits(label, labelsDir));
        }

        writeThread = new Thread(this::writeWhenChanged);
        writeThread.setName(WRITE_THREAD_NAME);
        writeThread.setPriority(WRITE_THREAD_PRIORITY);
        writeThread.start();
    }

    public LabelDB clear(int... indices) {
        lock.write(() -> {
            labelBits.values().forEach(bits -> bits.unset(indices));
            dates.unset(indices);
        });
        return this;
    }
    public int firstUnusedIndex() {
        return lock.read(dates::getFirstUnsetIndex);
    }
    public LabelDB set(int index, long datetime, String... labels) {
        for(final String label : labels) set(label, index);
        set(index, datetime);
        dataWasAltered();
        return this;
    }
    public LabelDB set(int index, long datetime) {
        lock.write(() -> {
            dates.set(index, datetime);
            datesChanged[0] = true;
            dataWasAltered();
        });
        return this;
    }
    public LabelDB set(String label, int... indices) {
        lock.write(() -> {
            makeSureLabelExists(label);
            final Bits bits = labelBits.get(label);
            bits.set(indices);
            alteredLabels.add(label);
            dataWasAltered();
        });
        return this;
    }
    public LabelDB remove(String label, int... indices) {
        lock.write(() -> {
            final Bits bits = labelBits.get(label);
            if(bits == null) return;
            bits.unset(indices);
            alteredLabels.add(label);
            dataWasAltered();
        });
        return this;
    }

    /** Search through the whole database for indices that fall within the
      * query results. See query documentation on the query notation.
      *
      * Examples:<pre>
      * - a b c
      * - a AND b AND c
      * - a, b, c
      * - a AND (b OR c)
      * - a b <2019.6
      */
    public MatchResults find(String query) {
        return lock.read(() -> new QueryMatcher(labelBits, dates).getMatchResultsFor(query));
    }


    /** Stops the write thread. After this call it is not allowed to alter the database */
    public void close() {
        commit();
        stopped = true;
        awakenWriteThread();
    }
    public LabelDB setAutoCommit(boolean set) {
        autoCommit = set;
        return this;
    }
    public LabelDB setCommitDebounceMs(int ms) {
        writeDebounceTime = ms;
        awakenWriteThread();
        return this;
    }
    public int getCommitDebounceMs() {
        return writeDebounceTime;
    }

    /** Normally a write occurs a few seconds after a change (debounced), but here
      * a write can be forced immediately. Only actually writes when data was altered.
      */
    public LabelDB commit() {
        synchronized(writeThread) {
            write();
        }
        return this;
    }

    ///

    private void makeSureLabelExists(String label) {
        if(!labelBits.containsKey(label)) {
            labelBits.put(label, new Bits(label, labelsDir));
        }
    }
    private void dataWasAltered() {
        if(stopped) throw new IllegalStateException("Cannot alter data when stopped");
        awakenWriteThread();
    }
    private void awakenWriteThread() {
        synchronized(writeThread) {
            writeThread.notifyAll();
        }
    }
    private void writeWhenChanged() {
        boolean needsWrite = false;
        while(!stopped) {
            synchronized(writeThread) {
                waitOn(writeThread, writeDebounceTime);

                if(autoCommit) {
                    final long now = System.currentTimeMillis();
                    final long writeAgo = now - lastWriteTime;
                    needsWrite = (!alteredLabels.isEmpty() || datesChanged[0])
                             && writeAgo > writeDebounceTime;
                }
            }
            if(needsWrite) { // outside sync
                write();
            }
        }
    }
    private void write() {
        lock.write(() -> {
            if(datesChanged[0]) dates.store();
            labelBits.forEach((name, bits) -> { if(alteredLabels.contains(name)) bits.store(); });
            lastWriteTime = System.currentTimeMillis();
            alteredLabels.clear();
            datesChanged[0] = false;
        });
    }
}
