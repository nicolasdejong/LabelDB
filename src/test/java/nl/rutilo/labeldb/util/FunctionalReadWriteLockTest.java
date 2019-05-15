package nl.rutilo.labeldb.util;

import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static junit.framework.TestCase.fail;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@SuppressWarnings("ConstantConditions")
public class FunctionalReadWriteLockTest {

    @Test public void testSupplier() {
        final FunctionalReadWriteLock rw = new FunctionalReadWriteLock();
        final int[] data = { 0 };
        final Waits waits = new Waits();

        fork(() -> {
            assertThat(rw.write(() -> {
                waits.set("w1");
                waits.waitFor("r1");
                assertThat(data[0], is(0));
                data[0] = 1;
                return data[0];
            }), is(1));
        });
        fork(() -> {
            waits.waitFor("w1");
            assertThat(rw.write(() -> {
                waits.set("w2");
                assertThat(data[0], is(1));
                data[0] = 2;
                return data[0];
            }), is(2));
        });
        waits.waitFor("w1");
        waits.set("r1");
        waits.waitFor("w2");
        assertThat(rw.read(() -> {
            assertThat(data[0], is(2));
            return data[0];
        }), is(2));

        try {
            rw.read(() -> {
                if(true) throw new RuntimeException("test throw");
                return 0;
            });
            fail("Expected exception");
        } catch(final Exception e) {
            assertThat(e.getMessage(), is("test throw"));
        }
        try {
            rw.write(() -> {
                if(true) throw new RuntimeException("test throw");
                return 0;
            });
            fail("Expected exception");
        } catch(final Exception e) {
            assertThat(e.getMessage(), is("test throw"));
        }
    }
    @Test public void testRunnable() {
        final FunctionalReadWriteLock rw = new FunctionalReadWriteLock();
        final int[] data = { 0 };
        final Waits waits = new Waits();

        fork(() -> {
            rw.write(() -> {
                waits.set("w1");
                waits.waitFor("r1");
                assertThat(data[0], is(0));
                data[0] = 1;
            });
        });
        fork(() -> {
            waits.waitFor("w1");
            rw.write(() -> {
                waits.set("w2");
                assertThat(data[0], is(1));
                data[0] = 2;
            });
        });
        waits.waitFor("w1");
        waits.set("r1");
        waits.waitFor("w2");
        rw.read(() -> {
            assertThat(data[0], is(2));
        });

        try {
            rw.read(() -> {
                throw new RuntimeException("test throw");
            });
            fail("Expected exception");
        } catch(final Exception e) {
            assertThat(e.getMessage(), is("test throw"));
        }
        try {
            rw.write(() -> {
                throw new RuntimeException("test throw");
            });
            fail("Expected exception");
        } catch(final Exception e) {
            assertThat(e.getMessage(), is("test throw"));
        }
    }

    static class Waits {
        private final Set<String> names = new HashSet<>();
        public void waitFor(String name) {
            synchronized(names) {
                while (!names.contains(name)) {
                    try {
                        names.wait(500);
                    } catch (final InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        public void set(String name) {
            synchronized (names) {
                names.add(name);
                names.notifyAll();
            }
        }
    }
    private static void fork(Runnable r) {
        new Thread(r).start();
    }
}