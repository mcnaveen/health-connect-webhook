package com.hcwebhook.app

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

object ProtoSchemaExporter {
    private const val ASSET_NAME = "health_payload.proto"
    private const val EXPORT_FILE_NAME = "health_payload.proto"

    fun share(context: Context) {
        val exportDir = File(context.cacheDir, "exports").also { it.mkdirs() }
        val exportFile = File(exportDir, EXPORT_FILE_NAME)
        context.assets.open(ASSET_NAME).use { input ->
            exportFile.outputStream().use { output -> input.copyTo(output) }
        }
        val fileUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            exportFile
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, fileUri)
            putExtra(Intent.EXTRA_SUBJECT, EXPORT_FILE_NAME)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(
            Intent.createChooser(
                shareIntent,
                context.getString(R.string.webhooks_delivery_share_proto_title)
            )
        )
    }
}
