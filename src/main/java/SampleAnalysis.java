import analysis.Analysis;
import analysis.library.ImportAnalysis;
import export.XlsxResultWriter;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class SampleAnalysis {
    public static void main(String[] args) {
        /*
         * ImportAnalysis
         *  - 자바 루트 경로를 입력받아 해당 경로 하위에 있는 모든 자바파일의 import를 확인합니다.
         *  - 각 자바 파일에서 호출한 import를 분석해서 실제 사용중인 import, 호출했으나 사용하지 않는 import 정보를 제공합니다.
         */
        Path javaPath = Path.of("C:/xxx/xxx/src/main/java");
        Analysis analysis = new ImportAnalysis(javaPath);
        List<Map<String, String>> analysisResult = analysis.analyze();

        XlsxResultWriter writer = new XlsxResultWriter();
        writer.setTitle("Import Analysis Results");
        writer.exportToDefaultLocation(analysisResult);
    }
}