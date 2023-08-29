package com.kalymni

import android.os.Handler
import android.os.Looper
import java.util.Locale

class Timer(listener: OnTimerTickListener) {
    interface OnTimerTickListener {
        fun onTimerTick(duration: String)
    }

    private var handler = Handler(Looper.getMainLooper())
    private lateinit var runnable: Runnable
    private var duration = 0L
    private var delay = 100L

    init {
        runnable = Runnable {
            duration += delay
            handler.postDelayed(runnable, delay)
            listener.onTimerTick(format())
        }
    }

    fun start() {
        handler.postDelayed(runnable, delay)
    }

    fun pause() {
        handler.removeCallbacks(runnable)
    }

    fun stop() {
        handler.removeCallbacks(runnable)
        duration = 0L
    }

    private fun format(): String {
        val seconds = (duration / 1000) % 60
        val minutes = (duration / (1000 * 60)) % 60
        val hours = (duration / (1000 * 60 * 60))

        return if (hours > 0) {
            "${String.format(Locale.ENGLISH, "%01d", hours)}:${
                String.format(
                    Locale.ENGLISH,
                    "%02d",
                    minutes
                )
            }:${String.format(Locale.ENGLISH, "%02d", seconds)}"
        } else {
            "${String.format(Locale.ENGLISH, "%01d", minutes)}:${
                String.format(
                    Locale.ENGLISH,
                    "%02d",
                    seconds
                )
            }"
        }
    }
}