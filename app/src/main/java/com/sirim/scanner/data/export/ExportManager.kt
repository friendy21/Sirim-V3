package com.sirim.scanner.data.export

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.sirim.scanner.data.db.SirimRecord
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import org.apache.poi.ss.usermodel.BorderStyle
import org.apache.poi.ss.usermodel.HorizontalAlignment
import org.apache.poi.ss.util.CellRangeAddress
import org.apache.poi.xssf.usermodel.XSSFCellStyle
import org.apache.poi.xssf.usermodel.XSSFWorkbook

class ExportManager(private val context: Context) {

    private val headers = listOf(
        "SIRIM Serial No.",
        "Batch No.",
        "Brand/Trademark",
        "Model",
        "Type",
        "Rating",
        "Size"
    )

    fun exportToPdf(records: List<SirimRecord>): Uri {
        val file = File(context.getExternalFilesDir(null), "sirim_records.pdf")
        PdfWriter(file).use { writer ->
            val document = Document(com.itextpdf.kernel.pdf.PdfDocument(writer))
            document.add(Paragraph("SIRIM Records"))
            val table = Table(headers.size.toFloat())
            headers.forEach { header ->
                table.addHeaderCell(Cell().add(Paragraph(header)))
            }
            records.forEach { record ->
                record.toFieldList().forEach { value ->
                    table.addCell(Cell().add(Paragraph(value)))
                }
            }
            document.add(table)
            document.close()
        }
        return toFileUri(file)
    }

    fun exportToCsv(records: List<SirimRecord>): Uri {
        val file = File(context.getExternalFilesDir(null), "sirim_records.csv")
        FileOutputStream(file).use { output ->
            OutputStreamWriter(output).use { writer ->
                writer.appendLine(headers.joinToString(","))
                records.forEach { record ->
                    writer.appendLine(record.toFieldList().joinToString(","))
                }
            }
        }
        return toFileUri(file)
    }

    fun exportToExcel(records: List<SirimRecord>): Uri {
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("SIRIM")
        val headerStyle: XSSFCellStyle = workbook.createCellStyle().apply {
            alignment = HorizontalAlignment.CENTER
            borderBottom = BorderStyle.MEDIUM
        }
        val headerRow = sheet.createRow(0)
        headers.forEachIndexed { index, header ->
            sheet.setColumnWidth(index, 20_000)
            val cell = headerRow.createCell(index)
            cell.setCellValue(header)
            cell.cellStyle = headerStyle
        }
        records.forEachIndexed { rowIndex, record ->
            val row = sheet.createRow(rowIndex + 1)
            record.toFieldList().forEachIndexed { columnIndex, value ->
                row.createCell(columnIndex).setCellValue(value)
            }
        }
        sheet.setAutoFilter(CellRangeAddress(0, records.size, 0, headers.lastIndex))
        val file = File(context.getExternalFilesDir(null), "sirim_records.xlsx")
        file.outputStream().use { output ->
            workbook.use { it.write(output) }
        }
        return toFileUri(file)
    }

    private fun toFileUri(file: File): Uri = FileProvider.getUriForFile(
        context,
        context.packageName + ".provider",
        file
    )

    private fun SirimRecord.toFieldList(): List<String> = listOf(
        sirimSerialNo,
        batchNo,
        brandTrademark,
        model,
        type,
        rating,
        size
    )
}
