package nl.rutilo.labeldb.query;

import nl.rutilo.labeldb.util.HumanReadableTime;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import static nl.rutilo.labeldb.query.TokenType.*;

public class Token {
    public final TokenType type;
    public final String text;
    public final long value;

    public Token(TokenType type, String text) {
        this.type = Objects.requireNonNull(type);
        this.text = text == null ? "" : text;
        this.value =
              isOneOf(LT_DATE, GTE_DATE)  ? toHrTime(this.text, /*minimized=*/true)
            : isOneOf(LTE_DATE, GT_DATE)  ? toHrTime(this.text, /*minimized=*/false)
            : type == ID                  ? Long.parseLong("0" + this.text.replaceAll("\\D", ""))
            : 0;
    }

    public String toString() {
        switch(type) {
            case TEXT:     return text;
            case LT_DATE:  return "<"  + value;
            case LTE_DATE: return "<=" + value;
            case GT_DATE:  return ">"  + value;
            case GTE_DATE: return ">=" + value;
            case ID:       return "ID=" + value;
            default:       return type.toString();
        }
    }

    public boolean isOneOf(TokenType... types) {
        for(final TokenType t : types) if(t == type) return true;
        return false;
    }

    public boolean isDate() {
        return isOneOf(LT_DATE, LTE_DATE, GT_DATE, GTE_DATE);
    }

    protected static long toHrTime(String s, boolean minimized) {
        // groups: yyyy mm dd hh mm ss iii
        //
        // - groups may be separated by non-digits
        // - all groups except year are optional
        // - no groups are allowed after a missing group
        // - any group may have a missing optional '0' prefix (or 2 missing for iii)
        //
        // Examples:
        // - 20190625
        // - 20196
        // - 2019.6.23,548
        // - 20190612-112233
        if(s == null || s.isEmpty()) return 0;
        return getGroups("^"
            + "[<>=\\s]*"
            + "(\\d{4}|\\d{1,3}$)"                 // 0: year
            + "(?:\\D?(1[012]|0?[1-9]))?"          // 1: month
            + "(?:\\D?([12][0-9]|3[01]|0?[1-9]))?" // 2: day
            + "(?:\\D?(1[0-9]|2[0123]|0?[0-9]))?"  // 3: hour
            + "(?:\\D?([12345][0-9]|0?[0-9]))?"    // 4: minute
            + "(?:\\D?([12345][0-9]|0?[0-9]))?"    // 5: second
            + "(?:\\D?(\\d{1,3}))?"                // 6: ms
            , s)
            .map(parts -> HumanReadableTime.toDateTime(
                toTimePart(parts[0], 0),                  // year
                toTimePart(parts[1], minimized ? 1 : 12), // month
                toTimePart(parts[2], minimized ? 1 : 31), // day
                toTimePart(parts[3], minimized ? 0 : 23), // hour
                toTimePart(parts[4], minimized ? 0 : 59), // min
                toTimePart(parts[5], minimized ? 0 : 59), // sec
                toTimePart(parts[6], minimized ? 0 : 999, ms -> ms + (ms.length() < 2 ? "00" : ms.length() < 3 ? "0" : "")) // ms
            )).orElse(0L);
    }

    private static long toTimePart(String in, long unsetValue) { return toTimePart(in, unsetValue, null); }
    private static long toTimePart(String in, long unsetValue, Function<String,String> mapper) {
        return in == null || !in.matches("^\\d+$") ? unsetValue : Long.parseLong(mapper == null ? in : mapper.apply(in));
    }

    private static Optional<String[]> getGroups(String regexp, String text) {
        final Matcher matcher = Pattern.compile(regexp).matcher(text);
        return Optional.ofNullable(matcher.matches() ? matcher.toMatchResult() : null)
            .map(mr -> IntStream.range(0, mr.groupCount()).mapToObj(i -> mr.group(i+1)).toArray(String[]::new));
    }
}
