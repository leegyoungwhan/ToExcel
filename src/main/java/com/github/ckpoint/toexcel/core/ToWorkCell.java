package com.github.ckpoint.toexcel.core;

import com.github.ckpoint.toexcel.core.style.ToWorkBookStyle;
import com.github.ckpoint.toexcel.core.type.ToWorkCellType;
import com.github.ckpoint.toexcel.util.ExcelHeaderHelper;
import lombok.Getter;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.RichTextString;

import java.util.Calendar;
import java.util.Date;

/**
 * The type To work cell.
 */
public class ToWorkCell implements ExcelHeaderHelper {

    private final ToWorkSheet sheet;
    private final Cell _cell;
    private final ToWorkCellType cellType;
    @Getter
    private final ToWorkBookStyle style;

    /**
     * Instantiates a new To work cell.
     *
     * @param sheet the sheet
     * @param cell  the cell
     * @param value the value
     * @param type  the type
     */
    public ToWorkCell(ToWorkSheet sheet, Cell cell, Object value, ToWorkCellType type) {

        this.sheet = sheet;
        this._cell = cell;
        this.cellType = type;
        this.style = new ToWorkBookStyle(value);
        if (type.isTitle()) {
            this.style.updateTitleType();
        }

        this._cell.setCellStyle(this.sheet.getWorkBook().createStyle(this));

        if (value == null) {
            return;
        }
        this.updateValue(value);
    }

    /**
     * Instantiates a new To work cell.
     *
     * @param sheet the sheet
     * @param cell  the cell
     * @param value the value
     */
    public ToWorkCell(ToWorkSheet sheet, Cell cell, Object value) {
        this(sheet, cell, value, ToWorkCellType.VALUE);
    }

    /**
     * Update style.
     *
     * @param cellStyle the cell style
     */
    public void updateStyle(CellStyle cellStyle) {
        this._cell.setCellStyle(cellStyle);
    }

    private ToWorkCell updateValue(Object value) {
        if (value instanceof Double) {
            this._cell.setCellValue((double) value);
        } else if (value instanceof Boolean) {
            this._cell.setCellValue((Boolean) value);
        } else if (value instanceof RichTextString) {
            this._cell.setCellValue((RichTextString) value);
        } else if (value instanceof Date) {
            this._cell.setCellValue((Date) value);
        } else if (value instanceof Calendar) {
            this._cell.setCellValue((Calendar) value);
        } else {
            this._cell.setCellValue(String.valueOf(value));
        }
        return this;
    }
}
