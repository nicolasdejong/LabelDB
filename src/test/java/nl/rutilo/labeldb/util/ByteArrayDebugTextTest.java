package nl.rutilo.labeldb.util;

import nl.rutilo.labeldb.util.ByteArrayDebugText;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class ByteArrayDebugTextTest {

    @Test
    public void testToDebugString() {
        final PrintStream stdout = System.out;
        final ByteArrayOutputStream bout = new ByteArrayOutputStream();
        final PrintStream tmpout = new PrintStream(bout);

        try {
            System.setOut(tmpout);
            final byte[] bytes = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 65, 66, 67, 68, 69, 70, 71};
            ByteArrayDebugText.print(bytes);
        } finally {
            System.setOut(stdout);
        }
        final String text = new String(bout.toByteArray());
        assertThat(text.trim(), is(
            "00000: 01 02 03 04 05 06 07 08 09 0a 0b 41 42 43 44 45  ...........ABCDE\n"
          + "00010: 46 47                                            FG"));
    }
}