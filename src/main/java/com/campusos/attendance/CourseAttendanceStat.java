package com.campusos.attendance;

public record CourseAttendanceStat(Long courseId, String courseCode, String courseTitle,
                                   long present, long total) {
    public double percentage() {
        return total == 0 ? 0 : Math.round((present * 10000.0) / total) / 100.0;
    }
}
