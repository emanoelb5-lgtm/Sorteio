package com.b5solucoes.sorteio

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.text.InputFilter
import android.text.InputType
import java.security.SecureRandom
import kotlin.math.max
import kotlin.math.min

class MainActivity : Activity() {
    private val background = Color.rgb(9, 15, 32)
    private val card = Color.rgb(20, 28, 51)
    private val muted = Color.rgb(147, 158, 188)
    private val white = Color.rgb(247, 248, 255)
    private val violet = Color.rgb(164, 140, 255)
    private val mint = Color.rgb(110, 244, 190)
    private val rng = SecureRandom()
    private val prefs by lazy { getSharedPreferences("sorteio_local", Context.MODE_PRIVATE) }

    private var limit = 100
    private val chosen = mutableListOf<Int>()
    private val chosenSet = hashSetOf<Int>()
    private var spinning = false

    private lateinit var rangeText: TextView
    private lateinit var drawnCount: TextView
    private lateinit var remainingCount: TextView
    private lateinit var numberText: TextView
    private lateinit var numberHint: TextView
    private lateinit var action: TextView
    private lateinit var progress: ProgressBar
    private lateinit var history: LinearLayout
    private lateinit var historyTitle: TextView
    private lateinit var celebration: ConfettiView
    private lateinit var changeConfig: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = background
        window.navigationBarColor = background
        readState()
        buildScreen()
        refresh()
    }

    private fun readState() {
        limit = prefs.getInt("limit", 100).coerceIn(1, 10000)
        val raw = prefs.getString("numbers", "").orEmpty()
        raw.split(',').forEach { item ->
            val number = item.toIntOrNull()
            if (number != null && number in 1..limit && chosenSet.add(number)) chosen.add(number)
        }
    }

    private fun saveState() {
        // commit() ensures a completed draw survives an immediate app close.
        prefs.edit().putInt("limit", limit).putString("numbers", chosen.joinToString(",")).commit()
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density + .5f).toInt()
    private fun shape(color: Int, radius: Int = 20, stroke: Int? = null): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
            if (stroke != null) setStroke(dp(1), stroke)
        }

    private fun gradient(colors: IntArray, radius: Int = 22, stroke: Int? = null): GradientDrawable =
        GradientDrawable(GradientDrawable.Orientation.TL_BR, colors).apply {
            cornerRadius = dp(radius).toFloat()
            if (stroke != null) setStroke(dp(1), stroke)
        }

    private fun label(text: String, size: Float, color: Int = white, bold: Boolean = false): TextView =
        TextView(this).apply {
            this.text = text
            textSize = size
            setTextColor(color)
            typeface = if (bold) Typeface.create("sans-serif", Typeface.BOLD) else Typeface.create("sans-serif", Typeface.NORMAL)
            includeFontPadding = false
        }

    private fun column(): LinearLayout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
    private fun row(): LinearLayout = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
    private fun addSpace(parent: LinearLayout, height: Int) {
        parent.addView(View(this), LinearLayout.LayoutParams(1, dp(height)))
    }

    private fun buildScreen() {
        val scroller = ScrollView(this).apply {
            fillViewport = true
            clipToPadding = false
            isVerticalScrollBarEnabled = false
            setBackgroundColor(background)
        }
        val base = column().apply { setPadding(dp(20), dp(24), dp(20), dp(26)) }
        scroller.addView(base, ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        setContentView(scroller)

        // Cabeçalho
        val top = row()
        val brandIcon = label("✦", 30f, white, true).apply {
            gravity = Gravity.CENTER
            background = gradient(intArrayOf(Color.rgb(122, 90, 245), Color.rgb(77, 47, 162)), 16)
        }
        top.addView(brandIcon, LinearLayout.LayoutParams(dp(54), dp(54)))
        val brand = column().apply { setPadding(dp(13), 0, 0, 0) }
        brand.addView(label("SORTEIO", 23f, white, true))
        addSpace(brand, 4)
        brand.addView(label("by B5 Soluções", 11f, muted))
        top.addView(brand, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        val offline = label("●  OFFLINE", 10f, mint, true).apply {
            gravity = Gravity.CENTER
            setPadding(dp(12), dp(9), dp(12), dp(9))
            background = shape(Color.rgb(22, 57, 55), 25)
        }
        top.addView(offline)
        base.addView(top)
        addSpace(base, 30)

        val eyebrow = label("SUA RIFA, SUAS REGRAS", 11f, violet, true).apply { letterSpacing = .14f }
        base.addView(eyebrow)
        addSpace(base, 7)
        base.addView(label("Cada número, uma emoção.", 24f, white, true))
        addSpace(base, 22)

        // Configuração da quantidade
        val config = row().apply {
            setPadding(dp(17), dp(18), dp(14), dp(18))
            background = shape(card, 20, Color.rgb(46, 57, 87))
        }
        val configText = column()
        configText.addView(label("QUANTIDADE DE NÚMEROS", 10f, muted, true).apply { letterSpacing = .07f })
        addSpace(configText, 7)
        rangeText = label("001 a 100", 21f, white, true)
        configText.addView(rangeText)
        config.addView(configText, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        changeConfig = label("ALTERAR  ✎", 11f, violet, true).apply {
            gravity = Gravity.CENTER
            setPadding(dp(12), dp(12), dp(12), dp(12))
            background = shape(Color.rgb(48, 39, 84), 12)
            setOnClickListener { showChangeLimit() }
        }
        config.addView(changeConfig)
        base.addView(config)
        addSpace(base, 15)

        // Indicadores
        val stats = row()
        stats.addView(metric("SORTEADOS", "0", violet).also { drawnCount = it.second }.first,
            LinearLayout.LayoutParams(0, dp(91), 1f))
        stats.addView(View(this), LinearLayout.LayoutParams(dp(12), 1))
        stats.addView(metric("RESTANTES", "100", mint).also { remainingCount = it.second }.first,
            LinearLayout.LayoutParams(0, dp(91), 1f))
        base.addView(stats)
        addSpace(base, 17)

        // Painel principal com halo e número grande
        val panel = GlowPanel(this).apply {
            background = gradient(intArrayOf(Color.rgb(38, 30, 88), Color.rgb(23, 25, 61), Color.rgb(21, 29, 58)), 27,
                Color.rgb(77, 62, 151))
            clipToOutline = true
        }
        val panelContent = column().apply {
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(14), dp(24), dp(14), dp(28))
        }
        panelContent.addView(label("✦   NÚMERO SORTEADO   ✦", 11f, Color.rgb(199, 187, 255), true).apply {
            gravity = Gravity.CENTER
            letterSpacing = .12f
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        addSpace(panelContent, 17)
        numberText = label("—", 108f, white, true).apply {
            gravity = Gravity.CENTER
            typeface = Typeface.create("sans-serif-black", Typeface.BOLD)
            setShadowLayer(dp(18).toFloat(), 0f, 0f, Color.rgb(91, 77, 190))
            if (android.os.Build.VERSION.SDK_INT >= 26) {
                setAutoSizeTextTypeUniformWithConfiguration(48, 108, 2, android.util.TypedValue.COMPLEX_UNIT_SP)
            }
        }
        panelContent.addView(numberText, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(144)))
        addSpace(panelContent, 8)
        numberHint = label("A sorte começa com um toque", 13f, Color.rgb(189, 195, 226)).apply {
            gravity = Gravity.CENTER
        }
        panelContent.addView(numberHint, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        panel.addView(panelContent, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        celebration = ConfettiView(this).apply { isClickable = false }
        panel.addView(celebration, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        base.addView(panel, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(285)))
        addSpace(base, 18)

        action = label("✦    SORTEAR NÚMERO", 17f, white, true).apply {
            gravity = Gravity.CENTER
            letterSpacing = .04f
            elevation = dp(7).toFloat()
            background = gradient(intArrayOf(Color.rgb(138, 94, 255), Color.rgb(102, 62, 221)), 19)
            setOnClickListener { drawNumber() }
            contentDescription = "Sortear próximo número da rifa"
        }
        base.addView(action, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(67)))
        addSpace(base, 15)

        val progressCaption = row()
        progressCaption.addView(label("PROGRESSO DO SORTEIO", 10f, muted, true).apply { letterSpacing = .08f },
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        val percent = label("", 11f, violet, true)
        progressCaption.addView(percent)
        base.addView(progressCaption)
        addSpace(base, 9)
        progress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = limit
            progressTintList = android.content.res.ColorStateList.valueOf(violet)
            progressBackgroundTintList = android.content.res.ColorStateList.valueOf(Color.rgb(44, 51, 79))
        }
        base.addView(progress, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(7)))
        progress.tag = percent
        addSpace(base, 29)

        val historyHeading = row()
        historyTitle = label("Histórico do sorteio", 19f, white, true)
        historyHeading.addView(historyTitle, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        val seeAll = label("VER TODOS  →", 11f, violet, true).apply {
            setPadding(dp(9), dp(10), dp(3), dp(10))
            setOnClickListener { showHistory() }
        }
        historyHeading.addView(seeAll)
        base.addView(historyHeading)
        addSpace(base, 10)
        history = column().apply {
            setPadding(dp(13), dp(13), dp(13), dp(13))
            background = shape(card, 21, Color.rgb(44, 53, 81))
        }
        base.addView(history)
        addSpace(base, 25)

        val bottomActions = row()
        bottomActions.addView(outlineButton("↻  NOVO SORTEIO") { confirmReset() },
            LinearLayout.LayoutParams(0, dp(52), 1f))
        bottomActions.addView(View(this), LinearLayout.LayoutParams(dp(11), 1))
        bottomActions.addView(outlineButton("↗  COMPARTILHAR") { shareHistory() },
            LinearLayout.LayoutParams(0, dp(52), 1f))
        base.addView(bottomActions)
        addSpace(base, 26)
        base.addView(label("SORTEIO JUSTO • SEM REPETIÇÕES • 100% OFFLINE", 10f, muted, true).apply {
            gravity = Gravity.CENTER
            letterSpacing = .05f
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        base.tag = percent
    }

    private fun metric(title: String, initial: String, accent: Int): Pair<LinearLayout, TextView> {
        val wrap = column().apply {
            setPadding(dp(16), dp(16), dp(10), dp(11))
            background = shape(card, 19, Color.rgb(39, 50, 77))
        }
        wrap.addView(label(title, 10f, muted, true).apply { letterSpacing = .08f })
        addSpace(wrap, 7)
        val value = label(initial, 28f, accent, true)
        wrap.addView(value)
        return wrap to value
    }

    private fun outlineButton(title: String, onClick: () -> Unit): TextView =
        label(title, 11f, white, true).apply {
            gravity = Gravity.CENTER
            background = shape(Color.rgb(21, 30, 52), 15, Color.rgb(53, 62, 94))
            setOnClickListener { if (!spinning) onClick() }
        }

    private fun formatted(number: Int): String = number.toString().padStart(max(3, limit.toString().length), '0')

    private fun refresh() {
        rangeText.text = "${formatted(1)}  até  ${formatted(limit)}"
        drawnCount.text = chosen.size.toString()
        remainingCount.text = (limit - chosen.size).toString()
        progress.max = limit
        progress.progress = chosen.size
        val percentage = chosen.size * 100 / limit
        (progress.tag as? TextView)?.text = "$percentage%"
        if (!spinning) {
            numberText.text = chosen.lastOrNull()?.let(::formatted) ?: "—"
            numberHint.text = when {
                chosen.isEmpty() -> "A sorte começa com um toque"
                chosen.size == limit -> "Todos os números foram sorteados!"
                else -> "Número confirmado • Boa sorte!"
            }
            action.text = if (chosen.size == limit) "✓    SORTEIO CONCLUÍDO" else "✦    SORTEAR NÚMERO"
            action.alpha = if (chosen.size == limit) .58f else 1f
        }
        updateHistory()
    }

    private fun drawNumber() {
        if (spinning || chosen.size == limit) return
        val available = (1..limit).filterNot { chosenSet.contains(it) }
        if (available.isEmpty()) return
        val winner = available[rng.nextInt(available.size)]
        spinning = true
        action.text = "✧    SORTEANDO..."
        action.alpha = .7f
        numberHint.text = "Aguarde... a sorte está girando"
        action.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        ValueAnimator.ofInt(0, 23).apply {
            duration = 1350
            interpolator = DecelerateInterpolator()
            addUpdateListener { animator ->
                val sample = available[rng.nextInt(available.size)]
                numberText.text = formatted(sample)
                numberText.scaleX = 1f + .035f * (animator.animatedFraction)
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    chosen.add(winner)
                    chosenSet.add(winner)
                    saveState()
                    spinning = false
                    numberText.scaleX = 1f
                    refresh()
                    celebration.play()
                    vibrateOnWin()
                    numberText.animate().scaleX(1.10f).scaleY(1.10f).setDuration(145)
                        .withEndAction { numberText.animate().scaleX(1f).scaleY(1f).setDuration(210).start() }.start()
                    numberText.announceForAccessibility("Número sorteado: $winner")
                }
            })
            start()
        }
    }

    private fun vibrateOnWin() {
        val service = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
        if (!service.hasVibrator()) return
        @Suppress("DEPRECATION")
        if (android.os.Build.VERSION.SDK_INT >= 26) service.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
        else service.vibrate(100)
    }

    private fun updateHistory() {
        history.removeAllViews()
        if (chosen.isEmpty()) {
            history.addView(label("Nenhum número sorteado ainda.\nOs resultados aparecerão aqui.", 13f, muted).apply {
                gravity = Gravity.CENTER
                setPadding(dp(6), dp(20), dp(6), dp(20))
            }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            return
        }
        val shown = chosen.asReversed().take(24)
        val perRow = if (resources.displayMetrics.widthPixels / resources.displayMetrics.density < 370f) 4 else 5
        shown.chunked(perRow).forEach { numbers ->
            val line = row().apply { gravity = Gravity.CENTER_VERTICAL }
            numbers.forEachIndexed { index, number ->
                val cell = label(formatted(number), 14f, if (number == chosen.last()) white else Color.rgb(208, 214, 241), true).apply {
                    gravity = Gravity.CENTER
                    background = if (number == chosen.last()) gradient(intArrayOf(Color.rgb(128, 88, 242), Color.rgb(93, 66, 180)), 12)
                        else shape(Color.rgb(35, 45, 74), 12)
                }
                val lp = LinearLayout.LayoutParams(0, dp(43), 1f)
                lp.setMargins(if (index == 0) 0 else dp(4), 0, if (index == numbers.lastIndex) 0 else dp(4), dp(8))
                line.addView(cell, lp)
            }
            repeat(perRow - numbers.size) { line.addView(View(this), LinearLayout.LayoutParams(0, dp(43), 1f)) }
            history.addView(line)
        }
        if (chosen.size > 24) history.addView(label("+ ${chosen.size - 24} anteriores • toque em VER TODOS", 11f, muted).apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(7), 0, dp(5))
        })
    }

    private fun showChangeLimit() {
        if (spinning) return
        val edit = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            setSingleLine(true)
            setText(limit.toString())
            setSelectAllOnFocus(true)
            filters = arrayOf(InputFilter.LengthFilter(5))
            textSize = 21f
            setPadding(dp(16), dp(13), dp(16), dp(13))
        }
        val wrapper = FrameLayout(this).apply { setPadding(dp(18), dp(8), dp(18), 0); addView(edit) }
        AlertDialog.Builder(this)
            .setTitle("Quantidade de números")
            .setMessage("Escolha quantos números participam da rifa (de 1 até 10.000). Os números vão de 1 até a quantidade definida.")
            .setView(wrapper)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Aplicar") { _, _ ->
                val newLimit = edit.text.toString().toIntOrNull()
                if (newLimit == null || newLimit !in 1..10000) {
                    alert("Quantidade inválida", "Escolha um número inteiro de 1 a 10.000.")
                } else if (newLimit != limit) {
                    if (chosen.isNotEmpty()) {
                        AlertDialog.Builder(this)
                            .setTitle("Começar um novo sorteio?")
                            .setMessage("Alterar a quantidade reiniciará a rifa e apagará ${chosen.size} número(s) do histórico atual.")
                            .setNegativeButton("Manter rifa", null)
                            .setPositiveButton("Sim, alterar") { _, _ -> reset(newLimit) }
                            .show()
                    } else reset(newLimit)
                }
            }.show()
    }

    private fun confirmReset() {
        if (chosen.isEmpty()) {
            alert("Rifa pronta", "Ainda não há números sorteados. Toque em SORTEAR NÚMERO para começar!")
            return
        }
        AlertDialog.Builder(this).setTitle("Iniciar novo sorteio?")
            .setMessage("Os ${chosen.size} números sorteados serão apagados. Esta ação não pode ser desfeita.")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Reiniciar") { _, _ -> reset(limit) }.show()
    }

    private fun reset(newLimit: Int) {
        limit = newLimit
        chosen.clear()
        chosenSet.clear()
        saveState()
        refresh()
    }

    private fun showHistory() {
        if (chosen.isEmpty()) {
            alert("Histórico vazio", "Os números aparecerão aqui após o primeiro sorteio.")
            return
        }
        val content = chosen.asReversed().mapIndexed { index, number ->
            "${(chosen.size - index).toString().padStart(3, '0')}º   •   ${formatted(number)}"
        }.joinToString("\n")
        val textView = label(content, 17f, Color.rgb(30, 36, 63), true).apply {
            setPadding(dp(20), dp(15), dp(20), dp(15))
            setLineSpacing(dp(5).toFloat(), 1f)
        }
        val scroll = ScrollView(this).apply { addView(textView) }
        AlertDialog.Builder(this).setTitle("Todos os sorteados (${chosen.size})")
            .setView(scroll)
            .setNegativeButton("Fechar", null)
            .setPositiveButton("Compartilhar") { _, _ -> shareHistory() }
            .show()
    }

    private fun shareHistory() {
        if (chosen.isEmpty()) {
            alert("Nada para compartilhar", "Realize pelo menos um sorteio primeiro.")
            return
        }
        val text = buildString {
            append("🎟️ SORTEIO • B5 Soluções\n")
            append("Números: 1 a $limit\n")
            append("Sorteados: ${chosen.size} de $limit\n\n")
            chosen.forEachIndexed { i, number -> append("${i + 1}º: $number\n") }
            append("\nSorteio sem repetição.")
        }
        startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }, "Compartilhar resultado"))
    }

    private fun alert(title: String, message: String) =
        AlertDialog.Builder(this).setTitle(title).setMessage(message).setPositiveButton("OK", null).show()

    private inner class GlowPanel(context: Context) : FrameLayout(context) {
        private val glow = Paint(Paint.ANTI_ALIAS_FLAG)
        init { setWillNotDraw(false) }
        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val centerX = width * .5f
            val centerY = height * .53f
            glow.shader = RadialGradient(centerX, centerY, width * .60f,
                intArrayOf(Color.argb(57, 169, 130, 255), Color.argb(10, 169, 130, 255), Color.TRANSPARENT),
                floatArrayOf(0f, .45f, 1f), Shader.TileMode.CLAMP)
            canvas.drawCircle(centerX, centerY, width * .60f, glow)
            glow.shader = null
        }
    }

    private inner class ConfettiView(context: Context) : View(context) {
        private var phase = -1f
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val dots = List(38) { i ->
            floatArrayOf((i * 73 % 101) / 101f, (i * 47 % 89) / 89f, (i * 31 % 7 + 3).toFloat())
        }
        fun play() {
            ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 1050
                addUpdateListener { phase = it.animatedValue as Float; invalidate() }
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) { phase = -1f; invalidate() }
                })
                start()
            }
        }
        override fun onDraw(canvas: Canvas) {
            if (phase < 0f) return
            val colors = intArrayOf(Color.rgb(255, 214, 116), Color.rgb(153, 255, 209), Color.rgb(195, 161, 255), white)
            dots.forEachIndexed { index, dot ->
                paint.color = colors[index % colors.size]
                paint.alpha = ((1f - phase) * 240).toInt().coerceIn(0, 255)
                val x = width * (.5f + (dot[0] - .5f) * phase * 1.75f)
                val y = height * (.55f + (dot[1] - .5f) * phase * 1.5f + phase * phase * .30f)
                canvas.drawRoundRect(x, y, x + dp(dot[2].toInt()).toFloat(), y + dp(5).toFloat(), dp(2).toFloat(), dp(2).toFloat(), paint)
            }
        }
    }
}
