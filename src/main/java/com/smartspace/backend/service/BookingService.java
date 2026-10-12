package com.smartspace.backend.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.smartspace.backend.model.Booking;
import com.smartspace.backend.model.Space;
import com.smartspace.backend.model.User;
import com.smartspace.backend.repository.BookingRepository;
import com.smartspace.backend.repository.SpaceRepository;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final SpaceRepository spaceRepository;

    public BookingService(
            BookingRepository bookingRepository,
            SpaceRepository spaceRepository) {
        this.bookingRepository = bookingRepository;
        this.spaceRepository = spaceRepository;
    }

    @Transactional
    public Booking createBooking(Booking requestedBooking, User user) {

        if (requestedBooking.getSpaceId() == null
                || requestedBooking.getBookingDate() == null
                || requestedBooking.getStartTime() == null
                || requestedBooking.getDurationHours() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Space, date, start time, and duration are required.");
        }

        if (requestedBooking.getSpaceId() < 1) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid space ID.");
        }

        int duration = requestedBooking.getDurationHours();

        if (duration < 1 || duration > 12) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Duration must be between 1 and 12 hours.");
        }

        int memberCount = requestedBooking.getMemberCount() == null
                ? 1
                : requestedBooking.getMemberCount();

        if (memberCount < 1) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "At least one member is required.");
        }

        LocalDate bookingDate = requestedBooking.getBookingDate();
        LocalTime startTime = requestedBooking.getStartTime();

        if (bookingDate.isBefore(LocalDate.now())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Booking date cannot be in the past.");
        }

        LocalTime endTime;

        try {
            endTime = startTime.plusHours(duration);
        } catch (RuntimeException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid booking time.");
        }

        if (!endTime.isAfter(startTime)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Bookings cannot extend past midnight.");
        }

        Space space = spaceRepository
                .findByIdForUpdate(requestedBooking.getSpaceId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "The selected space does not exist."));

        if (space.getPrice() == null || space.getPrice() < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "The selected space has an invalid price.");
        }

        if (space.getCapacity() == null || space.getCapacity() < 1) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "The selected space has an invalid capacity.");
        }

        String bookingType = space.getBookingType();

        if (bookingType == null
                || (!bookingType.equalsIgnoreCase("SHARED")
                && !bookingType.equalsIgnoreCase("PRIVATE"))) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "The selected space has an invalid booking type.");
        }

        int capacity = space.getCapacity();

        if (memberCount > capacity) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This room can accommodate at most "
                            + capacity + " members.");
        }

        List<Booking> existingBookings =
                bookingRepository.findActiveBookingsForSpaceAndDate(
                        space.getId(),
                        bookingDate);

        int overlappingMembers = 0;

        for (Booking existing : existingBookings) {

            if (existing.getStartTime() == null
                    || existing.getDurationHours() == null) {
                continue;
            }

            LocalTime existingStart = existing.getStartTime();
            LocalTime existingEnd;

            try {
                existingEnd = existingStart.plusHours(
                        existing.getDurationHours());
            } catch (RuntimeException exception) {
                continue;
            }

            boolean overlaps =
                    startTime.isBefore(existingEnd)
                    && endTime.isAfter(existingStart);

            if (overlaps) {

                // Private rooms cannot have overlapping bookings.
                if (bookingType.equalsIgnoreCase("PRIVATE")) {
                    throw new ResponseStatusException(
                            HttpStatus.CONFLICT,
                            "This private room is already booked "
                                    + "for part of that time.");
                }

                int existingMembers = existing.getMemberCount() == null
                        ? 1
                        : existing.getMemberCount();

                overlappingMembers += existingMembers;
            }
        }

        // Shared rooms allow overlapping bookings within their capacity.
        if (bookingType.equalsIgnoreCase("SHARED")
                && overlappingMembers + memberCount > capacity) {
            int availableSeats = Math.max(
                    0, capacity - overlappingMembers);

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Only " + availableSeats
                            + " seats are available for that time.");
        }

        requestedBooking.setId(null);
        requestedBooking.setUser(user);
        requestedBooking.setMemberCount(memberCount);
        requestedBooking.setTotalPrice(space.getPrice() * duration);
        requestedBooking.setStatus("CONFIRMED");

        return bookingRepository.save(requestedBooking);
    }
}