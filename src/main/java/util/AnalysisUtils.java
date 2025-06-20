package util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class AnalysisUtils {
    /**
     * 입력받은 경로에 있는 특정 확장자의 모든 파일을 리스트에 담아서 반환한다.
     * - 파일 경로를 입력받은 경우 : 해당 파일 경로를 리스트로 반환
     * - 디렉토리 경로를 입력받은 경우 : 해당 디렉토리 기준 모든 하위 폴더를 탐색하여 특정 확장자 파일을 리스트로 반환
     *
     * @param searchPath directory 경로 또는 파일 경로
     * @param allowedExt 목록에 넣을 확장자 리스트
     * @throws IllegalArgumentException 경로가 존재하지 않거나 디렉터리가 아닌 경우
     */
    public static List<Path> extractFileList(Path searchPath, List<String> allowedExt) {
        String callClassName = Thread.currentThread().getStackTrace()[2].getClassName().replaceAll("^.*\\.", "");
        List<Path> resutlPathMap = null;

        if (Files.notExists(searchPath)) {
            throw new IllegalArgumentException("[" + callClassName + "]" + " 해당 경로(파일)를 찾을 수 없어요. 경로(파일): " + searchPath + "]");
        }

        try {
            resutlPathMap = Files.walk(searchPath)
                    .filter(Files::isRegularFile)
                    .filter(path -> {
                        String fileName = path.getFileName().toString().toLowerCase();
                        return allowedExt.stream().anyMatch(ext -> fileName.endsWith("." + ext.toLowerCase()));
                    })
                    .toList();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return resutlPathMap;
    }
}
