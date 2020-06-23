package nl.b3p.geotools.data.linker.poi;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFDateUtil;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

/**
 *
 * @author Boy de Wit
 */
public class ExcelReader {

    private final DecimalFormatSymbols symbols = new DecimalFormatSymbols();
    public static final Locale LOCALE_NL = new Locale("nl", "NL");
    public static final String DATE_FORMAT = "dd-MM-yyyy";
    
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

                        case FORMULA:
                            value = cell.getCellFormula();
                            break;

                        case NUMERIC:
                            value = String.valueOf((int) cell.getNumericCellValue());

                            break;

                        case STRING:
                            value = cell.getStringCellValue();
                            break;

                        default:
                    }

                    if (r == 0 && index > -1) {
                        record.add(value);
                    }

                    if (value != null && c == index
                            && value.equalsIgnoreCase(searchValue)) {

                        for (int d = 0; d < cells; d++) {
                            HSSFCell tempCell = row.getCell(d);

                            if (tempCell == null) {
                                record.add(null);
                                
                                continue;
                            }

                            switch (tempCell.getCellType()) {
                                case FORMULA:
                                    try {
                                        String temp = tempCell.getStringCellValue();
                                        value = temp;
                                    } catch (IllegalStateException ise) {
                                        double num = tempCell.getNumericCellValue();
                                        value = Double.toString(num);
                                    }

                                    break;

                                case NUMERIC:
                                    HSSFCellStyle style = tempCell.getCellStyle();
                                    if (HSSFDateUtil.isCellDateFormatted(tempCell)) {
                                        value = getDateValue(tempCell);
                                    } else {
                                        value = getNumericValue(tempCell).toString();
                                    }

                                    break;

                                case STRING:
                                    value = tempCell.getStringCellValue();
                                    break;

                                default:
                                    value = null;
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
                        case FORMULA:
                            value = cell.getCellFormula();
                            break;

                        case NUMERIC:
                            value = String.valueOf((int) cell.getNumericCellValue());
                            break;

                        case STRING:
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

    protected Object getDateValueFromJavaNumber(HSSFCell cell) {
        double numericValue = cell.getNumericCellValue();
        BigDecimal numericValueBd = new BigDecimal(String.valueOf(numericValue));
        numericValueBd = stripTrailingZeros(numericValueBd);

        return numericValueBd.longValue();
    }

    protected String getDateValue(HSSFCell cell) {

        double numericValue = cell.getNumericCellValue();
        Date date = HSSFDateUtil.getJavaDate(numericValue);
        
        // Add the timezone offset again because it was subtracted 
        // automatically by Apache-POI (we need UTC)
        long tzOffset = TimeZone.getDefault().getOffset(date.getTime());
        date = new Date(date.getTime() + tzOffset);

        return new SimpleDateFormat(DATE_FORMAT, LOCALE_NL).format(date);
    }
    
    private BigDecimal stripTrailingZeros(BigDecimal value) {
        if (value.scale() <= 0) {
            return value;
        }

        String valueAsString = String.valueOf(value);
        int idx = valueAsString.indexOf(".");
        if (idx == -1) {
            return value;
        }

        for (int i = valueAsString.length() - 1; i > idx; i--) {
            if (valueAsString.charAt(i) == '0') {
                valueAsString = valueAsString.substring(0, i);
            } else if (valueAsString.charAt(i) == '.') {
                valueAsString = valueAsString.substring(0, i);
                // Stop when decimal point is reached
                break;
            } else {
                break;
            }
        }
        BigDecimal result = new BigDecimal(valueAsString);
        
        return result;
    }

    protected BigDecimal getNumericValue(HSSFCell cell) {
        String formatString = cell.getCellStyle().getDataFormatString();
        String resultString = null;
        double cellValue = cell.getNumericCellValue();

        if ((formatString != null)) {
            if (!formatString.equals("General") && !formatString.equals("@")) {

                DecimalFormat nf = new DecimalFormat(formatString, symbols);
                resultString = nf.format(cellValue);
            }
        }

        BigDecimal result;
        if (resultString != null) {
            try {
                result = new BigDecimal(resultString);
            } catch (NumberFormatException e) {
                result = toBigDecimal(cellValue);
            }
        } else {
            result = toBigDecimal(cellValue);
        }
        
        return result;
    }
    
    private BigDecimal toBigDecimal(double cellValue) {
        String resultString = String.valueOf(cellValue);
        
        // To ensure that intergral numbers do not have decimal point and trailing zero
        // (to restore backward compatibility and provide a string representation consistent with Excel)
        if (resultString.endsWith(".0")) {
            resultString = resultString.substring(0, resultString.length() - 2);
        }
        
        BigDecimal result = new BigDecimal(resultString);
        
        return result;

    }
}