package com.dxc.document.utility;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ExcelWriterUtil {
	

    private static final Object EXCEL_WRITE_LOCK = new Object();

	private static final Logger logger = LoggerFactory.getLogger(ExcelWriterUtil.class);

	    public static void appendDataToExcel(String filePath, Set<String> headers, List<Map<String, String>> rows) throws IOException {
			logger.info("📄 Starting Excel append: {}", filePath);
			synchronized (EXCEL_WRITE_LOCK) {
				Workbook workbook;
				Sheet sheet;
				File file = new File(filePath);

				if (file.exists()) {
					logger.info("🔄 Existing Excel found. Appending to it.");
					try (FileInputStream fis = new FileInputStream(file)) {
						workbook = new XSSFWorkbook(fis);
					}
				} else {
					logger.info("🆕 Excel file not found. Creating new workbook.");
					workbook = new XSSFWorkbook();
				}

				sheet = (workbook.getNumberOfSheets() == 0)
						? workbook.createSheet("Sheet1")
						: workbook.getSheetAt(0);

				CellStyle wrapStyle = workbook.createCellStyle();
				wrapStyle.setWrapText(true);

				// Write headers if not present
				Row headerRow = sheet.getRow(0);
				if (headerRow == null) {
					headerRow = sheet.createRow(0);
					int col = 0;
					for (String h : headers) {
						Cell cell = headerRow.createCell(col++);
						cell.setCellValue(h);
						cell.setCellStyle(wrapStyle);
					}
					logger.info("✅ Header row created");
				}

				// Write data rows
				int rowNum = sheet.getLastRowNum() + 1;
				for (Map<String, String> data : rows) {
					Row row = sheet.createRow(rowNum++);
					int col = 0;
					for (String key : headers) {
						Cell cell = row.createCell(col++);
						String value = data.getOrDefault(key, "");
						if (value.startsWith("=") || value.startsWith("-")) {
							value = "'" + value;
						}
						cell.setCellValue(value);
						cell.setCellStyle(wrapStyle);
					}
				}

				// Auto-size columns
				int totalCols = headers.size();
				for (int i = 0; i < totalCols; i++) {
					sheet.autoSizeColumn(i);
				}

				// Auto-adjust row height
				for (int r = 1; r <= sheet.getLastRowNum(); r++) {
					Row row = sheet.getRow(r);
					if (row != null) {
						row.setHeight((short) -1);
					}
				}

				try (FileOutputStream fos = new FileOutputStream(filePath)) {
					workbook.write(fos);
					logger.info("✅ Excel write completed: {}", filePath);
				} catch (IOException e) {
					logger.error("❌ Error writing Excel file: {}", e.getMessage(), e);
					throw e;
				} finally {
					workbook.close();
				}
			}
		}

}
