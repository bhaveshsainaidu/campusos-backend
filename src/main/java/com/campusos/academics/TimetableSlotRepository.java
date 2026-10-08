package com.campusos.academics;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalTime;
import java.util.List;

public interface TimetableSlotRepository extends JpaRepository<TimetableSlot, Long> {

    @Query("""
        SELECT DISTINCT t FROM TimetableSlot t
        JOIN FETCH t.assignment a
        JOIN FETCH a.course c
        JOIN FETCH a.faculty f
        JOIN FETCH t.batch b
        WHERE t.batch.id = :batchId
        ORDER BY t.dayOfWeek, t.startTime
        """)
    List<TimetableSlot> findByBatch(@Param("batchId") Long batchId);

    @Query("""
        SELECT DISTINCT t FROM TimetableSlot t
        JOIN FETCH t.assignment a
        JOIN FETCH a.course c
        JOIN FETCH t.batch b
        WHERE a.faculty.id = :facultyId
        ORDER BY t.dayOfWeek, t.startTime
        """)
    List<TimetableSlot> findByFaculty(@Param("facultyId") Long facultyId);

    /** Overlap condition: existing.start < newEnd AND existing.end > newStart on the same day. */
    @Query("""
        SELECT t FROM TimetableSlot t
        WHERE t.dayOfWeek = :day
          AND t.startTime < :end AND t.endTime > :start
          AND (t.batch.id = :batchId OR t.assignment.faculty.id = :facultyId OR t.room = :room)
        """)
    List<TimetableSlot> findClashes(@Param("batchId") Long batchId,
                                    @Param("facultyId") Long facultyId,
                                    @Param("room") String room,
                                    @Param("day") int day,
                                    @Param("start") LocalTime start,
                                    @Param("end") LocalTime end);
}
