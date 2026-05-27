package com.nxvibeon.backend.javaanalysis.service;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.nxvibeon.backend.javaanalysis.dto.JavaAnalysisResponse;
import com.nxvibeon.backend.javaanalysis.dto.JavaTypeSummary;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Service
public class JavaProjectAnalysisService {
    public JavaAnalysisResponse analyze(Path projectPath, int maxFiles) {
        List<JavaTypeSummary> summaries = new ArrayList<>();
        int parsed = 0;
        int failed = 0;

        try (Stream<Path> stream = Files.walk(projectPath)) {
            List<Path> javaFiles = stream
                .filter(path -> path.toString().endsWith(".java"))
                .limit(maxFiles)
                .toList();

            for (Path file : javaFiles) {
                try {
                    CompilationUnit unit = StaticJavaParser.parse(file);
                    String packageName = unit.getPackageDeclaration()
                        .map(pd -> pd.getName().asString())
                        .orElse("");

                    for (ClassOrInterfaceDeclaration declaration : unit.findAll(ClassOrInterfaceDeclaration.class)) {
                        summaries.add(new JavaTypeSummary(
                            packageName,
                            declaration.getNameAsString(),
                            declaration.isInterface() ? "interface" : "class",
                            declaration.getMethods().size(),
                            declaration.getFields().size(),
                            file.toString()
                        ));
                    }

                    for (EnumDeclaration declaration : unit.findAll(EnumDeclaration.class)) {
                        summaries.add(new JavaTypeSummary(
                            packageName,
                            declaration.getNameAsString(),
                            "enum",
                            declaration.getMethods().size(),
                            declaration.getFields().size(),
                            file.toString()
                        ));
                    }
                    parsed++;
                } catch (Exception ex) {
                    failed++;
                }
            }
        } catch (Exception ex) {
            throw new IllegalArgumentException("Cannot analyze project path: " + projectPath, ex);
        }

        return new JavaAnalysisResponse(projectPath.toString(), parsed, failed, summaries);
    }
}
