package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.CourseEntity
import com.example.data.NoteEntity
import com.example.data.UserEntity
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfExportHelper {

    fun exportFavoritesToPdf(
        context: Context,
        user: UserEntity,
        courses: List<CourseEntity>,
        notes: List<NoteEntity>,
        onSuccess: (String) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val pdfDocument = PdfDocument()
        val paint = Paint()
        
        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas
        
        var currentY = 40f

        // Helper to handle new page creation
        fun checkAndCreateNewPage() {
            if (currentY > 780f) {
                // Draw page number footer before closing the page
                paint.color = Color.DKGRAY
                paint.textSize = 9f
                paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
                canvas.drawText("Page $pageNumber", 510f, 810f, paint)
                canvas.drawText("Exported from EduHub", 40f, 810f, paint)
                
                pdfDocument.finishPage(page)
                
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                currentY = 50f
            }
        }

        try {
            // 1. HEADER BANNER
            paint.style = Paint.Style.FILL
            paint.color = 0xFF1E3A8A.toInt() // Dark Navy/Blue
            canvas.drawRect(40f, currentY, 555f, currentY + 60f, paint)
            
            paint.color = Color.WHITE
            paint.textSize = 18f
            paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            canvas.drawText("EduHub Saved Favorites", 55f, currentY + 38f, paint)
            
            currentY += 80f
            
            // 2. USER INFO SECTION
            paint.color = Color.BLACK
            paint.textSize = 11f
            paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            canvas.drawText("STUDENT REVISION REPORT", 40f, currentY, paint)
            currentY += 16f
            
            paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            paint.color = Color.DKGRAY
            canvas.drawText("User Name: ${user.name}", 40f, currentY, paint)
            canvas.drawText("Email: ${user.email}", 320f, currentY, paint)
            currentY += 15f
            
            val formattedDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            canvas.drawText("Exported On: $formattedDate", 40f, currentY, paint)
            canvas.drawText("Total Saved Items: ${courses.size + notes.size}", 320f, currentY, paint)
            currentY += 25f
            
            // Draw Divider
            paint.color = Color.LTGRAY
            paint.strokeWidth = 1f
            canvas.drawLine(40f, currentY, 555f, currentY, paint)
            currentY += 25f
            
            // 3. COURSES SECTION
            if (courses.isNotEmpty()) {
                paint.color = 0xFF0D9488.toInt() // Teal
                paint.textSize = 14f
                paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                canvas.drawText("Wishlisted Courses (${courses.size})", 40f, currentY, paint)
                currentY += 20f
                
                courses.forEachIndexed { index, course ->
                    checkAndCreateNewPage()
                    
                    // Box background for course entry
                    paint.style = Paint.Style.STROKE
                    paint.color = 0xFFE5E7EB.toInt() // light gray border
                    paint.strokeWidth = 0.8f
                    canvas.drawRect(40f, currentY, 555f, currentY + 54f, paint)
                    
                    // Accent vertical line on the left of item
                    paint.style = Paint.Style.FILL
                    paint.color = 0xFF0D9488.toInt()
                    canvas.drawRect(40f, currentY, 44f, currentY + 54f, paint)
                    
                    // Text details
                    paint.color = Color.BLACK
                    paint.textSize = 11f
                    paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                    
                    // Slice title if too long
                    val displayTitle = if (course.title.length > 55) course.title.take(52) + "..." else course.title
                    canvas.drawText("${index + 1}. $displayTitle", 52f, currentY + 18f, paint)
                    
                    paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.ITALIC)
                    paint.textSize = 9.5f
                    paint.color = Color.DKGRAY
                    canvas.drawText("Category: ${course.category} | Instructor: ${course.instructorName}", 52f, currentY + 34f, paint)
                    
                    paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
                    canvas.drawText("Rating: ${course.rating} ⭐ (${course.reviewsCount} reviews)", 52f, currentY + 47f, paint)
                    
                    // Price
                    paint.color = 0xFF2563EB.toInt() // primary blue
                    paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                    paint.textSize = 13f
                    val priceStr = if (course.price == 0.0) "FREE" else "$${String.format(Locale.US, "%.2f", course.price)}"
                    canvas.drawText(priceStr, 480f, currentY + 32f, paint)
                    
                    currentY += 66f
                    checkAndCreateNewPage()
                }
                currentY += 15f
                checkAndCreateNewPage()
            }
            
            // 4. NOTES SECTION
            if (notes.isNotEmpty()) {
                paint.color = 0xFF7C3AED.toInt() // Purple
                paint.textSize = 14f
                paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                canvas.drawText("Saved PDF Lecture Notes (${notes.size})", 40f, currentY, paint)
                currentY += 20f
                
                notes.forEachIndexed { index, note ->
                    checkAndCreateNewPage()
                    
                    // Box background for note entry
                    paint.style = Paint.Style.STROKE
                    paint.color = 0xFFE5E7EB.toInt() // light gray border
                    paint.strokeWidth = 0.8f
                    canvas.drawRect(40f, currentY, 555f, currentY + 54f, paint)
                    
                    // Accent vertical line on the left of item
                    paint.style = Paint.Style.FILL
                    paint.color = 0xFF7C3AED.toInt()
                    canvas.drawRect(40f, currentY, 44f, currentY + 54f, paint)
                    
                    // Text details
                    paint.color = Color.BLACK
                    paint.textSize = 11f
                    paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                    
                    val displayTitle = if (note.title.length > 55) note.title.take(52) + "..." else note.title
                    canvas.drawText("${index + 1}. $displayTitle", 52f, currentY + 18f, paint)
                    
                    paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.ITALIC)
                    paint.textSize = 9.5f
                    paint.color = Color.DKGRAY
                    canvas.drawText("Category: ${note.category} | Contributor: ${note.sellerName}", 52f, currentY + 34f, paint)
                    
                    paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
                    canvas.drawText("Rating: ${note.rating} ⭐ (${note.reviewsCount} downloads)", 52f, currentY + 47f, paint)
                    
                    // Price
                    paint.color = 0xFF7C3AED.toInt()
                    paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                    paint.textSize = 13f
                    val priceStr = if (note.price == 0.0) "FREE" else "$${String.format(Locale.US, "%.2f", note.price)}"
                    canvas.drawText(priceStr, 480f, currentY + 32f, paint)
                    
                    currentY += 66f
                    checkAndCreateNewPage()
                }
            }
            
            // Empty state check
            if (courses.isEmpty() && notes.isEmpty()) {
                paint.color = Color.GRAY
                paint.textSize = 12f
                paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.ITALIC)
                canvas.drawText("You have no favorites or wishlisted resources stored currently.", 45f, currentY + 20f, paint)
                currentY += 50f
            }

            // Draw final page footer
            paint.color = Color.DKGRAY
            paint.textSize = 9f
            paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            canvas.drawText("Page $pageNumber", 510f, 810f, paint)
            canvas.drawText("Exported from EduHub", 40f, 810f, paint)
            
            pdfDocument.finishPage(page)
            
            // Save file
            val directory = context.getExternalFilesDir(null) ?: context.filesDir
            val pdfFile = File(directory, "EduHub_Favorites_Summary.pdf")
            
            val outputStream = FileOutputStream(pdfFile)
            pdfDocument.writeTo(outputStream)
            outputStream.flush()
            outputStream.close()
            pdfDocument.close()
            
            onSuccess(pdfFile.absolutePath)
        } catch (e: Exception) {
            e.printStackTrace()
            try {
                pdfDocument.close()
            } catch (ignored: Exception) {}
            onError(e.message ?: "Unknown PDF Generation Error")
        }
    }

    /**
     * Launch an intent to open the generated PDF with third-party applications.
     */
    fun openPdfFile(context: Context, filePath: String) {
        try {
            val file = File(filePath)
            if (!file.exists()) {
                Toast.makeText(context, "Error: PDF file does not exist.", Toast.LENGTH_SHORT).show()
                return
            }
            
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "com.example.fileprovider",
                file
            )
            
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            val chooser = Intent.createChooser(intent, "Open PDF Revision Summary")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "Error opening PDF reader: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
