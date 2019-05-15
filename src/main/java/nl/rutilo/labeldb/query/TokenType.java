package nl.rutilo.labeldb.query;

public enum TokenType {
    TEXT, ID, UNLABELED,
    OR, AND, NOT, NOP,
    GROUP, GROUP_END,
    LT_DATE, LTE_DATE, GT_DATE, GTE_DATE
}
