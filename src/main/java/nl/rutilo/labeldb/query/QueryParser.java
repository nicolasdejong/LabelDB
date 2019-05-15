package nl.rutilo.labeldb.query;

import java.util.List;

import static nl.rutilo.labeldb.query.TokenType.*;

/**
 * Parses label queries.
 *
 * Possible forms:
 *
 * OR:   - N, N, N
 *       - N | N | N
 *       - N || N || N
 *       - N OR N OR N
 * AND   - N N N
 *       - N & N & N
 *       - N AND N AND N
 * NOT   - !N
 *       - NOT N
 * GROUP - (N)
 * N     - text
 *       - NUM
 *       - "spaced text"
 *       - (N)
 * DATE  - date
 *       - <date
 *       - <=date
 *       - >date
 *       - >=date
 *       - date..date
 *
 * Examples:
 *
 * A, B, C           -> A OR B OR C
 * A B C             -> A AND B AND C
 * A (B, C D)        -> A AND (B OR C AND D)
 * (A & B) | (C & D) -> (A AND B) OR (C AND D)
 */
public class QueryParser {
    final String query;
    final List<Token> tokens;
    final QueryNode tree;

    public QueryParser(String text) {
        tokens = new Tokenizer(query = text).get();
        tree = buildTree(nextNode());
    }
    public String toString() {
        return tree.toString();
    }

    private QueryNode buildTree(QueryNode left) {
        final QueryNode op = nextNode();
        if(op.token.type == GROUP_END) return left;
        if(!op.token.isOneOf(AND, OR)) throw new QueryException("Expected operator but found [" + op.token + "] after [" + left + "] in: \"" + query + "\"");
        if(op.token.type == AND && !left.isGroup && left.right != null) { // AND is stronger than OR
            op.left = left.right;
            op.right= nextNode();
            left.right = op;
            return buildTree(left);
        }
        op.left = left;
        op.right= nextNode();
        return buildTree(op);
    }

    private QueryNode nextNode() {
        if(!hasNextToken()) return new QueryNode(GROUP_END);
        final Token token = nextToken();
        final QueryNode node = new QueryNode(token);
        if (token.type == GROUP) {
            final QueryNode gnode = buildTree(nextNode());
            gnode.isGroup = true;
            return gnode;
        }
        if (token.type == NOT) {
            node.left = nextNode();
            if(node.left.token.isOneOf(AND, OR) && node.left.left == null) throw new QueryException("Unexpected operator after NOT for \"" + query + "\" just before " + tokens);
            node.isGroup = true;
        }
        return node;
    }

    private boolean hasNextToken() { return !tokens.isEmpty(); }
    private Token nextToken() { return tokens.remove(0); }
}
