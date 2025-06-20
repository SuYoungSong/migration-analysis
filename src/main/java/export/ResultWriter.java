package export;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

public interface ResultWriter {

    void export(List<Map<String, String>> data, Path savePath);

    /**
     * exportToDefaultLocation에서 파일명을 완성하기 위해 확장자명을 반환한다.
     *
     * @return 파일의 확장자
     */
    String getFileExtension();

    /**
     * 기본폴더("analysis-result")에 결과 파일을 생성한다.
     * 결과파일의 이름은 클래스명_yyyyMMdd_HHmmss로 저장한다.
     *
     * @throws RuntimeException 기본 폴더 생성 실패 시 오류가 발생합니다.
     */
    default void exportToDefaultLocation(List<Map<String, String>> data) {
        Path resultDir = Path.of("analysis-results");
        try {
            Files.createDirectories(resultDir);
        } catch (IOException e) {
            throw new RuntimeException("[" + getClass().getSimpleName() + "]" + " 기본 폴더를 만드는데 실패했어요. 폴더명: " + resultDir, e);
        }

        String fileName = String.format("%s_%s.%s",
                getClass().getSimpleName(),
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")),
                getFileExtension());
        Path savePath = resultDir.resolve(fileName);
        export(data, savePath);
    }
}
