package nl.rutilo.labeldb.util;

import org.hamcrest.Matchers;
import org.junit.Test;

import java.io.*;
import java.nio.file.NoSuchFileException;
import java.security.CodeSource;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Policy;
import java.util.*;

import static nl.rutilo.util.testsupport.TestUtils.*;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class UtilsTest {

    @Test public void testNameToFilename() {
        assertThat(Utils.nameToFilename("Label"), is("name_^label"));
        assertThat(Utils.nameToFilename("tab=\t;code=\u7F89;foo"), is("name_tab=%(9)%(3b)code=%(7f89)%(3b)foo"));
        assertThat(Utils.nameToFilename("AbcDef:=>"), is("name_^abc^def%(3a)=%(3e)"));
        assertThat(Utils.nameToFilename(""), is("name_"));
        assertThat(Utils.nameToFilename("%"), is("name_%(25)"));
    }
    @Test public void testFilenameToName() {
        String name;

        name = "Label";
        assertThat(Utils.filenameToName(Utils.nameToFilename(name)), is(name));

        name = "tab=\t;code=\u7F89;foo";
        assertThat(Utils.filenameToName(Utils.nameToFilename(name)), is(name));

        name = "AbcDef:=>";
        assertThat(Utils.filenameToName(Utils.nameToFilename(name)), is(name));

        name = "";
        assertThat(Utils.filenameToName(Utils.nameToFilename(name)), is(name));

        name = "%";
        assertThat(Utils.filenameToName(Utils.nameToFilename(name)), is(name));

    }

    @Test public void testWaitOn() {
        final Object obj = new Object();
        assertThat((int) timed(() -> Utils.waitOn(obj, 101)), Matchers.greaterThan(100));
        final Thread t = new Thread(() -> {
            try {
                Utils.waitOn(obj, 100);
                fail("Expected exception for being interrupted");
            } catch(final RuntimeException interrupted) {
                assertTrue(interrupted.getCause() instanceof InterruptedException);
            }
        });
        t.start();
        sleep(20);
        t.interrupt();
        sleep(150);
    }

    @Test public void testCreateTempDir() {
        final File tmpDir = Utils.createTempDir();
        assertTrue(tmpDir.exists());
        tmpDir.delete();

        final String tmpDirKey = "java.io.tmpdir";
        final String javaTmpDir = System.getProperty(tmpDirKey);
        try {
            System.setProperty(tmpDirKey, "1".repeat(1024)); // too long filename should lead to fail
            final File tmpDir2 = Utils.createTempDir();
            fail("Expected IllegalStateException");
        } catch(final IllegalStateException e) {
            assertThat(e.getMessage(), containsString("Failed to create directory"));
        } finally {
            System.setProperty(tmpDirKey, javaTmpDir);
        }
    }

    @Test public void testInputStreamToByteArray() {
        final byte[] testBytes = "abc".getBytes();
        final InputStream bin = new ByteArrayInputStream(testBytes);
        assertThat(Utils.toByteArray(bin).orElse(new byte[0]), is(testBytes));
    }
    @Test public void testCopyStream() {
        final byte[] testBytes = "abc".getBytes();
        final ByteArrayInputStream bin = new ByteArrayInputStream(testBytes);
        final ByteArrayOutputStream bout = new ByteArrayOutputStream();

        assertThat(Utils.copy(bin,  null), is(Optional.empty()));
        assertThat(Utils.copy(null, bout), is(Optional.empty()));
        assertThat(Utils.copy(null, null), is(Optional.empty()));
        assertThat(Utils.copy(bin, bout).orElse(null), is(bout));
        assertThat(bout.toByteArray(), is(testBytes));

        final InputStream throwBin = new InputStream() {
            public int read() throws IOException {
                throw new IOException("test throw");
            }
        };
        assertThat(Utils.copy(throwBin, new ByteArrayOutputStream()), is(Optional.empty()));
    }
    @Test public void testReadWriteByteArrayToFile() throws IOException {
        final byte[] testBytes = "abc".getBytes();
        final File tmpFile = File.createTempFile("tmp", "writeByteArrayToFile");
        try {
            Utils.writeByteArrayToFile(tmpFile, testBytes);
            assertThat(Utils.readFileToByteArray(tmpFile).orElse(new byte[0]), is(testBytes));
        } finally {
            tmpFile.delete();
        }
        try {
            Utils.writeByteArrayToFile(new File("1".repeat(1024)), testBytes);
            fail("Expected 'Unable to write' exception");
        } catch(final RuntimeException e) {
            assertThat(e.getMessage(), containsString("Unable to write"));
        }
        assertThat(Utils.readFileToByteArray(tmpFile), is(Optional.empty())); // file no longer exists
    }
    @Test public void testReadWriteStringToFile() throws IOException {
        final String testString = "abc";
        final File tmpFile = File.createTempFile("tmp", "writeByteArrayToFile");
        try {
            Utils.writeStringToFile(tmpFile, testString);
            assertThat(Utils.fileToString(tmpFile).orElse(""), is(testString));
        } finally {
            tmpFile.delete();
        }
    }

    @Test public void testDeleteDirectory() {
        final File tmpDir = Utils.createTempDir();
        new File(tmpDir, "a/b/c").mkdirs();
        new File(tmpDir, "a/b/d").mkdirs();

        assertTrue(new File(tmpDir, "a/b/c").exists());
        Utils.deleteDirectory(tmpDir);
        assertFalse(tmpDir.exists());

        try {
            Utils.deleteDirectoryImpl(tmpDir);
        } catch(final RuntimeException wrapper) {
            assertTrue(wrapper.getCause() instanceof IOException);
            assertTrue(wrapper.getCause() instanceof NoSuchFileException);
        }
    }

    @Test public void testSleep() {
        final Thread testThread = Thread.currentThread();

        final long t1 = System.currentTimeMillis();
        Utils.sleep(100);
        final long t2 = System.currentTimeMillis();
        assertTrue(t2 - t1 >= 100);


        new Thread(() -> {
            Utils.sleep(50);
            testThread.interrupt();
        }).start();

        try {
            Utils.sleep(500);
            fail("Excepted interruption");
        } catch(final RuntimeException interrupted) {
            assertTrue(interrupted.getCause() instanceof InterruptedException);
        }
    }

    @Test public void testOr() {
        String none = null;
        assertThat(Utils.or(none, null, "a", "b", null), is("a"));
        try {
            Utils.or(none, null, null);
            fail("Expected NullPointerException");
        } catch(final NullPointerException e) {
            // expected
        }
    }
}