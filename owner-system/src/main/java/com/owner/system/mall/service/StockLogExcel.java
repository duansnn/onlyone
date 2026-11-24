package com.owner.system.mall.service;

import com.owner.system.mall.domain.vo.StockLogVO;
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
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 把库存流水写成一份 xlsx。一行一条流水。
 */
final class StockLogExcel {

    private static final String[] HEADERS = {
            "时间", "商品", "规格", "变动", "变动后库存", "变动后锁定", "原因", "关联订单", "备注"
    };

    private static final int[] WIDTHS = {20, 24, 16, 10, 14, 14, 12, 20, 30};

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private StockLogExcel() {
    }

    static byte[] write(List<StockLogVO> logs) {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            XSSFSheet sheet = workbook.createSheet("库存流水");
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
            for (StockLogVO log : logs) {
                Row row = sheet.createRow(rowIndex++);
                text(row, 0, log.createTime() == null ? "" : TIME.format(log.createTime()), textStyle);
                text(row, 1, log.productName(), textStyle);
                text(row, 2, log.specName(), textStyle);
                number(row, 3, log.changeQty() == null ? 0 : log.changeQty(), numberStyle);
                number(row, 4, log.afterStock() == null ? 0 : log.afterStock(), numberStyle);
                number(row, 5, log.afterLocked() == null ? 0 : log.afterLocked(), numberStyle);
                text(row, 6, log.reasonLabel(), textStyle);
                text(row, 7, log.orderId() == null ? "" : String.valueOf(log.orderId()), textStyle);
                text(row, 8, log.remark(), textStyle);
            }
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("导出库存流水失败", e);
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
