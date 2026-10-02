package com.safari.wildlife.service.observer;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * DESIGN PATTERN: Observer Pattern (Concrete Subject)
 * Justification: BookingService, BookingManagerService and PaymentService used to call the notification,
 * e-mail and audit services directly after every status change. Now they only announce the event here,
 * and each attached observer decides how to react. A new reaction (e.g. SMS alerts) is added as a new
 * observer class without changing the booking or payment services.
 */
@Component
public class BookingEventPublisher implements BookingEventSubject {

    // Thread-safe: many requests can publish events at the same time
    private final List<BookingEventObserver> observers = new CopyOnWriteArrayList<>();

    /** Spring passes in every BookingEventObserver bean (in @Order order) and we attach each one. */
    public BookingEventPublisher(List<BookingEventObserver> initialObservers) {
        initialObservers.forEach(this::attach);
    }

    @Override
    public void attach(BookingEventObserver observer) {
        if (!observers.contains(observer)) {
            observers.add(observer);
        }
    }

    @Override
    public void detach(BookingEventObserver observer) {
        observers.remove(observer);
    }

    @Override
    public void notifyObservers(BookingEvent event) {
        for (BookingEventObserver observer : observers) {
            observer.update(event);
        }
    }

    /** Read-only view, used by tests and for explaining which observers are registered. */
    public List<BookingEventObserver> getObservers() {
        return List.copyOf(observers);
    }
}
