package dev.sabti.alumni_connect.mentoring;

// The full set is declared now, including the states only the session lifecycle will reach:
// growing a checked enum column later costs a migration for no benefit.
public enum BookingState {
    BOOKED,
    CONFIRMED,
    RELEASED_UNCONFIRMED,
    CANCELLED_BY_STUDENT,
    CANCELLED_BY_MENTOR,
    ATTENDED,
    NO_SHOW
}
