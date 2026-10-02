package com.safari.wildlife.service.observer;

/**
 * DESIGN PATTERN: Observer Pattern (Observer role)
 * Every class that wants to react to booking events implements this interface.
 * The Subject (BookingEventPublisher) calls update() on each attached observer.
 */
public interface BookingEventObserver {
    void update(BookingEvent event);
}
