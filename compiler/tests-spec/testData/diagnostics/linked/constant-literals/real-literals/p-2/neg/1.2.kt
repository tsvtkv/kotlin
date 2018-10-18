/*
 * KOTLIN DIAGNOSTICS SPEC TEST (NEGATIVE)
 *
 * SECTIONS: constant-literals, real-literals
 * PARAGRAPH: 2
 * SENTENCE: [1] The exponent is an exponent mark (e or E) followed by an optionaly signed decimal integer (a sequence of decimal digits).
 * NUMBER: 2
 * DESCRIPTION: Real literals with a not allowed exponent mark at the beginning.
 */

// TESTCASE NUMBER: 1
val value_1 = <!UNRESOLVED_REFERENCE!>E0<!>

// TESTCASE NUMBER: 2
val value_2 = <!UNRESOLVED_REFERENCE!>e000<!>

// TESTCASE NUMBER: 3
val value_3 = <!UNRESOLVED_REFERENCE!>E<!><!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>+<!>0

// TESTCASE NUMBER: 4
val value_4 = <!UNRESOLVED_REFERENCE!>e00<!>

// TESTCASE NUMBER: 5
val value_5 = <!UNRESOLVED_REFERENCE!>e<!><!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>+<!>1

// TESTCASE NUMBER: 6
val value_6 = <!UNRESOLVED_REFERENCE!>e22<!>

// TESTCASE NUMBER: 7
val value_7 = <!UNRESOLVED_REFERENCE!>E<!><!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>-<!>333

// TESTCASE NUMBER: 8
val value_8 = <!UNRESOLVED_REFERENCE!>e4444<!>

// TESTCASE NUMBER: 9
val value_9 = <!UNRESOLVED_REFERENCE!>e<!><!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>-<!>55555

// TESTCASE NUMBER: 10
val value_10 = <!UNRESOLVED_REFERENCE!>e666666<!>

// TESTCASE NUMBER: 11
val value_11 = <!UNRESOLVED_REFERENCE!>E7777777<!>

// TESTCASE NUMBER: 12
val value_12 = <!UNRESOLVED_REFERENCE!>e<!><!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>-<!>88888888

// TESTCASE NUMBER: 13
val value_13 = <!UNRESOLVED_REFERENCE!>E<!><!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>+<!>999999999