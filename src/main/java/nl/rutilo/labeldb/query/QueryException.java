package nl.rutilo.labeldb.query;

public class QueryException extends RuntimeException {
    public QueryException(String query, int position, String message) {
        super("Error in query: " + message
            + "\nQuery: " + query.replace("\n", "\\n")
            + "\n       " + " ".repeat(position + query.substring(0, position).replaceAll("[^\n]","").length()) + "^");
    }
    public QueryException(String message) {
        super(message);
    }
}
