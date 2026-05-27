package com.nxvibeon.backend.codechange.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

/**
 * 외부 diff 라이브러리 없이 사용할 수 있는 단순 unified diff 생성기입니다.
 * 초기 버전에서는 줄 단위 비교 결과를 사람이 확인하기 쉬운 형태로 제공합니다.
 */
@Service
public class UnifiedDiffService {

    public String createUnifiedDiff(String filePath, String originalCode, String proposedCode) {
        List<String> before = splitLines(originalCode);
        List<String> after = splitLines(proposedCode);

        StringBuilder diff = new StringBuilder();
        diff.append("--- ").append(filePath == null ? "before" : filePath).append('\n');
        diff.append("+++ ").append(filePath == null ? "after" : filePath).append('\n');

        int max = Math.max(before.size(), after.size());
        for (int i = 0; i < max; i++) {
            String left = i < before.size() ? before.get(i) : null;
            String right = i < after.size() ? after.get(i) : null;

            if (left == null) {
                diff.append('+').append(right).append('\n');
            } else if (right == null) {
                diff.append('-').append(left).append('\n');
            } else if (left.equals(right)) {
                diff.append(' ').append(left).append('\n');
            } else {
                diff.append('-').append(left).append('\n');
                diff.append('+').append(right).append('\n');
            }
        }

        return diff.toString();
    }

    private List<String> splitLines(String value) {
        String safe = value == null ? "" : value;
        String[] lines = safe.split("\\R", -1);
        List<String> result = new ArrayList<>();
        for (String line : lines) {
            result.add(line);
        }
        return result;
    }
}
