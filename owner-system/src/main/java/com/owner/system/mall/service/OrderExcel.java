package com.owner.system.mall.service;

import com.owner.common.core.utils.StringUtils;
import com.owner.system.mall.domain.entity.OrderInfo;
import com.owner.system.mall.domain.entity.OrderItem;
import com.owner.system.mall.domain.enums.OrderStatus;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * 把订单写成一份 xlsx。一行一单，商品行拼在同一格里。
 */
final class OrderExcel {

    private static final String[] HEADERS = {
            "订单号", "下单时间", "状态", "收货人", "手机", "地址", "商品", "应付金额", "快递公司", "运单号"
    };

    private static final int[] WIDTHS = {22, 20, 12, 14, 16, 42, 48, 14, 14, 24};

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private OrderExcel() {
    }

    static byte[] write(List<OrderInfo> orders, Map<Long, List<OrderItem>> itemsByOrder) {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            XSSFSheet sheet = workbook.createSheet("订单");
            CellStyle headerStyle = headerStyle(workbook);
            CellStyle textStyle = textStyle(workbook);
            CellStyle moneyStyle = moneyStyle(workbook);

            Row header = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(HEADERS[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, WIDTHS[i] * 256);
            }
            sheet.createFreezePane(0, 1);

            int rowIndex = 1;
            for (OrderInfo order : orders) {
                Row row = sheet.createRow(rowIndex++);
                text(row, 0, order.getOrderNo(), textStyle);
                text(row, 1, order.getCreateTime() == null ? "" : TIME.format(order.getCreateTime()), textStyle);
                text(row, 2, statusLabel(order.getStatus()), textStyle);
                text(row, 3, order.getReceiverName(), textStyle);
                text(row, 4, order.getReceiverMobile(), textStyle);
                text(row, 5, order.getReceiverAddress(), textStyle);
                text(row, 6, productText(itemsByOrder.get(order.getId())), textStyle);
                Cell amount = row.createCell(7);
                amount.setCellValue(centsToYuan(order.getPayAmount()));
                amount.setCellStyle(moneyStyle);
                text(row, 8, order.getShipCompany(), textStyle);
                text(row, 9, order.getShipNo(), textStyle);
            }
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("导出订单失败", e);
        }
    }

    private static String statusLabel(String status) {
        OrderStatus parsed = OrderStatus.of(status);
        return parsed == null ? (status == null ? "" : status) : parsed.getLabel();
    }

    private static String productText(List<OrderItem> items) {
        if (items == null || items.isEmpty()) {
            return "";
        }
        StringBuilder text = new StringBuilder();
        for (OrderItem item : items) {
            if (!text.isEmpty()) {
                text.append('；');
            }
            text.append(item.getProductName() == null ? "" : item.getProductName());
            if (StringUtils.isNotBlank(item.getSpecName())) {
                text.append(' ').append(item.getSpecName().trim());
            }
            text.append(" × ").append(item.getQuantity() == null ? 0 : item.getQuantity());
        }
        return text.toString();
    }

    private static double centsToYuan(Long cents) {
        return (cents == null ? 0L : cents) / 100.0d;
    }

    private static void text(Row row, int column, String value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value == null ? "" : value);
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
        style.setVerticalAlignment(org.apache.poi.ss.usermodel.VerticalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private static CellStyle moneyStyle(XSSFWorkbook workbook) {
        CellStyle style = textStyle(workbook);
        style.setDataFormat(workbook.createDataFormat().getFormat("0.00"));
        style.setAlignment(HorizontalAlignment.RIGHT);
        return style;
    }
}
