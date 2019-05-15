package nl.rutilo.util.testsupport;

import nl.rutilo.labeldb.util.Utils;

import java.io.File;
import java.io.IOException;

public class TestUtils {
    private TestUtils() {}

    public static long timed(String message, Runnable r) { return timed(message, 1, r); }
    public static long timed(String message, int repeatCount, Runnable r) {
        System.out.print(message + (repeatCount > 1 ? " x " + toUnderscoredString(repeatCount) : "") + "...");
        System.out.flush();
        long time;
        System.out.println(" took " + (time=timed(repeatCount, r)) + "ms");
        return time;
    }
    public static long timed(Runnable r) { return timed(1, r); }
    public static long timed(int repeatCount, Runnable r) {
        final long t1 = System.currentTimeMillis();
        for(int count=0; count<repeatCount; count++) r.run();
        final long t2 = System.currentTimeMillis();
        return t2 - t1;
    }

    public static void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            throw new RuntimeException("sleep interrupted");
        }
    }

    public static class DeletedWhenClosedFile extends File implements AutoCloseable {
        public DeletedWhenClosedFile(File source) {
            super(source.getAbsolutePath());
        }
        public void close() {
            if(isFile()) delete(); else Utils.deleteDirectory(this);
        }
    }

    public static DeletedWhenClosedFile createTempDir() {
        return new DeletedWhenClosedFile(Utils.createTempDir());
    }
    public static DeletedWhenClosedFile createTempFile() {
        try {
            return new DeletedWhenClosedFile(File.createTempFile("test", "tmp"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static String toUnderscoredString(int n) {
        return reverse(String.join("_", reverse(""+n).split("(?<=\\G...)")));
    }
    private static String reverse(String s) {
        return new StringBuilder(s).reverse().toString();
    }
}
