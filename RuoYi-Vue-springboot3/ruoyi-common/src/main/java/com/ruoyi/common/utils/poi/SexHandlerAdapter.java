package com.ruoyi.common.utils.poi;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Workbook;

public class SexHandlerAdapter implements ExcelHandlerAdapter{
    @Override
    public Object format(Object value, String[] args, Cell cell, Workbook wb) {
        String cellValue = (String) value;
        System.out.println("cellValue = " + cellValue);
        System.out.println("args = " + String.join(",", args));
        if ("1".equals(cellValue)) return args[0];
        if ("0".equals(cellValue)) return args[1];
        return "未知";
    }
}
