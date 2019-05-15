package nl.rutilo.labeldb.query;

import nl.rutilo.labeldb.Bits;
import nl.rutilo.labeldb.Longs;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class QueryMatcher {
    private final Map<String, Bits> labels;
    private final Longs dates;

    public QueryMatcher(Map<String, Bits> labels, Longs dates) {
        this.labels = labels;
        this.dates = dates;
    }

    public static class MatchResults {
        public final int[] indices;
        public final Map<String, Integer> resultCountPerLabel;

        public MatchResults(Bits bits, Map<String, Integer> counts) {
            indices = bits.getIndices();
            resultCountPerLabel = Collections.unmodifiableMap(counts);
        }
    }

    public Bits match(String query) {
        final QueryNode tree = new QueryParser(query).tree;
        return match(getAll(), tree);
    }

    public MatchResults getMatchResultsFor(String query) {
        final Bits match = match(query);
        return new MatchResults(match, getCountPerLabelFor(match));
    }

    private Map<String, Integer> getCountPerLabelFor(Bits match) {
        final Map<String, Integer> counts = new HashMap<>();
        labels.forEach((label, bits) -> {
            final int count = bits.countOverlapWith(match);
            if(count > 0) counts.put(label, count);
        });
        return counts;
    }

    private Bits getAll() {
        return dates.asBits();
    }

    private Bits getUnlabeled() {
        final Bits bits = getAll();
        labels.values().forEach(bits::removeOverlapWith);
        return bits;
    }

    private Bits match(Bits bits, QueryNode node) {
        final Bits result;
        switch(node.token.type) {
//            case GROUP: {
//                result = match(bits, node.left);
//                break;
//            }
            case AND: {
                final Bits leftBits = match(bits, node.left);
                final Bits rightBits = match(bits, node.right);
                result = leftBits.retainOverlapWith(rightBits);
                break;
            }
            case OR: {
                final Bits leftBits = match(bits, node.left);
                final Bits rightBits = match(bits, node.right);
                result = leftBits.joinWith(rightBits);
                break;
            }
            case NOT: {
                final Bits leftBits = match(bits, node.left);
                result = leftBits.reverse();
                break;
            }
            default:
            case NOP: {
                result = bits;
                break;
            }
            case ID: {
                result = new Bits();
                result.set((int)node.token.value);
                break;
            }
            case UNLABELED: {
                result = getUnlabeled();
                break;
            }
            case TEXT: {
                final Bits labelBits = labels.get(node.token.text);
                result = labelBits == null ? new Bits() : labelBits.copy();
                break;
            }
            case LTE_DATE:
            case LT_DATE: {
                result = dates.asBits(0, node.token.value);
                break;
            }
            case GTE_DATE:
            case GT_DATE: {
                result = dates.asBits(node.token.value, Long.MAX_VALUE);
                break;
            }
        }
        return result;
    }
}
