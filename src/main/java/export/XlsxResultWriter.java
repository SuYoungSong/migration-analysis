package export;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@Slf4j
@Setter
public class XlsxResultWriter implements ResultWriter {

    private String title = "Analysis Result";

    @Override
    public String getFileExtension() {
        return "xlsx";
    }

    /**
     * List<Map<String, String>>를 입력받습니다. List에 포함된 Map는 각 행마다 들어갈 내용이다.
     * Map의 Key는 해당 행에 value가 들어갈 컬럼명으로 사용한다.
     *
     * @param data 엑셀로 변한활 데이터
     * @param savePath 엑셀 파일을 저장할 경로
     */
    @Override
    public void export(List<Map<String, String>> data, Path savePath) {
        if (data.isEmpty()) {
            throw new NoSuchElementException("[" + getClass().getSimpleName() + "]" + " 입력된 데이터가 비어있어요.");
        }

        try (Workbook workbook = new XSSFWorkbook();
             OutputStream outputStream = Files.newOutputStream(savePath);) {
            Sheet sheet = workbook.createSheet("result");

            Set<String> columnNames = new LinkedHashSet<>();
            data.forEach(row -> columnNames.addAll(row.keySet()));

            CellStyle defaultStyle = workbook.createCellStyle();
            defaultStyle.setWrapText(false);
            defaultStyle.setVerticalAlignment(VerticalAlignment.TOP);

            CellStyle lineWrapStyle = workbook.createCellStyle();
            lineWrapStyle.setWrapText(true);
            lineWrapStyle.setVerticalAlignment(VerticalAlignment.TOP);

            // 1. 타이틀 행 (0번 행)
            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue(title);

            CellStyle titleStyle = sheet.getWorkbook().createCellStyle();
            titleStyle.setWrapText(true);
            titleStyle.setAlignment(HorizontalAlignment.CENTER);
            titleStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            titleStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            titleStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            Font titleFont = sheet.getWorkbook().createFont();
            titleFont.setFontHeightInPoints((short) 14);
            titleFont.setBold(true);
            titleStyle.setFont(titleFont);

            titleCell.setCellStyle(titleStyle);

            if (columnNames.size() > 1) {
                sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(
                        0, 0, 0, columnNames.size() - 1
                ));
            }

            // 2. 헤더 행 (1번 행)
            Row headerRow = sheet.createRow(1);
            int colIndex = 0;
            for (String colName : columnNames) {
                Cell cell = headerRow.createCell(colIndex++);
                cell.setCellValue(colName);
                cell.setCellStyle(defaultStyle);
            }

            // 3. 데이터 행 (2번 행부터)
            int rowIndex = 2;
            for (Map<String, String> rowData : data) {
                Row row = sheet.createRow(rowIndex++);
                colIndex = 0;
                for (String colName : columnNames) {
                    Cell cell = row.createCell(colIndex++);

                    String value = Optional.ofNullable(rowData.get(colName)).orElse("");
                    cell.setCellValue(value);
                    cell.setCellStyle(value.contains("\n") ? lineWrapStyle : defaultStyle);
                }
            }

            // 4. 컬럼 너비 자동 조절
            for (int i = 0; i < columnNames.size(); i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(outputStream);

            log.info("엑셀 파일을 저장했어요. 파일 경로: {}", savePath);
        } catch (IOException e) {
            throw new RuntimeException("[" + getClass().getSimpleName() + "]" + " 엑셀 파일을 저장하는데 실패했어요.", e);
        }
    }
}
