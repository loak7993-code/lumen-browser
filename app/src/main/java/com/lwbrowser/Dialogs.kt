package com.lwbrowser

import android.content.Context
import android.view.LayoutInflater
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.lwbrowser.databinding.DialogConfirmBinding

object Dialogs {

    fun confirm(
        context: Context,
        iconRes: Int = R.drawable.ic_delete,
        title: CharSequence,
        message: CharSequence,
        positiveText: CharSequence,
        negativeText: CharSequence,
        onPositive: () -> Unit
    ) {
        confirm(context, iconRes, title, message, positiveText, negativeText, null, onPositive, null)
    }

    fun confirm(
        context: Context,
        iconRes: Int = R.drawable.ic_delete,
        title: CharSequence,
        message: CharSequence,
        positiveText: CharSequence,
        negativeText: CharSequence,
        neutralText: CharSequence?,
        onPositive: () -> Unit,
        onNeutral: (() -> Unit)?
    ) {
        val binding = DialogConfirmBinding.inflate(LayoutInflater.from(context))
        val dialog = AlertDialog.Builder(context).setView(binding.root).create()
        dialog.setOnShowListener {
            dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_bg_rounded)
        }
        binding.dialogIcon.setImageResource(iconRes)
        binding.dialogTitle.text = title
        binding.dialogMessage.text = message
        binding.btnPositive.text = positiveText
        binding.btnNegative.text = negativeText
        binding.btnPositive.setOnClickListener { dialog.dismiss(); onPositive() }
        binding.btnNegative.setOnClickListener { dialog.dismiss() }
        if (neutralText != null) {
            binding.btnNeutral.visibility = android.view.View.VISIBLE
            binding.btnNeutral.text = neutralText
            binding.btnNeutral.setOnClickListener { dialog.dismiss(); onNeutral?.invoke() }
        } else {
            binding.btnNeutral.visibility = android.view.View.GONE
        }
        dialog.show()
    }
}
