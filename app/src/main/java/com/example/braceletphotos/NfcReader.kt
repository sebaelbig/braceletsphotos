package com.example.braceletphotos

import android.app.Activity
import android.nfc.NfcAdapter
import android.nfc.Tag

class NfcReader(private val activity: Activity, private val onTag: (String) -> Unit) {
    private val adapter = NfcAdapter.getDefaultAdapter(activity)

    fun start() {
        adapter?.enableReaderMode(
            activity,
            { tag: Tag -> onTag(tag.id.toHex()) },
            NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_NFC_B or
                NfcAdapter.FLAG_READER_NFC_V or NfcAdapter.FLAG_READER_NFC_F or
                NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK,
            null
        )
    }

    fun stop() = adapter?.disableReaderMode(activity)

    private fun ByteArray.toHex() = joinToString("") { "%02X".format(it) }
}
