package com.safari.wildlife.service.observer;

/**
 * DESIGN PATTERN: Observer Pattern (Subject role)
 * Keeps a list of observers and notifies all of them when a booking event happens.
 */
public interface BookingEventSubject {
    void attach(BookingEventObserver observer);
    void detach(BookingEventObserver observer);
    void notifyObservers(BookingEvent event);
}
