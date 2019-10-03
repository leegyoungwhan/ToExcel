package com.github.ckpoint.toexcel.core;

import com.github.ckpoint.toexcel.annotation.ExcelHeader;
import com.github.ckpoint.toexcel.core.model.CellPosition;
import com.github.ckpoint.toexcel.core.model.ToTitleKey;
import com.github.ckpoint.toexcel.core.style.ToWorkBookStyle;
import com.github.ckpoint.toexcel.core.type.SheetDirection;
import com.github.ckpoint.toexcel.core.type.ToWorkCellType;
import com.github.ckpoint.toexcel.util.ExcelHeaderHelper;
import com.github.ckpoint.toexcel.util.ModelMapperGenerator;
import com.github.ckpoint.toexcel.util.TitleRowHelper;
import lombok.Getter;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * The type To work sheet.
 */
public class ToWorkSheet implements ExcelHeaderHelper, TitleRowHelper {

    private final static int DEFAULT_WIDTH_SIZE = 2560;

    @Getter
    private final ToWorkBook workBook;

    @Getter
    private final String name;
    private final Workbook _wb;
    private final CellPosition cellPosition;

    private Sheet _sheet;

    /**
     * Instantiates a new To work sheet.
     *
     * @param toWorkBook the to work book
     * @param wb         the wb
     */
    public ToWorkSheet(ToWorkBook toWorkBook, Workbook wb) {
        this(toWorkBook, wb, null);
    }

    /**
     * Instantiates a new To work sheet.
     *
     * @param toWorkBook the to work book
     * @param wb         the wb
     * @param name       the name
     */
    public ToWorkSheet(ToWorkBook toWorkBook, Workbook wb, String name) {
        this.workBook = toWorkBook;
        this._wb = wb;

        if (name != null && !name.isEmpty()) {
            this.name = name;
            this._sheet = this._wb.createSheet(name);
        } else {
            this._sheet = this._wb.createSheet();
            this.name = this._sheet.getSheetName();
        }
        this.cellPosition = new CellPosition(_sheet);
    }

    /**
     * Create title cell list.
     *
     * @param width  the width
     * @param values the values
     * @return the list
     */
    public List<ToWorkCell> createTitleCell(int width, String... values) {
        if (values == null || values.length < 1) {
            return new ArrayList<>();
        }

        List<ToWorkCell> cells = new ArrayList<>();

        for (String value : values) {
            Cell pcell = this.cellPosition.nextCell();
            _sheet.setColumnWidth(pcell.getColumnIndex(), DEFAULT_WIDTH_SIZE * width);
            cells.add(new ToWorkCell(this, pcell, value, ToWorkCellType.TITLE));
        }
        return cells;
    }

    /**
     * Create cell to newline list.
     *
     * @param values the values
     * @return the list
     */
    public List<ToWorkCell> createCellToNewline(Object... values) {
        this.newLine();
        return createCell(values);
    }

    /**
     * Create cell list.
     *
     * @param style  the style
     * @param values the values
     * @return the list
     */
    public List<ToWorkCell> createCell(ToWorkBookStyle style, Object... values) {
        if (values == null || values.length < 1) {
            return new ArrayList<>();
        }
        return Arrays.stream(values).map(v -> new ToWorkCell(this, cellPosition.nextCell(), v, style)).collect(Collectors.toList());
    }

    /**
     * Create cell list.
     *
     * @param values the values
     * @return the list
     */
    public List<ToWorkCell> createCell(Object... values) {
        if (values == null || values.length < 1) {
            return new ArrayList<>();
        }
        return Arrays.stream(values).map(v -> new ToWorkCell(this, cellPosition.nextCell(), v)).collect(Collectors.toList());
    }

    /**
     * New line.
     */
    public void newLine() {
        this.cellPosition.newLine();
    }

    /**
     * Map list.
     *
     * @param <T>  the type parameter
     * @param type the type
     * @return the list
     */
    public <T> List<T> map(Class<T> type) {
        Row titleRow = findTitleRow(type, this._sheet);

        Map<Integer, String> excelTitleMap = new HashMap<>();
        for (int i = 0; i < titleRow.getLastCellNum(); i++) {
            excelTitleMap.put(i, titleRow.getCell(i).getStringCellValue());
        }

        List<String> existTitles = IntStream.range(0, titleRow.getLastCellNum()).mapToObj(titleRow::getCell)
                .map(Cell::getStringCellValue).collect(Collectors.toList());

        Field[] fields = type.getDeclaredFields();
        Set<ToTitleKey> keyset = new HashSet<>();
        for (Field field : fields) {
            if (field.getAnnotation(ExcelHeader.class) != null) {
                keyset.add(new ToTitleKey(field, existTitles));
            }
        }
        List<Map<String, Object>> proxyMapList = IntStream.range(titleRow.getRowNum() + 1, this._sheet.getLastRowNum())
                .mapToObj(this._sheet::getRow).map(row -> rowToMap(row, excelTitleMap, keyset)).collect(Collectors.toList());

        return proxyMapList.stream().map(map -> ModelMapperGenerator.enableFieldModelMapper().map(map, type)).collect(Collectors.toList());
    }

    /**
     * Update direction to work sheet.
     *
     * @param sheetDirection the sheet direction
     * @return the to work sheet
     */
    public ToWorkSheet updateDirection(SheetDirection sheetDirection) {
        this.cellPosition.updateDirection(sheetDirection);
        return this;
    }

    /**
     * Skip list.
     *
     * @param cnt the cnt
     * @return the list
     */
    public List<ToWorkCell> skip(int cnt) {
        List<Cell> cells = this.cellPosition.skip(cnt);
        return cells.stream().map(v -> new ToWorkCell(this, v, null)).collect(Collectors.toList());
    }

    /**
     * Merge.
     *
     * @param width  the width
     * @param height the height
     */
    public void merge(int width, int height) {
        this.cellPosition.merge(width, height);
    }

    /**
     * From.
     *
     * @param list the list
     */
    public void from(List list) {
        if (list == null || list.isEmpty()) {
            return;
        }

        this.clear();

        Object obj = list.get(0);
        Field[] fields = obj.getClass().getDeclaredFields();
        List<ToTitleKey> keys = Arrays.stream(fields).filter(field -> field.getAnnotation(ExcelHeader.class) != null)
                .map(ToTitleKey::new).sorted().collect(Collectors.toList());
        keys.forEach(key -> this.createTitleCell(1, key.getHeader().headerName()));
        for (Object o : list) {
            writeObject(o, keys);
        }
    }

    private void writeObject(Object obj, List<ToTitleKey> keys) {
        this.newLine();
        keys.forEach(key -> {
            Object value = "";
            try {
                key.getField().setAccessible(true);
                value = key.getField().get(obj);
            } catch (IllegalAccessException e) {
                //TODO
            }
            this.createCell(value);
        });
    }

    private void clear() {

        for (int i = 0; i < this._sheet.getLastRowNum(); i++) {
            this._sheet.removeRow(this._sheet.getRow(i));
        }
        this.cellPosition.clear();
    }

    private Map<String, Object> rowToMap(Row row, Map<Integer, String> titleMaps, Set<ToTitleKey> setKeys) {

        Map<String, Object> valueMap = new HashMap<>();
        for (int i = 0; i < row.getLastCellNum(); i++) {
            Object value = getCellValue(row.getCell(i));
            String title = titleMaps.get(i);
            if (title == null) {
                break;
            }
            ToTitleKey toTitleKey = setKeys.stream().filter(key -> key.isMyName(title)).findFirst().orElse(null);
            if (toTitleKey == null) {
                continue;
            }

            valueMap.put(toTitleKey.getKey(), value);
        }
        return valueMap;
    }

    private Object getCellValue(Cell cell) {
        switch (cell.getCellType()) {
            case Cell.CELL_TYPE_NUMERIC:
                return cell.getNumericCellValue();
            case Cell.CELL_TYPE_BOOLEAN:
                return cell.getBooleanCellValue();
            default:
                return cell.getStringCellValue();
        }
    }

    private Row getLastRow() {
        return this._sheet.getRow(this._sheet.getLastRowNum());
    }
}
