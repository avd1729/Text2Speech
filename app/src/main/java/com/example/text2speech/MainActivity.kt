package com.example.text2speech

import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.text.Spannable
import android.text.SpannableString
import android.text.style.BackgroundColorSpan
import android.view.Menu
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.Locale
import java.util.UUID

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var editText: EditText
    private lateinit var btnSpeak: Button
    private lateinit var btnPause: Button
    private lateinit var btnResume: Button
    private lateinit var languageSpinner: Spinner
    private lateinit var voiceSpinner: Spinner
    private lateinit var rateSeekBar: SeekBar
    private lateinit var pitchSeekBar: SeekBar
    private lateinit var textDisplay: TextView
    private lateinit var historyRecyclerView: RecyclerView
    private lateinit var historyAdapter: HistoryAdapter

    private var tts: TextToSpeech? = null
    private val MAX_CHAR_LIMIT = 500
    private var speechHistory = mutableListOf<SpeechItem>()
    private var isSpeaking = false
    private var isPaused = false
    private var lastUtteranceId = ""
    private var currentLanguage = Locale.US

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize TextToSpeech engine
        tts = TextToSpeech(this, this)

        // Initialize views
        editText = findViewById(R.id.editText)
        btnSpeak = findViewById(R.id.btnSpeak)
        btnPause = findViewById(R.id.btnPause)
        btnResume = findViewById(R.id.btnResume)
        languageSpinner = findViewById(R.id.languageSpinner)
        voiceSpinner = findViewById(R.id.voiceSpinner)
        rateSeekBar = findViewById(R.id.rateSeekBar)
        pitchSeekBar = findViewById(R.id.pitchSeekBar)
        textDisplay = findViewById(R.id.textDisplay)
        historyRecyclerView = findViewById(R.id.historyRecyclerView)

        // Set up history RecyclerView
        historyAdapter = HistoryAdapter(speechHistory) { text ->
            editText.setText(text)
        }
        historyRecyclerView.layoutManager = LinearLayoutManager(this)
        historyRecyclerView.adapter = historyAdapter

        // Setup language spinner
        setupLanguageSpinner()

        // Initialize seekbars
        setupSeekBars()

        // Set up buttons
        setupButtons()
    }

    private fun setupLanguageSpinner() {
        val languages = arrayOf(
            "English (US)", "English (UK)", "French", "German", "Spanish",
            "Italian", "Chinese", "Japanese", "Russian", "Hindi"
        )
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, languages)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        languageSpinner.adapter = adapter

        languageSpinner.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                when (position) {
                    0 -> setLanguage(Locale.US)
                    1 -> setLanguage(Locale.UK)
                    2 -> setLanguage(Locale.FRANCE)
                    3 -> setLanguage(Locale.GERMANY)
                    4 -> setLanguage(Locale("es", "ES"))
                    5 -> setLanguage(Locale.ITALY)
                    6 -> setLanguage(Locale.CHINA)
                    7 -> setLanguage(Locale.JAPAN)
                    8 -> setLanguage(Locale("ru", "RU"))
                    9 -> setLanguage(Locale("hi", "IN"))
                }
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {
                // Do nothing
            }
        })
    }

    private fun setLanguage(locale: Locale) {
        val result = tts?.setLanguage(locale)
        currentLanguage = locale

        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            Toast.makeText(this, "Language not supported", Toast.LENGTH_SHORT).show()
        } else {
            updateVoiceOptions()
        }
    }

    private fun updateVoiceOptions() {
        val voices = tts?.voices?.filter { it.locale == currentLanguage }
        val voiceNames = voices?.map { it.name } ?: listOf("Default")

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, voiceNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        voiceSpinner.adapter = adapter

        voiceSpinner.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                voices?.get(position)?.let { voice ->
                    tts?.setVoice(voice)
                }
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {
                // Do nothing
            }
        })
    }

    private fun setupSeekBars() {
        // Speech rate (0.5f to 2.0f)
        rateSeekBar.progress = 50  // Default progress (1.0f)
        rateSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val rate = 0.5f + (progress / 100f) * 1.5f  // Map 0-100 to 0.5-2.0
                tts?.setSpeechRate(rate)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Pitch (0.5f to 2.0f)
        pitchSeekBar.progress = 50  // Default progress (1.0f)
        pitchSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val pitch = 0.5f + (progress / 100f) * 1.5f  // Map 0-100 to 0.5-2.0
                tts?.setPitch(pitch)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun setupButtons() {
        btnSpeak.setOnClickListener {
            speakText()
        }

        btnPause.setOnClickListener {
            if (isSpeaking && !isPaused) {
                tts?.stop()
                isPaused = true
                Toast.makeText(this, "Speech paused", Toast.LENGTH_SHORT).show()
            }
        }

        btnResume.setOnClickListener {
            if (isPaused) {
                speakText()
                isPaused = false
            }
        }
    }

    private fun speakText() {
        val text = editText.text.toString().trim()

        if (text.isEmpty()) {
            Toast.makeText(this, "Please enter text to speak", Toast.LENGTH_SHORT).show()
            return
        }

        if (text.length > MAX_CHAR_LIMIT) {
            Toast.makeText(this, "Text exceeds $MAX_CHAR_LIMIT character limit", Toast.LENGTH_SHORT).show()
            return
        }

        // Display the text
        textDisplay.text = text

        // Add to history if not already present
        if (!speechHistory.any { it.text == text }) {
            speechHistory.add(0, SpeechItem(text))
            historyAdapter.notifyItemInserted(0)
        }

        // Stop current speech if any
        if (isSpeaking) {
            tts?.stop()
        }

        // Set up utterance progress listener for highlighting
        setupUtteranceProgressListener()

        // Speak the text
        lastUtteranceId = UUID.randomUUID().toString()
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, lastUtteranceId)
        isSpeaking = true
    }

    private fun setupUtteranceProgressListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                runOnUiThread {
                    isSpeaking = true
                    isPaused = false
                }
            }

            override fun onDone(utteranceId: String?) {
                runOnUiThread {
                    isSpeaking = false
                    isPaused = false
                    // Clear highlighting
                    textDisplay.text = textDisplay.text.toString()
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                runOnUiThread {
                    isSpeaking = false
                    isPaused = false
                    Toast.makeText(applicationContext, "Error in TTS", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onRangeStart(utteranceId: String?, start: Int, end: Int, frame: Int) {
                runOnUiThread {
                    // Highlight current word
                    val text = textDisplay.text.toString()
                    if (start >= 0 && end <= text.length && start < end) {
                        val spannableString = SpannableString(text)
                        val highlightColor = ContextCompat.getColor(applicationContext, R.color.highlight_color)
                        spannableString.setSpan(
                            BackgroundColorSpan(highlightColor),
                            start, end,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                        textDisplay.text = spannableString
                    }
                }
            }
        })
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            // Set default language
            val result = tts?.setLanguage(Locale.US)

            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Toast.makeText(this, "TTS language not supported", Toast.LENGTH_SHORT).show()
            } else {
                // Enable buttons
                btnSpeak.isEnabled = true
                btnPause.isEnabled = true
                btnResume.isEnabled = true

                // Initialize voice options
                updateVoiceOptions()
            }
        } else {
            Toast.makeText(this, "Failed to initialize TTS engine", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_clear_history -> {
                speechHistory.clear()
                historyAdapter.notifyDataSetChanged()
                return true
            }
            R.id.action_paste -> {
                pasteFromClipboard()
                return true
            }
            R.id.action_multi_input -> {
                showMultiInputDialog()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun pasteFromClipboard() {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        if (clipboard.hasPrimaryClip()) {
            val item = clipboard.primaryClip?.getItemAt(0)
            val text = item?.text.toString()

            if (text.length > MAX_CHAR_LIMIT) {
                Toast.makeText(this, "Text exceeds $MAX_CHAR_LIMIT character limit", Toast.LENGTH_SHORT).show()
                editText.setText(text.substring(0, MAX_CHAR_LIMIT))
            } else {
                editText.setText(text)
            }
        }
    }

    private fun showMultiInputDialog() {
        val builder = AlertDialog.Builder(this)
        val inflater = layoutInflater
        val dialogView = inflater.inflate(R.layout.dialog_multi_input, null)
        val multiEditText = dialogView.findViewById<EditText>(R.id.multiEditText)

        builder.setView(dialogView)
            .setTitle("Multi-line Text Input")
            .setPositiveButton("Use Text") { dialog, _ ->
                val text = multiEditText.text.toString()
                if (text.length > MAX_CHAR_LIMIT) {
                    Toast.makeText(this, "Text exceeds $MAX_CHAR_LIMIT character limit", Toast.LENGTH_SHORT).show()
                    editText.setText(text.substring(0, MAX_CHAR_LIMIT))
                } else {
                    editText.setText(text)
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }

        builder.create().show()
    }

    override fun onDestroy() {
        // Shutdown TTS engine
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}
