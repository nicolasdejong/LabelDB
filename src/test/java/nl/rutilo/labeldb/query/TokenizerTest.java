package nl.rutilo.labeldb.query;

import org.junit.Test;

import java.util.List;

import static nl.rutilo.labeldb.query.TokenType.*;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class TokenizerTest {

    @Test public void testTokenTypes() {
        assertThat(tokenize("a b c"), is(array(TEXT, AND, TEXT, AND, TEXT)));
        assertThat(tokenize("a,b,c"), is(array(TEXT, OR, TEXT, OR, TEXT)));
        assertThat(tokenize("a AND b  AND     c"), is(array(TEXT, AND, TEXT, AND, TEXT)));
        assertThat(tokenize("a AND b OR c"), is(array(TEXT, AND, TEXT, OR, TEXT)));
        assertThat(tokenize("a >2012 >= 2013 <2014 <=2015"), is(array(TEXT, AND, GT_DATE, AND, GTE_DATE, AND, LT_DATE, AND, LTE_DATE)));
        assertThat(tokenize("a (b, c)"), is(array(TEXT, AND, GROUP, TEXT, OR, TEXT, GROUP_END)));
        assertThat(tokenize("a AND (b OR c)"), is(array(TEXT, AND, GROUP, TEXT, OR, TEXT, GROUP_END)));
        assertThat(tokenize("a!(b,c)d"), is(array(TEXT, AND, NOT, GROUP, TEXT, OR, TEXT, GROUP_END, AND, TEXT)));
        assertThat(tokenize("a && NOT (b|c) & d"), is(array(TEXT, AND, NOT, GROUP, TEXT, OR, TEXT, GROUP_END, AND, TEXT)));
        assertThat(tokenize("a(<2019-12-31.23:54,c)d"), is(array(TEXT, AND, GROUP, LT_DATE, OR, TEXT, GROUP_END, AND, TEXT)));
        assertThat(tokenize("a(<2019|> 201902 c)d"), is(array(TEXT, AND, GROUP, LT_DATE, OR, GT_DATE, AND, TEXT, GROUP_END, AND, TEXT)));
        assertThat(tokenize("2016..2018"), is(array(GROUP, GTE_DATE, AND, LTE_DATE,GROUP_END)));
        assertThat(tokenize("a b 2016..2018"), is(array(TEXT, AND, TEXT, AND, GROUP, GTE_DATE, AND, LTE_DATE,GROUP_END)));
        assertThat(tokenize("a `b c d` e"), is(array(TEXT, AND, TEXT, AND, TEXT)));
        assertThat(tokenize("@123,@456,@789"), is(array(ID, OR, ID, OR, ID)));
        assertThat(tokenize("@Unlabeled >=2019"), is(array(UNLABELED, AND, GTE_DATE)));
        assertThat(tokenize("2016 italy"), is(array(GROUP, GTE_DATE, AND, LTE_DATE, GROUP_END, AND, TEXT)));
    }
    @Test public void testTokenText() {
        final List<Token> tokens = new Tokenizer("a \"quoted \\\" escaped\" =b '123' `>456`").get();
        assertThat(tokens.size(), is(9));

        assertThat(tokens.get(0).type, is(TEXT));
        assertThat(tokens.get(0).text, is("a"));

        assertThat(tokens.get(1).type, is(AND));

        assertThat(tokens.get(2).type, is(TEXT));
        assertThat(tokens.get(2).text, is("quoted \" escaped"));

        assertThat(tokens.get(3).type, is(AND));

        assertThat(tokens.get(4).type, is(TEXT));
        assertThat(tokens.get(4).text, is("=b"));

        assertThat(tokens.get(5).type, is(AND));

        assertThat(tokens.get(6).type, is(TEXT));
        assertThat(tokens.get(6).text, is("123"));

        assertThat(tokens.get(7).type, is(AND));

        assertThat(tokens.get(8).type, is(TEXT));
        assertThat(tokens.get(8).text, is(">456"));
    }
    @Test public void testQueryErrors() {
        assertThat(runThrowingQuery("a b \u007F"), containsString("Unexpected character:"));
        assertThat(runThrowingQuery("a b 2016..2017..2018"), containsString("Illegal range"));
        assertThat(runThrowingQuery("a b >2016..2018"), containsString("Prefix operator not allowed"));
    }
    @Test public void testDateTokens() {
        assertThat(new Tokenizer("> 201906").get().get(0).toString(), is( ">20190630235959999"));
        assertThat(new Tokenizer(">=201906").get().get(0).toString(), is(">=20190601000000000"));
        assertThat(new Tokenizer("< 201906").get().get(0).toString(), is( "<20190601000000000"));
        assertThat(new Tokenizer("<=201906").get().get(0).toString(), is("<=20190630235959999"));
    }

    private static TokenType[] array(TokenType... types) { return types; }
    private static TokenType[] tokenize(String query) {
        return new Tokenizer(query).get().stream().map(token -> token.type).toArray(TokenType[]::new);
    }
    private static String runThrowingQuery(String query) {
        try {
            new Tokenizer(query).get();
            fail("Expected query exception for query: " + query);
            return ""; // never get here
        } catch(final QueryException e) {
            return e.getMessage();
        }
    }
}
