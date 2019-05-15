package nl.rutilo.labeldb.query;

import java.util.ArrayList;
import java.util.List;

import static nl.rutilo.labeldb.query.TokenType.*;

public class Tokenizer {
    private final String text;
    private final int len;
    private final StringBuilder buffer = new StringBuilder();
    private int pos;
    private char c;

    public Tokenizer(String text) {
        this.text = text.trim();
        len = this.text.length();
        pos = -1;
        next();
    }

    private boolean done()         { return pos >= len; }
    private void    next()         { pos++; c = done() ? 0 : text.charAt(pos); }
    private void    add()          { buffer.append(c); }
    private void    addNext()      { add(); next(); }
    private void skipWhitespaces() { while(isWhitespace()) next(); }
    private boolean isWhitespace() { return Character.isWhitespace(c); }
    private boolean isLabelChar()  { return (Character.isLetterOrDigit(c) || (c >= 33 && c <= 126)) && !isQuoteChar() && !isComparator() && !isOperator() && !isGroupStart() && !isGroupEnd(); }
    private boolean isQuoteChar()  { return c == '"' || c == '\'' || c == '`'; }
    private boolean isEscapeChar() { return c == '\\'; }
    private boolean isComparator() { return "<>=".indexOf(c) >=0; }
    private boolean isOperator()   { return "!|&,".indexOf(c) >=0; }
    private boolean isGroupStart() { return c == '('; }
    private boolean isGroupEnd()   { return c == ')'; }

    private Token nextToken() {
        final Token token;
        buffer.setLength(0);
        skipWhitespaces();

        if(isQuoteChar()) {
            next();
            while(!done() && !isQuoteChar()) {
                if(isEscapeChar()) next();
                addNext();
            }
            next(); // skip ending quote char
            token = new Token(TEXT, buffer.toString());
        } else
        if(isGroupStart()) {
            next();
            token = new Token(GROUP, "(");
        } else
        if(isGroupEnd()) {
            next();
            token = new Token(GROUP_END, ")");
        } else
        if(isOperator()) {
            char op = c;
            while(!done() && c == op) next();
            switch(op) {
                default: // default never happens but is required for 'final' token
                case ',':
                case '|': token = new Token(OR, "|"); break;
                case '&': token = new Token(AND, "&"); break;
                case '!': token = new Token(NOT, "!"); break;
            }
        } else
        if(isComparator()) {
            while(!done() && isComparator()) addNext();
            final String cmpText = buffer.toString();

            skipWhitespaces();
            buffer.setLength(0);
            while(!done() && !isWhitespace() && !isOperator()) addNext();
            final String rest = buffer.toString();

            if(cmpText.matches("[<>]=?") && rest.matches("^[\\d-.:]+$")) {
                if(rest.contains("..")) throw new QueryException(text, pos, "Prefix operator not allowed with ranges: " + rest);
                switch (cmpText) {
                    case "<":  token = new Token(LT_DATE, rest);  break;
                    case "<=": token = new Token(LTE_DATE, rest); break;
                    case ">":  token = new Token(GT_DATE, rest);  break;
                    default:   token = new Token(GTE_DATE, rest); break;
                }
            } else {
                token = new Token(TEXT, cmpText + rest);
            }
        } else
        if(isLabelChar()) {
            while(!done() && isLabelChar()) addNext();
            final String label = buffer.toString();
            if(label.equalsIgnoreCase("OR") || label.equals(",")) token = new Token(OR, label); else
            if(label.equalsIgnoreCase("AND")) token = new Token(AND, label); else
            if(label.equalsIgnoreCase("NOT")) token = new Token(NOT, label); else
            if(label.matches("^@\\d+"))       token = new Token(ID, label.substring(1)); else
            if(label.matches("(?i)^@(unlabeled|nolabel)$")) token = new Token(UNLABELED, label.substring(1)); else
                token = new Token(TEXT, label);
        } else {
            throw new QueryException(text, pos, "Unexpected character: " + c + " (" + (int)c + ")");
        }
        return token;
    }

    public List<Token> get() {
        final List<Token> tokens = new ArrayList<>();
        boolean foundOperator = true;

        while(!done()) {
            final Token token = nextToken();
            if(token != null) {
                if (!foundOperator && !token.isOneOf(AND, OR) && !token.isOneOf(GROUP_END)) tokens.add(new Token(AND, " "));
                foundOperator = token.isOneOf(AND, OR, NOT, GROUP);

                // Some tokens need to be split into multiple tokens

                // Date range
                if(token.type == TEXT && token.text.matches("^\\d[\\d:.-]+$") && token.text.contains("..")) {
                    final String[] parts = token.text.split("\\.{2,}");
                    if (parts.length != 2) throw new QueryException(text, pos, "Illegal range: " + token.text);
                    tokens.add(new Token(GROUP, "("));
                    tokens.add(new Token(GTE_DATE, "" + Token.toHrTime(parts[0], /*min=*/true)));
                    tokens.add(new Token(AND, "&"));
                    tokens.add(new Token(LTE_DATE, "" +  Token.toHrTime(parts[1], /*min=*/false)));
                    tokens.add(new Token(GROUP_END, ")"));
                } else
                if(token.type == TEXT && token.text.matches("^\\d[\\d:.-]{3,}$")) {
                    tokens.add(new Token(GROUP, "("));
                    tokens.add(new Token(GTE_DATE, "" + Token.toHrTime(token.text, /*min=*/true)));
                    tokens.add(new Token(AND, "&"));
                    tokens.add(new Token(LTE_DATE, "" +  Token.toHrTime(token.text, /*min=*/false)));
                    tokens.add(new Token(GROUP_END, ")"));
                } else {
                    tokens.add(token);
                }
            }
        }
        return tokens;
    }
}
