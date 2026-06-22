package com.uvr.hqs_phone.util

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

object KstTimeUtils {

    val KST: ZoneId = ZoneId.of("Asia/Seoul")
    private val DATE_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    fun nowKst(): ZonedDateTime = ZonedDateTime.now(KST)

    fun todayKstString(): String = nowKst().toLocalDate().format(DATE_FMT)

    fun epochToKstDate(epochMs: Long): String =
        Instant.ofEpochMilli(epochMs).atZone(KST).toLocalDate().format(DATE_FMT)

    /** Epoch ms for 23:59:59.999 on the given date in KST */
    fun endOfDayMs(date: String): Long =
        LocalDate.parse(date, DATE_FMT)
            .atTime(LocalTime.MAX)
            .atZone(KST)
            .toInstant()
            .toEpochMilli()

    /** Epoch ms for 00:00:00.000 on the NEXT day in KST */
    fun startOfNextDayMs(date: String): Long =
        LocalDate.parse(date, DATE_FMT)
            .plusDays(1)
            .atStartOfDay(KST)
            .toInstant()
            .toEpochMilli()

    /**
     * Splits a session across midnight boundaries.
     * Returns a list of Triple(date, startMs, endMs).
     */
    fun splitAtMidnight(startMs: Long, endMs: Long): List<Triple<String, Long, Long>> {
        if (startMs >= endMs) return emptyList()
        val result = mutableListOf<Triple<String, Long, Long>>()
        var curStart = startMs
        while (true) {
            val curDate = epochToKstDate(curStart)
            val dayEnd = endOfDayMs(curDate)
            if (endMs <= dayEnd) {
                result.add(Triple(curDate, curStart, endMs))
                break
            } else {
                result.add(Triple(curDate, curStart, dayEnd))
                curStart = startOfNextDayMs(curDate)
            }
        }
        return result
    }

    /** Milliseconds until next 01:00 AM KST from now */
    fun millisUntilNextSyncTime(): Long {
        val now = nowKst()
        var next = now.toLocalDate().atTime(1, 0, 0).atZone(KST)
        if (!now.toLocalDateTime().isBefore(now.toLocalDate().atTime(1, 0, 0))) {
            next = next.plusDays(1)
        }
        return next.toInstant().toEpochMilli() - System.currentTimeMillis()
    }

    fun formatEpochToReadable(epochMs: Long): String {
        val fmt = DateTimeFormatter.ofPattern("MM-dd HH:mm:ss")
        return Instant.ofEpochMilli(epochMs).atZone(KST).format(fmt)
    }

    fun formatDurationMs(durationMs: Long): String {
        val totalSec = durationMs / 1000
        val h = totalSec / 3600
        val m = (totalSec % 3600) / 60
        val s = totalSec % 60
        return if (h > 0) "${h}h ${m}m" else if (m > 0) "${m}m ${s}s" else "${s}s"
    }
}
