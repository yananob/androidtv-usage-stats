package io.github.yananob.template_android

import android.app.AppOpsManager
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.github.yananob.template_android.databinding.ActivityMainBinding
import io.github.yananob.template_android.databinding.ItemUsageStatBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Android TV 上で UsageStatsManager API を検証するためのメインアクティビティ。
 */
class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "UsageStatsTest"
    }

    private lateinit var binding: ActivityMainBinding
    private val usageListAdapter = UsageStatsAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUi()
        checkPermissionAndRefresh()
    }

    override fun onResume() {
        super.onResume()
        checkPermissionAndRefresh()
    }

    /**
     * UIコンポーネントの初期設定
     */
    private fun setupUi() {
        binding.recyclerViewUsageStats.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = usageListAdapter
        }

        binding.buttonOpenSettings.setOnClickListener {
            openUsageAccessSettings()
        }

        binding.buttonFetchStats.setOnClickListener {
            fetchUsageStats()
        }

        // 初期フォーカスを「Usage Access設定を開く」ボタンに設定
        binding.buttonOpenSettings.requestFocus()
    }

    /**
     * UsageStats 権限の状態を確認し画面表示を更新する
     */
    private fun checkPermissionAndRefresh() {
        val hasPermission = hasUsageStatsPermission()
        Log.d(TAG, "UsageStats permission: $hasPermission")

        if (hasPermission) {
            binding.textPermissionStatus.text = "権限状態: UsageStats 許可済み"
            binding.textErrorMessage.visibility = View.GONE
        } else {
            binding.textPermissionStatus.text = "権限状態: UsageStats 未許可"
            binding.textErrorMessage.text = "UsageStatsへのアクセス権限が必要です"
            binding.textErrorMessage.visibility = View.VISIBLE
        }
    }

    /**
     * Usage Access 権限が付与されているかを判定する
     */
    private fun hasUsageStatsPermission(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /**
     * Usage Access 設定画面を開く
     */
    private fun openUsageAccessSettings() {
        try {
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open Usage Access Settings", e)
            binding.textErrorMessage.text = "このAndroid TVではUsage Access設定画面を開けません"
            binding.textErrorMessage.visibility = View.VISIBLE
        }
    }

    /**
     * 今日の UsageStats を取得し、リスト表示およびログ出力を行う
     */
    private fun fetchUsageStats() {
        if (!hasUsageStatsPermission()) {
            binding.textErrorMessage.text = "UsageStatsへのアクセス権限が必要です"
            binding.textErrorMessage.visibility = View.VISIBLE
            binding.textResultCount.text = "取得結果: 0件"
            usageListAdapter.submitList(emptyList())
            return
        }

        try {
            val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            if (usageStatsManager == null) {
                binding.textErrorMessage.text = "UsageStatsManagerの取得に失敗しました"
                binding.textErrorMessage.visibility = View.VISIBLE
                return
            }

            val startTime = getTodayStartTimeMillis()
            val endTime = System.currentTimeMillis()

            val statsList = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                startTime,
                endTime
            ) ?: emptyList()

            // フォアグラウンド利用時間があるもの、または最終利用時間があるものをフィルタリングしソート
            val filteredStats = statsList
                .filter { it.totalTimeInForeground > 0 || it.lastTimeUsed > 0 }
                .sortedByDescending { it.lastTimeUsed }

            val count = filteredStats.size
            Log.d(TAG, "UsageStats count: $count")

            binding.textResultCount.text = "取得結果: ${count}件"

            if (count == 0) {
                binding.textErrorMessage.text = "UsageStatsデータが取得できませんでした"
                binding.textErrorMessage.visibility = View.VISIBLE
            } else {
                binding.textErrorMessage.visibility = View.GONE
            }

            // アイテム用モデルに変換
            val displayItems = filteredStats.map { usageStats ->
                val appName = getAppName(usageStats.packageName)
                Log.d(TAG, "package: ${usageStats.packageName}")
                Log.d(TAG, "lastTimeUsed: ${usageStats.lastTimeUsed}")
                Log.d(TAG, "totalTimeInForeground: ${usageStats.totalTimeInForeground}")

                UsageStatDisplayItem(
                    appName = appName,
                    packageName = usageStats.packageName,
                    lastTimeUsed = formatLastTimeUsed(usageStats.lastTimeUsed),
                    totalTimeInForeground = formatForegroundTime(usageStats.totalTimeInForeground)
                )
            }

            usageListAdapter.submitList(displayItems)

        } catch (e: Exception) {
            Log.e(TAG, "Error fetching usage stats", e)
            binding.textErrorMessage.text = "エラーが発生しました: ${e.message}"
            binding.textErrorMessage.visibility = View.VISIBLE
        }
    }

    /**
     * パッケージ名からアプリ表示名を取得する
     */
    private fun getAppName(packageName: String): String {
        return try {
            val packageManager = packageManager
            val appInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                packageManager.getApplicationInfo(packageName, 0)
            }
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            packageName
        }
    }

    /**
     * 今日の 00:00:00.000 のタイムスタンプ（ミリ秒）を取得する
     */
    private fun getTodayStartTimeMillis(): Long {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }

    /**
     * 最終使用時刻を「yyyy/MM/dd HH:mm」形式にフォーマットする
     */
    private fun formatLastTimeUsed(timestamp: Long): String {
        if (timestamp <= 0) return "最終使用: - "
        val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
        return "最終使用: ${dateFormat.format(Date(timestamp))}"
    }

    /**
     * 利用時間（ミリ秒）を「○時間○分」または「○分」形式にフォーマットする
     */
    private fun formatForegroundTime(millis: Long): String {
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

/**
 * 画面表示用のデータモデル
 */
data class UsageStatDisplayItem(
    val appName: String,
    val packageName: String,
    val lastTimeUsed: String,
    val totalTimeInForeground: String
)

/**
 * UsageStats 表示用の RecyclerView アダプター
 */
class UsageStatsAdapter : RecyclerView.Adapter<UsageStatsAdapter.ViewHolder>() {

    private val items = mutableListOf<UsageStatDisplayItem>()

    fun submitList(newItems: List<UsageStatDisplayItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemUsageStatBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(private val binding: ItemUsageStatBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: UsageStatDisplayItem) {
            binding.textAppName.text = item.appName
            binding.textPackageName.text = item.packageName
            binding.textLastTimeUsed.text = item.lastTimeUsed
            binding.textTotalTimeInForeground.text = item.totalTimeInForeground
        }
    }
}
