package com.owner.system.mall.service;

import com.owner.system.mall.domain.entity.Product;
import com.owner.system.mall.domain.entity.ProductSku;
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
import java.util.Map;

/**
 * 把商品写成一份 xlsx。一行一商品，规格拼在同一格里。
 */
final class ProductExcel {

    private static final String[] HEADERS = {
            "商品名", "卖点", "类目", "状态", "价格区间", "总库存", "可售库存", "规格明细", "更新时间"
    };

    private static final int[] WIDTHS = {26, 34, 14, 12, 16, 12, 12, 46, 20};

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private ProductExcel() {
    }

    static byte[] write(List<Product> products,
                        Map<Long, List<ProductSku>> skuMap,
                        Map<Long, String> categoryNames,
                        Map<Long, String> statusLabels,
                        Map<Long, Long> totalStock,
                        Map<Long, Long> totalAvailable) {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            XSSFSheet sheet = workbook.createSheet("商品");
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
            for (Product product : products) {
                Row row = sheet.createRow(rowIndex++);
                text(row, 0, product.getName(), textStyle);
                text(row, 1, product.getSellingPoint(), textStyle);
                text(row, 2, categoryNames.getOrDefault(product.getCategoryId(), ""), textStyle);
                text(row, 3, statusLabels.getOrDefault(product.getId(), product.getStatus()), textStyle);
                text(row, 4, priceRange(skuMap.get(product.getId())), textStyle);
                number(row, 5, totalStock.getOrDefault(product.getId(), 0L), numberStyle);
                number(row, 6, totalAvailable.getOrDefault(product.getId(), 0L), numberStyle);
                text(row, 7, specText(skuMap.get(product.getId())), textStyle);
                text(row, 8, product.getUpdateTime() == null ? "" : TIME.format(product.getUpdateTime()), textStyle);
            }
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("导出商品失败", e);
        }
    }

    private static String priceRange(List<ProductSku> skus) {
        if (skus == null || skus.isEmpty()) {
            return "";
        }
        long min = Long.MAX_VALUE;
        long max = Long.MIN_VALUE;
        for (ProductSku sku : skus) {
            Long price = sku.getPrice();
            if (price == null) {
                continue;
            }
            min = Math.min(min, price);
            max = Math.max(max, price);
        }
        if (min == Long.MAX_VALUE) {
            return "";
        }
        String low = String.format("%.2f", min / 100.0d);
        if (min == max) {
            return "¥" + low;
        }
        return "¥" + low + " ~ ¥" + String.format("%.2f", max / 100.0d);
    }

    private static String specText(List<ProductSku> skus) {
        if (skus == null || skus.isEmpty()) {
            return "";
        }
        StringBuilder text = new StringBuilder();
        for (ProductSku sku : skus) {
            if (!text.isEmpty()) {
                text.append('；');
            }
            String name = sku.getSpecName() == null ? "" : sku.getSpecName().trim();
            text.append(name.isEmpty() ? "默认规格" : name);
            Long price = sku.getPrice();
            if (price != null) {
                text.append(" ¥").append(String.format("%.2f", price / 100.0d));
            }
            text.append(" / 库存 ").append(sku.getStock() == null ? 0 : sku.getStock());
        }
        return text.toString();
    }

    private static void text(Row row, int column, String value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value == null ? "" : value);
        cell.setCellStyle(style);
    }

    private static void number(Row row, int column, long value, CellStyle style) {
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
