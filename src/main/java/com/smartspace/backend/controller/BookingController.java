package com.smartspace.backend.controller;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.smartspace.backend.model.Booking;
import com.smartspace.backend.model.Space;
import com.smartspace.backend.model.User;
import com.smartspace.backend.repository.BookingRepository;
import com.smartspace.backend.repository.SpaceRepository;
import com.smartspace.backend.repository.UserRepository;
import com.smartspace.backend.service.BookingService;

@RestController
@RequestMapping("/api/bookings")
@CrossOrigin(origins = {
        "http://localhost:5173",
        "http://localhost:5174"
})
public class BookingController {

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final SpaceRepository spaceRepository;
    private final BookingService bookingService;

    public BookingController(
            BookingRepository bookingRepository,
            UserRepository userRepository,
            SpaceRepository spaceRepository,
            BookingService bookingService) {

        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.spaceRepository = spaceRepository;
        this.bookingService = bookingService;
    }

    @GetMapping
    public ResponseEntity<?> getMyBookings(
            Authentication authentication) {

        User user = getAuthenticatedUser(authentication);

        if (user == null) {
            return unauthorized(
                    "Please log in to view your bookings.");
        }

        List<Booking> bookings =
                bookingRepository.findAllByUser_Id(user.getId());

        return ResponseEntity.ok(bookings);
    }

    @GetMapping("/availability")
    public ResponseEntity<?> getAvailability(
            @RequestParam Long spaceId,
            @RequestParam LocalDate bookingDate,
            @RequestParam LocalTime startTime,
            @RequestParam Integer durationHours) {

        if (spaceId < 1
                || durationHours < 1
                || durationHours > 12) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "message",
                            "Invalid room or booking duration."));
        }

        if (bookingDate.isBefore(LocalDate.now())) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "message",
                            "Booking date cannot be in the past."));
        }

        LocalTime endTime = startTime.plusHours(durationHours);

        if (!endTime.isAfter(startTime)) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "message",
                            "Bookings cannot extend past midnight."));
        }

        Space space = spaceRepository.findById(spaceId)
                .orElse(null);

        if (space == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "message",
                            "The selected room does not exist."));
        }

        List<Booking> existingBookings =
                bookingRepository.findActiveBookingsForSpaceAndDate(
                        spaceId, bookingDate);

        int overlappingMembers = 0;
        boolean privateRoomBooked = false;

        for (Booking booking : existingBookings) {

            if (booking.getStartTime() == null
                    || booking.getDurationHours() == null) {
                continue;
            }

            LocalTime existingStart = booking.getStartTime();
            LocalTime existingEnd =
                    existingStart.plusHours(booking.getDurationHours());

            boolean overlaps =
                    startTime.isBefore(existingEnd)
                    && endTime.isAfter(existingStart);

            if (overlaps) {
                privateRoomBooked = true;

                overlappingMembers += booking.getMemberCount() == null
                        ? 1
                        : booking.getMemberCount();
            }
        }

        boolean shared =
                "SHARED".equalsIgnoreCase(space.getBookingType());

        int availableSeats = shared
                ? Math.max(0, space.getCapacity() - overlappingMembers)
                : (privateRoomBooked ? 0 : space.getCapacity());

        Map<String, Object> result = new HashMap<>();
        result.put("spaceId", space.getId());
        result.put("roomName", space.getTitle());
        result.put("bookingDate", bookingDate);
        result.put("startTime", startTime);
        result.put("endTime", endTime);
        result.put("capacity", space.getCapacity());
        result.put("bookingType", space.getBookingType());
        result.put("bookedMembers", overlappingMembers);
        result.put("availableSeats", availableSeats);
        result.put("available", availableSeats > 0);

        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getBookingById(
            @PathVariable Long id,
            Authentication authentication) {

        User user = getAuthenticatedUser(authentication);

        if (user == null) {
            return unauthorized(
                    "Please log in to view this booking.");
        }

        return bookingRepository.findById(id)
                .filter(booking ->
                        booking.getUser() != null
                        && booking.getUser().getId()
                                .equals(user.getId()))
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() ->
                        ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createBooking(
            @RequestBody Booking booking,
            Authentication authentication) {

        User user = getAuthenticatedUser(authentication);

        if (user == null) {
            return unauthorized(
                    "Please log in to create a booking.");
        }

        try {
            Booking savedBooking =
                    bookingService.createBooking(booking, user);

            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(savedBooking);

        } catch (ResponseStatusException exception) {

            HttpStatus status = HttpStatus.valueOf(
                    exception.getStatusCode().value());

            String message = exception.getReason() != null
                    ? exception.getReason()
                    : "Unable to create the booking.";

            return ResponseEntity.status(status)
                    .body(Map.of("message", message));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> cancelBooking(
            @PathVariable Long id,
            Authentication authentication) {

        User user = getAuthenticatedUser(authentication);

        if (user == null) {
            return unauthorized(
                    "Please log in to cancel a booking.");
        }

        Booking booking = bookingRepository.findById(id)
                .filter(existingBooking ->
                        existingBooking.getUser() != null
                        && existingBooking.getUser().getId()
                                .equals(user.getId()))
                .orElse(null);

        if (booking == null) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(Map.of(
                            "message",
                            "Booking not found or you do not have "
                                    + "permission to cancel it."));
        }

        if ("CANCELLED".equalsIgnoreCase(
                booking.getStatus())) {

            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "message",
                            "This booking has already been cancelled."));
        }

        booking.setStatus("CANCELLED");
        bookingRepository.save(booking);

        return ResponseEntity.ok(
                Map.of(
                        "message",
                        "Booking cancelled successfully.",
                        "bookingId",
                        booking.getId(),
                        "status",
                        booking.getStatus()
                )
        );
    }

    private User getAuthenticatedUser(
            Authentication authentication) {

        if (authentication == null
                || !authentication.isAuthenticated()) {
            return null;
        }

        return userRepository.findByEmail(
                        authentication.getName())
                .orElse(null);
    }

    private ResponseEntity<Map<String, String>> unauthorized(
            String message) {

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("message", message));
    }
}