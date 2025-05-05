package com.diegowh.konohaproject.settings.infrastructure.ui

import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.SeekBar
import com.diegowh.konohaproject.R
import com.diegowh.konohaproject.databinding.FragmentSettingsBinding
import com.diegowh.konohaproject.app.App
import com.diegowh.konohaproject.settings.domain.repository.TimerSettingsRepository
import com.diegowh.konohaproject.timer.application.usecases.TimerScreenEvent
import com.diegowh.konohaproject.timer.infrastructure.ui.TimerFragment
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.util.Locale

class SettingsFragment : BottomSheetDialogFragment(R.layout.fragment_settings) {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

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

    private lateinit var settings: TimerSettingsRepository

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentSettingsBinding.bind(view)
        settings = (requireActivity().application as App).timerSettings

        dialog?.setOnShowListener { dialog ->
            val bottomSheetDialog = dialog as BottomSheetDialog
            setupFullHeight(bottomSheetDialog)
        }
        loadPreferences()
        initUi()
    }

    private fun setupFullHeight(bottomSheetDialog: BottomSheetDialog) {
        val bottomSheet = bottomSheetDialog.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
        bottomSheet?.let {
            val behavior = BottomSheetBehavior.from(it)
            val layoutParams = it.layoutParams

            val windowHeight = requireActivity().window.decorView.height
            val desiredHeight = (windowHeight * 0.9).toInt()

            layoutParams.height = desiredHeight
            it.layoutParams = layoutParams

            behavior.state = BottomSheetBehavior.STATE_EXPANDED

            behavior.isDraggable = true
            behavior.skipCollapsed = true
            
            behavior.peekHeight = (windowHeight * 0.6).toInt()
        }
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
        (parentFragment as? TimerFragment)?.let { timerFragment ->
            val viewModel = timerFragment.viewModel
            viewModel.onEvent(
                TimerScreenEvent.SettingsEvent.UpdateSettings(
                    focusMinutes = selectedFocus.toLong(),
                    shortBreakMinutes = selectedShortBreak.toLong(),
                    longBreakMinutes = selectedLongBreak.toLong(),
                    totalRounds = selectedRounds,
                    isAutorunEnabled = autorunEnabled,
                    isMuteEnabled = muteEnabled
                )
            )

            viewModel.onEvent(TimerScreenEvent.TimerEvent.Reset)
        }
        dismiss()
    }

    private fun handleReset() {
        (parentFragment as? TimerFragment)?.let { timerFragment ->
            val viewModel = timerFragment.viewModel
            viewModel.onEvent(TimerScreenEvent.SettingsEvent.Reset)
        }
        
        loadPreferences()
        updateUi()
    }
}