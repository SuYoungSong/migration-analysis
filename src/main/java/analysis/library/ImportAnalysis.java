package analysis.library;

import analysis.Analysis;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import util.AnalysisUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@Slf4j
@RequiredArgsConstructor
public class ImportAnalysis implements Analysis {

    private final Path sourceRootPath;

    /**
     * 자바 파일 내 import 구문을 분석한다.
     *
     * @return import 분석 결과를 담은 List
     */
    @Override
    public List<Map<String, String>> analyze() {

        List<Path> pathList = AnalysisUtils.extractFileList(sourceRootPath, List.of("java"));
        List<Map<String, String>> fileInfoList = pathList.stream()
                .map(this::extractFileInfo)
                .filter(Objects::nonNull)
                .toList();

        /*
         * 여러 하위 폴더에 있는 모든 파일을 대상으로 검색하기에 depth 개수 맞추고 반환
         */
        Set<String> allKeys = fileInfoList.stream()
                .max(Comparator.comparingInt(Map::size))
                .map(Map::keySet)
                .orElseGet(LinkedHashSet::new);
        List<Map<String, String>> result = fileInfoList.stream()
                .map(fileInfoMap -> {
                    Map<String, String> analysisMap = new LinkedHashMap<>();
                    allKeys.forEach(key -> analysisMap.put(key, fileInfoMap.get(key)));
                    return analysisMap;
                })
                .toList();

        log.info("분석을 모두 완료했어요.");
        return result;
    }


    /**
     * 입력된 자바 파일의 경로 정보와 import 정보를 Map에 담아 반환한다.
     *
     * @param path 자바 파일의 경로
     * @return 자바 파일의 import 분석 결과를 담은 Map
     */
    private Map<String, String> extractFileInfo(Path path)  {
        Map<String, String> extractMap = new LinkedHashMap<>();
        try {
            extractMap.putAll(getJavaFileDepths(path));
            extractMap.putAll(getJavaFileImports(path));
        } catch (IOException e) {
            log.info("파일 분석에 실패했어요. 파일 경로: {}", path, e);
            return null;
        }

        return extractMap;
    }


    /**
     * 입력된 자바 파일을 경로 정보를 맵으로 반환한다.
     *
     * @param path 자바 파일의 경로
     * @return depth로 구분한 경로 구조 Map
     *          ex) path: aa/bb/cc.txt
     *          ┝ "depth 1" : aa
     *          ┝ "depth 2" : bb
     *          └ "depth 3" : cc.txt
     */
    private Map<String, String> getJavaFileDepths(Path path) {
        Path relativePath = sourceRootPath.getParent().relativize(path);
        return IntStream.range(0, relativePath.getNameCount())
                .boxed()
                .collect(Collectors.toMap(
                        i -> "depth " + (i + 1),
                        i -> relativePath.getName(i).toString(),
                        (a,b) -> b,
                        LinkedHashMap::new
                ));
    }


    /**
     * 입력된 자바 파일의 import문을 추출하고 import 후 사용 여부를 판단하여 맵으로 반환한다.
     *
     * @param file 자바 파일의 경로
     * @return import에 관련된 정보를 담은 used, unused, all 키를 갖고있는 Map
     *         ┣ "used" : 실제 코드에서 참조된 import 목록
     *         ┣ "unused" : import로 선언되었지만 소스 내 사용되지 않은 import 목록
     *         ┗ "all" : 코드에 선언된 모든 import 목록
     * @throws IOException 파일 읽는 중 I/O 오류 발생한 경우
     */
    public Map<String, String> getJavaFileImports(Path file) throws IOException {
        CompilationUnit cu = StaticJavaParser.parse(file);
        List<ImportDeclaration> imports = cu.getImports();

        /*
         *  import 사용 여부를 판단하기 위한 추출과정
         */
        // 1. 변수(매개변수 포함)명 추출
        Set<String> usedSimpleNames = cu.findAll(NameExpr.class).stream()
                .map(NameExpr::getNameAsString)
                .collect(Collectors.toSet());

        // 2. 클래스나 인터페이스 이름 추출 (ex: List, ArrayList)
        Set<String> usedTypeNames = cu.findAll(ClassOrInterfaceType.class).stream()
                .map(t -> t.getName().getIdentifier())
                .collect(Collectors.toSet());

        // 3. Annotation 이름 추출 (ex: Override, Deprecated)
        Set<String> usedAnnotationNames = cu.findAll(AnnotationExpr.class).stream()
                .map(a -> a.getName().getIdentifier())
                .collect(Collectors.toSet());

        // 4. static import 사용 판단을 위해 메서드나 필드 이름 추출
        Set<String> usedStaticNames = new HashSet<>();
        usedStaticNames.addAll(cu.findAll(MethodCallExpr.class).stream()
                .map(MethodCallExpr::getNameAsString)
                .collect(Collectors.toSet()));
        usedStaticNames.addAll(cu.findAll(FieldAccessExpr.class).stream()
                .map(FieldAccessExpr::getNameAsString)
                .collect(Collectors.toSet()));

        /*
         * import 호출 후 사용, 미사용으로 분리
         */
        Map<Boolean, List<String>> partitionedImports = imports.stream()
                .collect(Collectors.partitioningBy(
                        imp -> {
                            String importSimpleName = imp.getName().getIdentifier();
                            return imp.isStatic()
                                    ? usedStaticNames.contains(importSimpleName)
                                    : usedSimpleNames.contains(importSimpleName)
                                    || usedTypeNames.contains(importSimpleName)
                                    || usedAnnotationNames.contains(importSimpleName);
                        },
                        Collectors.mapping(ImportDeclaration::getNameAsString, Collectors.toList())
                ));

        List<String> usedImports = partitionedImports.get(true);
        List<String> unusedImports = partitionedImports.get(false);
        Set<String> allImports =  Stream.concat(usedImports.stream(), unusedImports.stream())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Map<String, String> importMap = new LinkedHashMap<>();
        importMap.put("used", String.join("\n", usedImports));
        importMap.put("unused", String.join("\n",unusedImports));
        importMap.put("all", String.join("\n",allImports));

        return importMap;
    }
}


