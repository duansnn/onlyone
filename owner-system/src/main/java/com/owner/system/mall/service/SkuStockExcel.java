package com.owner.system.mall.service;

import com.owner.system.mall.domain.vo.SkuStockVO;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

/**
 * 把 SKU 库存写成一份 xlsx。一行一个规格。
 */
final class SkuStockExcel {

    private static final String[] HEADERS = {
            "商品", "规格", "货号", "库存", "锁定量", "可售库存", "安全阈值", "状态"
    };

    private static final int[] WIDTHS = {24, 16, 16, 10, 10, 12, 12, 12};

    private SkuStockExcel() {
    }

    static byte[] write(List<SkuStockVO> rows) {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            XSSFSheet sheet = workbook.createSheet("库存");
            CellStyle headerStyle = headerStyle(workbook);
            CellStyle textStyle = textStyle(workbook);
            CellStyle numberStyle = numberStyle(workbook);

            Row header = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(HEADERS[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, WIDTHS[i] * 256);
            }
            sheet.createFreezePane(0, 1);

            int rowIndex = 1;
            for (SkuStockVO vo : rows) {
                Row row = sheet.createRow(rowIndex++);
                text(row, 0, vo.productName(), textStyle);
                text(row, 1, vo.specName(), textStyle);
                text(row, 2, vo.skuCode(), textStyle);
                number(row, 3, vo.stock() == null ? 0 : vo.stock(), numberStyle);
                number(row, 4, vo.lockedStock() == null ? 0 : vo.lockedStock(), numberStyle);
                number(row, 5, vo.availableStock() == null ? 0 : vo.availableStock(), numberStyle);
                number(row, 6, vo.safeStock() == null ? 0 : vo.safeStock(), numberStyle);
                text(row, 7, vo.statusLabel(), textStyle);
            }
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("导出库存失败", e);
        }
    }

    private static void text(Row row, int column, String value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value == null ? "" : value);
        cell.setCellStyle(style);
    }

    private static void number(Row row, int column, int value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private static CellStyle headerStyle(XSSFWorkbook workbook) {
        CellStyle style = textStyle(workbook);
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private static CellStyle textStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setWrapText(true);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private static CellStyle numberStyle(XSSFWorkbook workbook) {
        CellStyle style = textStyle(workbook);
        style.setAlignment(HorizontalAlignment.RIGHT);
        return style;
    }
}
