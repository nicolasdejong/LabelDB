package nl.rutilo.labeldb.query;

import org.junit.Test;

import static nl.rutilo.labeldb.query.TokenType.*;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class TokenTest {

    @Test public void testConstructor() {
        try {
            new Token(null, null);
            fail("Expected np exception");
        } catch(final NullPointerException e) {
            // this is expected
        }
        assertThat(new Token(TEXT, null).text, is(""));
        assertThat(new Token(TEXT, "abc").text, is("abc"));
        assertThat(new Token(LT_DATE,  "201906").value, is(20190601000000000L));
        assertThat(new Token(GTE_DATE, "201906").value, is(20190601000000000L));
        assertThat(new Token(GT_DATE,  "201906").value, is(20190630235959999L));
        assertThat(new Token(LTE_DATE, "201906").value, is(20190630235959999L));
        assertThat(new Token(ID, "12345").value, is(12345L));
    }

    @Test public void testToString() {
        final String date = "20190101000000000";
        assertThat(new Token(TEXT, null).toString(), is(""));
        assertThat(new Token(AND,  null).toString(), is("AND"));
        assertThat(new Token(TEXT, "ab").toString(), is("ab"));
        assertThat(new Token(LT_DATE,  date).toString(), is("<" + date));
        assertThat(new Token(LTE_DATE, date).toString(), is("<=" + date));
        assertThat(new Token(GT_DATE,  date).toString(), is(">" + date));
        assertThat(new Token(GTE_DATE, date).toString(), is(">=" + date));
        assertThat(new Token(AND,    "&").toString(), is("AND"));
        assertThat(new Token(OR,     "|").toString(), is("OR"));
        assertThat(new Token(NOT,   null).toString(), is("NOT"));
        assertThat(new Token(GROUP, null).toString(), is("GROUP"));
        assertThat(new Token(GROUP_END, null).toString(), is("GROUP_END"));
        assertThat(new Token(ID,    "123").toString(), is("ID=123"));
    }
    @Test public void testIsOneOf() {
        assertTrue(new Token(OR,   null).isOneOf(OR, AND, TEXT));
        assertTrue(new Token(AND,  null).isOneOf(OR, AND, TEXT));
        assertTrue(new Token(TEXT, null).isOneOf(OR, AND, TEXT));
    }
    @Test public void testIsNumber() {
        assertTrue(new Token(LTE_DATE, null).isDate());
    }

    @Test public void testToHrTime() {
        assertThat(Token.toHrTime("2019", false),               is(2019_12_31__23_59_59_999L));
        assertThat(Token.toHrTime("2019", true),                is(2019_01_01__00_00_00_000L));
        assertThat(Token.toHrTime("20196", false),              is(2019_06_30__23_59_59_999L));
        assertThat(Token.toHrTime("20196", true),               is(2019_06_01__00_00_00_000L));
        assertThat(Token.toHrTime("2019.6", true),              is(2019_06_01__00_00_00_000L));
        assertThat(Token.toHrTime("201906", true),              is(2019_06_01__00_00_00_000L));
        assertThat(Token.toHrTime("2019.06", true),             is(2019_06_01__00_00_00_000L));
        assertThat(Token.toHrTime("201965", true),              is(2019_06_05__00_00_00_000L));
        assertThat(Token.toHrTime("2019.6.5", true),            is(2019_06_05__00_00_00_000L));
        assertThat(Token.toHrTime("20190605", true),            is(2019_06_05__00_00_00_000L));
        assertThat(Token.toHrTime("2019.06.05", true),          is(2019_06_05__00_00_00_000L));
        assertThat(Token.toHrTime("2019999", true),             is(2019_09_09__09_00_00_000L));
        assertThat(Token.toHrTime("2019.1.2", true),            is(2019_01_02__00_00_00_000L));
        assertThat(Token.toHrTime("2019.12", true),             is(2019_12_01__00_00_00_000L));
        assertThat(Token.toHrTime("201912", true),              is(2019_12_01__00_00_00_000L));
        assertThat(Token.toHrTime("20191227-11:22:33", true),   is(2019_12_27__11_22_33_000L));
        assertThat(Token.toHrTime("20191227-1:2:3", true),      is(2019_12_27__01_02_03_000L));
        assertThat(Token.toHrTime("20191227-123", true),        is(2019_12_27__12_03_00_000L));
        assertThat(Token.toHrTime("20191227-123456.789", true), is(2019_12_27__12_34_56_789L));

        assertThat(Token.toHrTime("20191227-123456.1", true), is(2019_12_27__12_34_56_100L));
        assertThat(Token.toHrTime("20191227-123456.12", true), is(2019_12_27__12_34_56_120L));
        assertThat(Token.toHrTime("20191227-123456.123", true), is(2019_12_27__12_34_56_123L));
    }

    @Test public void testId() {
        assertThat(new Token(ID, "@1234").value, is(1234L));
    }
    @Test public void testDates() {
        assertThat(new Token(LT_DATE,   "<201906").toString(), is( "<20190601000000000"));
        assertThat(new Token(LTE_DATE, "<=201906").toString(), is("<=20190630235959999"));
        assertThat(new Token(GT_DATE,   ">201906").toString(), is( ">20190630235959999"));
        assertThat(new Token(GTE_DATE, ">=201906").toString(), is(">=20190601000000000"));
    }
}