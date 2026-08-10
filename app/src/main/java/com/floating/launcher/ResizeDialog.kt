package com.floating.launcher

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import android.view.View

class ResizeDialog(
    context: Context,
    private val app: AppInfo,
    private val onApply: (Int, Int) -> Unit
) : Dialog(context) {

    private var previewWidth = app.width
    private var previewHeight = app.height

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_resize)
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val seekWidth = findViewById<SeekBar>(R.id.seekWidth)
        val seekHeight = findViewById<SeekBar>(R.id.seekHeight)
        val tvWidth = findViewById<TextView>(R.id.tvWidth)
        val tvHeight = findViewById<TextView>(R.id.tvHeight)
        val previewBox = findViewById<View>(R.id.previewBox)
        val btnApply = findViewById<Button>(R.id.btnApply)
        val btnCancel = findViewById<Button>(R.id.btnCancel)

        seekWidth.max = 1200
        seekWidth.progress = app.width
        seekHeight.max = 1000
        seekHeight.progress = app.height

        tvWidth.text = "Ширина: ${app.width}px"
        tvHeight.text = "Высота: ${app.height}px"

        updatePreview(previewBox)

        seekWidth.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                previewWidth = progress.coerceAtLeast(200)
                tvWidth.text = "Ширина: ${previewWidth}px"
                updatePreview(previewBox)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        seekHeight.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                previewHeight = progress.coerceAtLeast(200)
                tvHeight.text = "Высота: ${previewHeight}px"
                updatePreview(previewBox)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        btnApply.setOnClickListener {
            onApply(previewWidth, previewHeight)
            dismiss()
        }

        btnCancel.setOnClickListener {
            dismiss()
        }
    }

    private fun updatePreview(view: View) {
        val params = view.layoutParams
        params.width = (previewWidth * 0.3).toInt().coerceAtLeast(100)
        params.height = (previewHeight * 0.3).toInt().coerceAtLeast(100)
        view.layoutParams = params
    }
}
