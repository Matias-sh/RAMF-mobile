package com.cocido.ramf.utils

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.FileProvider
import com.github.mikephil.charting.charts.LineChart
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

object ExportUtils {
    
    fun exportChartAsPNG(
        context: Context,
        chart: LineChart,
        fileName: String? = null,
        onSuccess: (Uri) -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            val bitmap = chart.chartBitmap
            if (bitmap == null) {
                onError("No se pudo generar la imagen del gráfico")
                return
            }
            
            val finalFileName = fileName ?: generateFileName("chart", "png")
            val file = saveToInternalStorage(context, bitmap, finalFileName)
            
            if (file != null) {
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
                onSuccess(uri)
            } else {
                onError("Error al guardar el archivo")
            }
        } catch (e: Exception) {
            onError("Error al exportar: ${e.localizedMessage}")
        }
    }
    
    fun exportChartAsJPG(
        context: Context,
        chart: LineChart,
        fileName: String? = null,
        onSuccess: (Uri) -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            val bitmap = chart.chartBitmap
            if (bitmap == null) {
                onError("No se pudo generar la imagen del gráfico")
                return
            }
            
            val finalFileName = fileName ?: generateFileName("chart", "jpg")
            val file = saveToInternalStorage(context, bitmap, finalFileName, Bitmap.CompressFormat.JPEG)
            
            if (file != null) {
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
                onSuccess(uri)
            } else {
                onError("Error al guardar el archivo")
            }
        } catch (e: Exception) {
            onError("Error al exportar: ${e.localizedMessage}")
        }
    }
    
    private fun saveToInternalStorage(
        context: Context,
        bitmap: Bitmap,
        fileName: String,
        format: Bitmap.CompressFormat = Bitmap.CompressFormat.PNG
    ): File? {
        return try {
            val directory = File(context.filesDir, "exports")
            if (!directory.exists()) {
                directory.mkdirs()
            }
            
            val file = File(directory, fileName)
            val outputStream = FileOutputStream(file)
            
            bitmap.compress(format, Constants.EXPORT_QUALITY, outputStream)
            outputStream.flush()
            outputStream.close()
            
            file
        } catch (e: IOException) {
            null
        }
    }
    
    private fun generateFileName(prefix: String, extension: String): String {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            .format(Date())
        return "${prefix}_${timestamp}.$extension"
    }
    
    fun shareFile(context: Context, uri: Uri, title: String = "Compartir gráfico") {
        try {
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_STREAM, uri)
                type = "image/*"
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            context.startActivity(Intent.createChooser(shareIntent, title))
        } catch (e: Exception) {
            // Handle error
        }
    }
    
    fun exportChartData(
        context: Context,
        data: List<Pair<String, List<Pair<Long, Float>>>>,
        fileName: String? = null,
        onSuccess: (Uri) -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            val finalFileName = fileName ?: generateFileName("data", "csv")
            val csv = generateCSV(data)
            val file = saveTextToInternalStorage(context, csv, finalFileName)
            
            if (file != null) {
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
                onSuccess(uri)
            } else {
                onError("Error al guardar el archivo CSV")
            }
        } catch (e: Exception) {
            onError("Error al exportar datos: ${e.localizedMessage}")
        }
    }
    
    private fun generateCSV(data: List<Pair<String, List<Pair<Long, Float>>>>): String {
        if (data.isEmpty()) return ""
        
        val StringBuilder = StringBuilder()
        val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        
        // Header
        StringBuilder.append("Fecha")
        data.forEach { (parameter, _) ->
            StringBuilder.append(",$parameter")
        }
        StringBuilder.append("\n")
        
        // Find all unique timestamps
        val allTimestamps = data.flatMap { it.second.map { pair -> pair.first } }.toSet().sorted()
        
        // Data rows
        allTimestamps.forEach { timestamp ->
            StringBuilder.append(dateFormatter.format(Date(timestamp)))
            
            data.forEach { (_, values) ->
                val value = values.find { it.first == timestamp }?.second
                StringBuilder.append(",${value ?: ""}")
            }
            StringBuilder.append("\n")
        }
        
        return StringBuilder.toString()
    }
    
    private fun saveTextToInternalStorage(
        context: Context,
        text: String,
        fileName: String
    ): File? {
        return try {
            val directory = File(context.filesDir, "exports")
            if (!directory.exists()) {
                directory.mkdirs()
            }
            
            val file = File(directory, fileName)
            file.writeText(text)
            file
        } catch (e: IOException) {
            null
        }
    }
}