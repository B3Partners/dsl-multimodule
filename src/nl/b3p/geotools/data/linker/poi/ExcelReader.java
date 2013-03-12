package nl.b3p.geotools.data.linker.poi;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

/**
 *
 * @author Boy de Wit
 */
public class ExcelReader {

    private HSSFWorkbook readFile(String fileName)
            throws FileNotFoundException, IOException {

        return new HSSFWorkbook(new FileInputStream(fileName));
    }

    public List<String> getRecord(String fileName, Integer index, String searchValue)
            throws FileNotFoundException, IOException {

        List<String> record = new ArrayList();

        HSSFWorkbook wb = readFile(fileName);

        for (int k = 0; k < wb.getNumberOfSheets(); k++) {
            HSSFSheet sheet = wb.getSheetAt(k);
            int rows = sheet.getPhysicalNumberOfRows();

            for (int r = 0; r < rows; r++) {
                HSSFRow row = sheet.getRow(r);
                if (row == null) {
                    continue;
                }

                int cells = row.getPhysicalNumberOfCells();

                for (int c = 0; c < cells; c++) {
                    HSSFCell cell = row.getCell(c);
                    String value = null;

                    if (cell == null) {
                        continue;
                    }

                    switch (cell.getCellType()) {

                        case HSSFCell.CELL_TYPE_FORMULA:
                            value = cell.getCellFormula();
                            break;

                        case HSSFCell.CELL_TYPE_NUMERIC:
                            value = String.valueOf((int) cell.getNumericCellValue());
                            break;

                        case HSSFCell.CELL_TYPE_STRING:
                            value = cell.getStringCellValue();
                            break;

                        default:
                    }

                    if (r == 0 && index > 0) {
                        record.add(value);
                    }

                    if (value != null && c == index
                            && value.equalsIgnoreCase(searchValue)) {

                        for (int d = 0; d < cells; d++) {
                            HSSFCell tempCell = row.getCell(d);

                            if (tempCell == null) {
                                continue;
                            }

                            switch (tempCell.getCellType()) {
                                case HSSFCell.CELL_TYPE_FORMULA:
                                    value = tempCell.getCellFormula();
                                    break;

                                case HSSFCell.CELL_TYPE_NUMERIC:
                                    value = String.valueOf((int) tempCell.getNumericCellValue());
                                    break;

                                case HSSFCell.CELL_TYPE_STRING:
                                    value = tempCell.getStringCellValue();
                                    break;

                                default:
                            }

                            record.add(value);
                        }

                        break;
                    }

                } // end cells

            } // end rows

        } // end sheets

        return record;
    }

    public List<String> getColumns(String fileName)
            throws FileNotFoundException, IOException {

        List<String> columns = new ArrayList();

        HSSFWorkbook wb = readFile(fileName);

        for (int k = 0; k < wb.getNumberOfSheets(); k++) {
            HSSFSheet sheet = wb.getSheetAt(k);
            int rows = sheet.getPhysicalNumberOfRows();
            for (int r = 0; r < 1; r++) {
                HSSFRow row = sheet.getRow(r);
                if (row == null) {
                    continue;
                }

                int cells = row.getPhysicalNumberOfCells();
                for (int c = 0; c < cells; c++) {
                    HSSFCell cell = row.getCell(c);
                    String value = null;

                    if (cell == null) {
                        continue;
                    }

                    switch (cell.getCellType()) {
                        case HSSFCell.CELL_TYPE_FORMULA:
                            value = cell.getCellFormula();
                            break;

                        case HSSFCell.CELL_TYPE_NUMERIC:
                            value = String.valueOf((int) cell.getNumericCellValue());
                            break;

                        case HSSFCell.CELL_TYPE_STRING:
                            value = cell.getStringCellValue();
                            break;

                        default:
                    }

                    if (value != null) {                        
                        columns.add(value.toLowerCase());
                    }
                }
            }
        }

        return columns;
    }
}