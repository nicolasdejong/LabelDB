package nl.rutilo.labeldb.query;

class QueryNode {
    QueryNode left = null;
    QueryNode right = null;
    boolean isGroup = false;
    final Token token;

    public QueryNode(Token token) { this.token = token; }
    public QueryNode(TokenType type) { this.token = new Token(type, ""); }
    public String toString() {
        return (token == null ? "NOP" : ""
             + token.toString())
             + (left != null || right != null
                 ? ("(" + (left  == null ? "" : left.toString())
                        + (right == null ? "" : ", " + right.toString())
                   + ")")
                 : ""
               );
    }
}
