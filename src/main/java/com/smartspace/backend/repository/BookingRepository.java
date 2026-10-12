package com.smartspace.backend.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.smartspace.backend.model.Booking;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findAllByUser_Id(Long userId);

    List<Booking> findAllByUser_Email(String email);

    @Query("""
        SELECT b
        FROM Booking b
        WHERE b.spaceId = :spaceId
          AND b.bookingDate = :bookingDate
          AND b.status <> 'CANCELLED'
        """)
    List<Booking> findActiveBookingsForSpaceAndDate(
            @Param("spaceId") Long spaceId,
            @Param("bookingDate") LocalDate bookingDate
    );

    @Query("""
        SELECT b
        FROM Booking b
        WHERE b.spaceId = :spaceId
          AND b.bookingDate = :bookingDate
          AND b.status <> 'CANCELLED'
          AND b.startTime < :endTime
          AND b.startTime >= :startTime
        """)
    List<Booking> findBookingsStartingWithinTimeRange(
            @Param("spaceId") Long spaceId,
            @Param("bookingDate") LocalDate bookingDate,
            @Param("startTime") java.time.LocalTime startTime,
            @Param("endTime") java.time.LocalTime endTime
    );
}