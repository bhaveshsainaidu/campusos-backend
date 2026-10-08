package com.campusos.attendance;

public record OverallAttendanceStat(Long present, Long total) {
    public double percentage() {
        return (total == null || total == 0) ? 0 : Math.round((present * 10000.0) / total) / 100.0;
    }
}
