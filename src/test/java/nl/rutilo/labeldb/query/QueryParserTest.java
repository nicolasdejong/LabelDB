package nl.rutilo.labeldb.query;

import org.junit.Test;

import static junit.framework.TestCase.fail;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class QueryParserTest {

    @Test public void testParse() {
        assertThat(qs("a b"),               is("AND(a, b)"));
        assertThat(qs("a,b"),               is("OR(a, b)"));
        assertThat(qs("a,b c"),             is("OR(a, AND(b, c))"));
        assertThat(qs("a & b"),             is("AND(a, b)"));
        assertThat(qs("a & b | c"),         is("OR(AND(a, b), c)"));
        assertThat(qs("a | b & c"),         is("OR(a, AND(b, c))"));
        assertThat(qs("(a | b) & c"),       is("AND(OR(a, b), c)"));
        assertThat(qs("a & b | c & d | e"), is("OR(OR(AND(a, b), AND(c, d)), e)"));
        assertThat(qs("!a & (c & d | e)"),  is("AND(NOT(a), OR(AND(c, d), e))"));
        assertThat(qs("!a | c & d)"),       is("OR(NOT(a), AND(c, d))"));
        assertThat(qs("a & >=2019"),        is("AND(a, >=20190101000000000)"));
        assertThat(qs("a 2016..2018"),      is("AND(a, AND(>=20160101000000000, <=20181231235959999))"));
        assertThat(qs("2016 & @12345"),     is("AND(AND(>=20160101000000000, <=20161231235959999), ID=12345)"));
        assertThat(qs("!(a | c & d) & e"),  is("AND(NOT(OR(a, AND(c, d))), e)"));
        assertThat(qs("a | !b & !c"),       is("OR(a, AND(NOT(b), NOT(c)))"));
    }

    @Test public void testIllegalTokens() {
        try {
            new QueryParser("a & & b");
            fail("Expected throw");
        } catch(final QueryException e) {
            assertThat(e.getMessage(), containsString("Expected operator"));
        }
        try {
            new QueryParser("a | ! | b");
            fail("Expected throw");
        } catch(final QueryException e) {
            assertThat(e.getMessage(), containsString("Unexpected operator"));
        }

    }

    private static String qs(String query) {
        return new QueryParser(query).toString();
    }
}