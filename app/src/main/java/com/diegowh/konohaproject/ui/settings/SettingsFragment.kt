package com.diegowh.konohaproject.ui.settings

import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.widget.SeekBar
import com.diegowh.konohaproject.R
import com.diegowh.konohaproject.databinding.FragmentSettingsListDialogBinding
import com.diegowh.konohaproject.domain.main.App
import com.diegowh.konohaproject.domain.settings.SettingsProvider
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.util.Locale

class SettingsFragment : BottomSheetDialogFragment(R.layout.fragment_settings_list_dialog) {

    private var _binding: FragmentSettingsListDialogBinding? = null
    private val binding get() = _binding!!
    private var listener: Listener? = null

    private val focusOptions = getFocusValues()
    private val shortBreakOptions = getShortBreakValues()
    private val longBreakOptions = getLongBreakValues()
    private val roundsOptions = getRoundValues()

    private var selectedFocus: Int = 0
    private var selectedShortBreak: Int = 0
    private var selectedLongBreak: Int = 0
    private var selectedRounds: Int = 2
    private var autorunEnabled: Boolean = true
    private var muteEnabled: Boolean = false

    private lateinit var settings: SettingsProvider

    interface Listener {
        fun onSettingsChanged()
        fun onDismiss()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentSettingsListDialogBinding.bind(view)
        settings = (requireActivity().application as App).settingsProvider
        listener = parentFragment as? Listener ?: activity as? Listener
        loadPreferences()
        initUi()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun loadPreferences() {
        selectedFocus = settings.focusMinutes().toInt()
        selectedShortBreak = settings.shortBreakMinutes().toInt()
        selectedLongBreak = settings.longBreakMinutes().toInt()
        selectedRounds = settings.totalRounds()
        autorunEnabled = settings.isAutorunEnabled()
        muteEnabled = settings.isMuteEnabled()
    }

    private fun initUi() = with(binding) {

        seekBarFocusTime.max = focusOptions.size - 1
        seekBarFocusTime.setOnSeekBarChangeListener(changeListener { p ->
            txtFocusTime.text = getString(R.string.minutes_format, focusOptions[p])
            selectedFocus = focusOptions[p]
        })

        seekBarShortBreak.max = shortBreakOptions.size - 1
        seekBarShortBreak.setOnSeekBarChangeListener(changeListener { p ->
            txtShortBreak.text = getString(R.string.minutes_format, shortBreakOptions[p])
            selectedShortBreak = shortBreakOptions[p]
        })

        seekBarLongBreak.max = longBreakOptions.size - 1
         seekBarLongBreak.setOnSeekBarChangeListener(changeListener { p ->
            txtLongBreak.text = getString(R.string.minutes_format, longBreakOptions[p])
            selectedLongBreak = longBreakOptions[p]
        })

        seekBarRounds.max = roundsOptions.size - 1
         seekBarRounds.setOnSeekBarChangeListener(changeListener { p ->
            txtRounds.text = String.format(Locale.US, "%d", roundsOptions[p])
            selectedRounds = roundsOptions[p]
        })

        btnAutorun.setOnCheckedChangeListener { _, checked -> autorunEnabled = checked }
        btnMute.setOnCheckedChangeListener { _, checked -> muteEnabled = checked }

        btnSave.setOnClickListener { handleSave() }
        btnReset.setOnClickListener { handleReset() }
        updateUi()
    }

    private fun updateUi() = with(binding) {
        seekBarFocusTime.progress = focusOptions.indexOf(selectedFocus).coerceAtLeast(0)
        seekBarShortBreak.progress = shortBreakOptions.indexOf(selectedShortBreak).coerceAtLeast(0)
        seekBarLongBreak.progress = longBreakOptions.indexOf(selectedLongBreak).coerceAtLeast(0)
        seekBarRounds.progress = roundsOptions.indexOf(selectedRounds).coerceAtLeast(0)

        txtFocusTime.text = getString(R.string.minutes_format, selectedFocus)
        txtShortBreak.text = getString(R.string.minutes_format, selectedShortBreak)
        txtLongBreak.text = getString(R.string.minutes_format, selectedLongBreak)
        txtRounds.text = String.format(Locale.US, "%d", selectedRounds)

        btnAutorun.isChecked = autorunEnabled
        btnMute.isChecked = muteEnabled
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        (parentFragment as? Listener ?: activity as? Listener)?.onDismiss()
    }

    private fun getFocusValues(): List<Int> =
        (1..5).toList() +
        (10..60 step 5).toList() +
        (75..120 step 15).toList()

    private fun getShortBreakValues(): List<Int> =
        (1..5).toList() + (10..30 step 5).toList()

    private fun getLongBreakValues(): List<Int> =
        (1..5).toList() + (10..45 step 5).toList() + 60

    private fun getRoundValues(): List<Int> =
        (2..6).toList()

    private fun changeListener(onChange: (Int) -> Unit) = object :
        SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
            if (fromUser) onChange(progress)
        }
        override fun onStartTrackingTouch(seekBar: SeekBar) {}
        override fun onStopTrackingTouch(seekBar: SeekBar) {}
    }

    private fun handleSave() {
        settings.updateSettings(
            focus = selectedFocus.toLong(),
            shortBreak = selectedShortBreak.toLong(),
            longBreak = selectedLongBreak.toLong(),
            rounds = selectedRounds,
            autorun = autorunEnabled,
            mute = muteEnabled
        )
        listener?.onSettingsChanged()
        dismiss()
    }

    private fun handleReset() {
        settings.resetToDefaults()
        loadPreferences()
        updateUi()
    }
}