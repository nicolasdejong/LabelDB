package nl.rutilo.labeldb.query;

import org.junit.Test;

import static nl.rutilo.labeldb.query.TokenType.AND;
import static nl.rutilo.labeldb.query.TokenType.TEXT;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class QueryNodeTest {

    @Test public void testToString() {
        assertThat(new QueryNode((Token)null).toString(), is("NOP"));
        assertThat(new QueryNode(AND).toString(), is("AND"));
        assertThat(new QueryNode(new Token(TEXT, "abc")).toString(), is("abc"));
        assertThat(qn("left", "right").toString(), is("AND(left, right)"));
        assertThat(qn(null, "right").toString(), is("AND(, right)"));
        assertThat(qn("left", null).toString(), is("AND(left)"));
    }

    private QueryNode qn(String left, String right) {
        final QueryNode node = new QueryNode(new Token(AND, "&"));
        node.left  = left  == null ? null : new QueryNode(new Token(TEXT, left));
        node.right = right == null ? null : new QueryNode(new Token(TEXT, right));
        return node;
    }
}