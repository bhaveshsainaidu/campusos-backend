package com.campusos.attendance;

/** Projections used by attendance aggregate queries. */
record CourseAttendanceStat(Long courseId, String courseCode, String courseTitle,
                                   long present, long total) {
    public double percentage() {
        return total == 0 ? 0 : Math.round((present * 10000.0) / total) / 100.0;
    }
}

record OverallAttendanceStat(Long present, Long total) {
    public double percentage() {
        return (total == null || total == 0) ? 0 : Math.round((present * 10000.0) / total) / 100.0;
    }
}

record StudentAttendanceStat(Long studentId, String rollNumber, String name,
                                    Long present, Long total) {
    public double percentage() {
        return (total == null || total == 0) ? 0 : Math.round((present * 10000.0) / total) / 100.0;
    }
}
