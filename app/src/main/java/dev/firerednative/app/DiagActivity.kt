package dev.firerednative.app

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

import android.app.Activity
import android.app.ActivityManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.text.method.ScrollingMovementMethod
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class DiagActivity : Activity() {

    private var report: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val scroll = ScrollView(this)
        val column = LinearLayout(this)
        column.orientation = LinearLayout.VERTICAL
        column.setPadding(dp(16), dp(24), dp(16), dp(32))
        scroll.addView(column)
        setContentView(scroll)

        column.addView(sectionText("FireRed Native", 24f, 0xFFF2F5F9.toInt(), true, 0))
        column.addView(sectionText("Milestone 0 - ABI capability probe", 13f, 0xFF8A97A8.toInt(), false, 2))

        val verdict = computeVerdict()
        column.addView(verdictBanner(verdict))

        report = buildReport(verdict)
        column.addView(reportView(report))
        column.addView(buttonRow())
    }

    private data class Verdict(val supported: Boolean, val message: String)

    private fun computeVerdict(): Verdict {
        val has32 = Build.SUPPORTED_ABIS.any { it == "armeabi-v7a" || it == "armeabi" }
        return if (has32) {
            Verdict(
                true,
                "GO - 32-bit (AArch32) app processes are supported.\n" +
                    "Literal native execution is viable on this device."
            )
        } else {
            Verdict(
                false,
                "NO-GO - 32-bit apps cannot run on this device.\n" +
                    "The core will pivot to the AArch64 DynaRec variant."
            )
        }
    }

    private fun verdictBanner(verdict: Verdict): LinearLayout {
        val box = LinearLayout(this)
        box.orientation = LinearLayout.VERTICAL
        box.setPadding(dp(14), dp(12), dp(14), dp(12))
        val bg = GradientDrawable()
        bg.cornerRadius = dp(12).toFloat()
        if (verdict.supported) {
            bg.setColor(0x2A1E4A2F.toInt())
            bg.setStroke(dp(1), 0xFF66BB6A.toInt())
        } else {
            bg.setColor(0x2A4A1E1E.toInt())
            bg.setStroke(dp(1), 0xFFE57373.toInt())
        }
        box.background = bg
        box.addView(sectionText(verdict.message, 15f, 0xFFE9EEF5.toInt(), true, 0))
        box.layoutParams = columnParams(0, 16, 0, 8)
        return box
    }

    private fun reportView(content: String): TextView {
        val tv = TextView(this)
        tv.typeface = Typeface.MONOSPACE
        tv.textSize = 12f
        tv.setTextColor(0xFFD7DEE8.toInt())
        tv.setPadding(0, dp(10), 0, dp(10))
        tv.setHorizontallyScrolling(true)
        tv.movementMethod = ScrollingMovementMethod()
        tv.text = content
        tv.layoutParams = columnParams(0, 0, 0, 4)
        return tv
    }

    private fun buttonRow(): LinearLayout {
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER
        row.layoutParams = columnParams(0, 0, 0, 0)

        val copy = Button(this)
        copy.text = "Copy report"
        copy.setOnClickListener {
            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("FireRed Native probe report", report))
            Toast.makeText(this, "Report copied", Toast.LENGTH_SHORT).show()
        }

        val share = Button(this)
        share.text = "Share report"
        share.setOnClickListener {
            val intent = Intent(Intent.ACTION_SEND)
            intent.type = "text/plain"
            intent.putExtra(Intent.EXTRA_SUBJECT, "FireRed Native ABI probe report")
            intent.putExtra(Intent.EXTRA_TEXT, report)
            startActivity(Intent.createChooser(intent, "Share report"))
        }

        val lpCopy = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        lpCopy.setMargins(dp(4), dp(12), dp(10), 0)
        copy.layoutParams = lpCopy

        val lpShare = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        lpShare.setMargins(dp(4), dp(12), dp(4), 0)
        share.layoutParams = lpShare

        row.addView(copy)
        row.addView(share)
        return row
    }

    private fun buildReport(verdict: Verdict): String {
        val lines = ArrayList<String>()

        lines.add("FireRed Native - ABI capability probe")
        lines.add("version: " + tryVersionName())
        lines.add("generated: " + timestamp())
        lines.add("")
        lines.add("VERDICT: " + if (verdict.supported) "GO" else "NO-GO")
        lines.add("  " + verdict.message.replace("\n", "\n  "))
        lines.add("")

        val abiCount = Build.SUPPORTED_ABIS.size
        lines.add("[abis - install order, " + abiCount + " total]")
        Build.SUPPORTED_ABIS.forEachIndexed { i, abi ->
            val bits = if (abi.startsWith("arm64") || abi.startsWith("x86_64") || abi.startsWith("riscv64")) {
                "64-bit"
            } else {
                "32-bit"
            }
            val mark = if (bits == "32-bit") "   <== 32-bit processes available" else ""
            lines.add("  " + (i + 1) + ". " + abi.padEnd(12) + "[" + bits + "]" + mark)
        }
        lines.add("")

        lines.add("[device]")
        lines.add(kv("model", Build.MODEL))
        lines.add(kv("manufacturer", Build.MANUFACTURER))
        lines.add(kv("brand", Build.BRAND))
        lines.add(kv("device", Build.DEVICE))
        lines.add(kv("product", Build.PRODUCT))
        lines.add(kv("board", Build.BOARD))
        if (Build.VERSION.SDK_INT >= 31) {
            lines.add(kv("soc", Build.SOC_MANUFACTURER + " " + Build.SOC_MODEL))
        }
        lines.add(kv("android", Build.VERSION.RELEASE + " (SDK " + Build.VERSION.SDK_INT + ")"))
        lines.add(kv("kernel", safe("unavailable") { System.getProperty("os.version") ?: "unavailable" }))
        lines.add(kv("this process", if (Process.is64Bit()) "64-bit" else "32-bit"))
        lines.add("          (probe apps run in the primary ABI; not the verdict)")
        lines.add(kv("ram", totalRam()))
        lines.add("")

        lines.add("[system props - /system/build.prop]")
        val props = buildPropKeys(
            "ro.product.cpu.abilist",
            "ro.product.cpu.abilist32",
            "ro.product.cpu.abilist64",
            "ro.zygote"
        )
        for (key in sortedSetOf(
            "ro.product.cpu.abilist",
            "ro.product.cpu.abilist32",
            "ro.product.cpu.abilist64",
            "ro.zygote"
        )) {
            lines.add(kv(key, props[key] ?: "n/a"))
        }
        lines.add("")

        lines.add("[cpu - /proc/cpuinfo]")
        val cpu = readCpuInfo()
        lines.add(kv("cores", cpu.cores.toString()))
        lines.add(kv("hardware", if (cpu.hardware.isEmpty()) "n/a" else cpu.hardware))
        lines.add(kv("model names", if (cpu.modelNames.isEmpty()) "n/a" else cpu.modelNames.joinToString(" | ")))
        lines.add(kv("cpu parts", if (cpu.parts.isEmpty()) "n/a" else cpu.parts.joinToString(" | ")))
        if (cpu.features.isNotEmpty()) {
            lines.add(kv("features", cpu.features))
        }
        lines.add("")

        lines.add("[next steps]")
        lines.add("  GO    -> ship the armeabi-v7a core; literal execution plan proceeds (M1/M2).")
        lines.add("  NO-GO -> core pivots to the AArch64 DynaRec variant automatically.")
        lines.add("")
        lines.add("(share this report back so the core variant can be locked in)")
        return lines.joinToString("\n")
    }

    private fun kv(label: String, value: String): String {
        return "  " + label.padEnd(24) + ": " + value
    }

    private fun totalRam(): String {
        return try {
            val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val mem = ActivityManager.MemoryInfo()
            am.getMemoryInfo(mem)
            String.format(Locale.US, "%.1f GiB", mem.totalMem / (1024.0 * 1024.0 * 1024.0))
        } catch (t: Throwable) {
            "unavailable"
        }
    }

    private fun buildPropKeys(vararg wanted: String): Map<String, String> {
        val found = HashMap<String, String>()
        try {
            val propLines = File("/system/build.prop").readLines()
            for (line in propLines) {
                val idx = line.indexOf('=')
                if (idx <= 0) continue
                val key = line.substring(0, idx).trim()
                if (wanted.contains(key) && !found.containsKey(key)) {
                    var value = line.substring(idx + 1).trim()
                    if (value.isEmpty()) value = "(empty)"
                    found[key] = value
                }
            }
        } catch (t: Throwable) {
            // build.prop is not readable on all builds; report stays useful
        }
        return found
    }

    private data class CpuInfo(
        val cores: Int,
        val hardware: String,
        val modelNames: List<String>,
        val parts: List<String>,
        val features: String
    )

    private fun readCpuInfo(): CpuInfo {
        var cores = 0
        var hardware = ""
        var features = ""
        val names = LinkedHashSet<String>()
        val parts = LinkedHashSet<String>()
        try {
            for (raw in File("/proc/cpuinfo").readLines()) {
                val idx = raw.indexOf(':')
                if (idx <= 0) continue
                val key = raw.substring(0, idx).trim()
                val value = raw.substring(idx + 1).trim()
                when (key) {
                    "processor" -> cores++
                    "Hardware" -> hardware = value
                    "Features" -> features = value
                    "model name" -> names.add(value)
                    "CPU part" -> parts.add(value)
                }
            }
        } catch (t: Throwable) {
            // keep whatever parsed successfully
        }
        return CpuInfo(cores, hardware, names.toList(), parts.toList(), features)
    }

    private fun tryVersionName(): String {
        return try {
            val info = packageManager.getPackageInfo(packageName, 0)
            info.versionName ?: "?"
        } catch (t: Throwable) {
            "?"
        }
    }

    private fun timestamp(): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm z", Locale.US).format(Date())
    }

    private fun safe(default: String, block: () -> String): String {
        return try {
            block()
        } catch (t: Throwable) {
            default
        }
    }

    private fun sectionText(text: String, sizeSp: Float, color: Int, bold: Boolean, topPad: Int): TextView {
        val tv = TextView(this)
        tv.text = text
        tv.textSize = sizeSp
        tv.setTextColor(color)
        tv.setPadding(0, dp(topPad), 0, 0)
        if (bold) {
            tv.setTypeface(null, Typeface.BOLD)
        }
        return tv
    }

    private fun columnParams(left: Int, top: Int, right: Int, bottom: Int): LinearLayout.LayoutParams {
        val lp = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        lp.setMargins(dp(left), dp(top), dp(right), dp(bottom))
        return lp
    }

    private fun dp(v: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            v.toFloat(),
            resources.displayMetrics
        ).roundToInt()
    }
}
