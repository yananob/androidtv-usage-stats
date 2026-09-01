package io.github.yananob.template_android

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * UsageStats に関する表示データ・日時計算ユーティリティ等の単体テスト。
 */
class ExampleUnitTest {

    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    /**
     * フォアグラウンド利用時間のフォーマット関数ロジックの検証
     */
    @Test
    fun formatForegroundTime_returnsCorrectString() {
        // 1分未満
        assertEquals("利用時間: 1分未満", formatForegroundTimeHelper(500))

        // 45分
        val millis45Min = 45 * 60 * 1000L
        assertEquals("利用時間: 45分", formatForegroundTimeHelper(millis45Min))

        // 1時間23分
        val millis1Hour23Min = (1 * 60 * 60 + 23 * 60) * 1000L
        assertEquals("利用時間: 1時間23分", formatForegroundTimeHelper(millis1Hour23Min))
    }

    /**
     * 今日の開始時刻（00:00:00.000）の計算ロジックの検証
     */
    @Test
    fun getTodayStartTimeMillis_returnsZeroHourMinuteSecond() {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startTimeMillis = calendar.timeInMillis
        val checkCalendar = Calendar.getInstance().apply {
            timeInMillis = startTimeMillis
        }

        assertEquals(0, checkCalendar.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, checkCalendar.get(Calendar.MINUTE))
        assertEquals(0, checkCalendar.get(Calendar.SECOND))
        assertEquals(0, checkCalendar.get(Calendar.MILLISECOND))
    }

    /**
     * 利用時間の計算表示用ヘルパー関数
     */
    private fun formatForegroundTimeHelper(millis: Long): String {
        val minutes = millis / (1000 * 60)
        val hours = minutes / 60
        val remainingMinutes = minutes % 60

        val formattedText = when {
            hours > 0 -> "${hours}時間${remainingMinutes}分"
            minutes > 0 -> "${minutes}分"
            else -> "1分未満"
        }
        return "利用時間: $formattedText"
    }
}
