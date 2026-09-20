package com.cdc.agent.service.rag;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.segment.TextSegment;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 自定义递归文本切分器
 * 按照语义层级递归切分：段落 → 句子 → 单词 → 字符
 * 优先保持语义完整性，在 chunkSize 限制下尽可能保留完整的语义单元
 */
public class RecursiveTextSplitter implements DocumentSplitter {

    private final int chunkSize;
    private final int chunkOverlap;

    /**
     * 切分层级：段落(\n\n) → 行(\n) → 句子 → 单词
     */
    private static final Pattern[] SPLIT_PATTERNS = {
            Pattern.compile("\\n\\n+"),           // 段落（连续换行）
            Pattern.compile("\\n"),               // 行（单个换行）
            Pattern.compile("(?<=[.!?。！？])\\s+"), // 句子（中英文标点）
            Pattern.compile("\\s+")               // 单词/词组（空白字符）
    };

    public RecursiveTextSplitter(int chunkSize, int chunkOverlap) {
        this.chunkSize = chunkSize;
        this.chunkOverlap = Math.min(chunkOverlap, chunkSize / 2);
    }

    @Override
    public List<TextSegment> split(Document document) {
        String text = document.text();
        List<String> chunks = recursiveSplit(text, 0);
        List<TextSegment> segments = new ArrayList<>();
        for (String chunk : chunks) {
            String trimmed = chunk.trim();
            if (!trimmed.isEmpty()) {
                segments.add(TextSegment.from(trimmed));
            }
        }
        return applyOverlap(segments);
    }

    /**
     * 递归切分核心逻辑
     *
     * @param text  待切分文本
     * @param level 当前切分层级（对应 SPLIT_PATTERNS 索引）
     * @return 切分后的文本块列表
     */
    private List<String> recursiveSplit(String text, int level) {
        List<String> result = new ArrayList<>();

        // 如果文本已经在 chunkSize 范围内，直接返回
        if (text.length() <= chunkSize) {
            result.add(text);
            return result;
        }

        // 如果已经到最细粒度（单词级），按字符强制切分
        if (level >= SPLIT_PATTERNS.length) {
            return forceSplit(text);
        }

        // 按当前层级的分隔符切分
        String[] parts = SPLIT_PATTERNS[level].split(text);

        // 将切分后的片段合并到 chunkSize 大小
        List<String> merged = mergeParts(parts, level);
        result.addAll(merged);

        return result;
    }

    /**
     * 将切分后的片段按 chunkSize 合并，同时递归处理超大片段
     */
    private List<String> mergeParts(String[] parts, int level) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }

            // 获取分隔符（用于还原原始文本结构）
            String separator = getSeparator(level);

            // 如果加上当前片段后超出 chunkSize
            if (current.length() + separator.length() + part.length() > chunkSize) {
                // 如果当前累积不为空，先处理它
                if (current.length() > 0) {
                    String chunk = current.toString();
                    if (chunk.length() > chunkSize) {
                        // 当前块仍然太大，递归到下一层级继续切分
                        result.addAll(recursiveSplit(chunk, level + 1));
                    } else {
                        result.add(chunk);
                    }
                }

                // 处理单个 part 就超过 chunkSize 的情况
                if (part.length() > chunkSize) {
                    result.addAll(recursiveSplit(part, level + 1));
                    current = new StringBuilder();
                } else {
                    current = new StringBuilder(part);
                }
            } else {
                if (current.length() > 0) {
                    current.append(separator);
                }
                current.append(part);
            }
        }

        // 处理最后剩余的累积内容
        if (current.length() > 0) {
            String chunk = current.toString();
            if (chunk.length() > chunkSize) {
                result.addAll(recursiveSplit(chunk, level + 1));
            } else {
                result.add(chunk);
            }
        }

        return result;
    }

    /**
     * 强制按字符数切分（最后的兜底策略）
     */
    private List<String> forceSplit(String text) {
        List<String> result = new ArrayList<>();
        int step = chunkSize - chunkOverlap;
        if (step <= 0) {
            step = 1;
        }

        for (int i = 0; i < text.length(); i += step) {
            int end = Math.min(i + chunkSize, text.length());
            result.add(text.substring(i, end));
            if (end >= text.length()) {
                break;
            }
        }
        return result;
    }

    /**
     * 应用重叠逻辑：在相邻 chunk 之间添加 overlap 内容
     */
    private List<TextSegment> applyOverlap(List<TextSegment> segments) {
        if (chunkOverlap <= 0 || segments.size() <= 1) {
            return segments;
        }

        List<TextSegment> result = new ArrayList<>();
        result.add(segments.get(0));

        for (int i = 1; i < segments.size(); i++) {
            String prevText = segments.get(i - 1).text();
            String currText = segments.get(i).text();

            // 从前一个 chunk 的尾部取 overlap 内容
            String overlapText = prevText.length() > chunkOverlap
                    ? prevText.substring(prevText.length() - chunkOverlap)
                    : prevText;

            // 将 overlap 添加到当前 chunk 的头部
            String merged = overlapText + currText;
            // 如果合并后超过 chunkSize，截断到 chunkSize
            if (merged.length() > chunkSize) {
                merged = merged.substring(0, chunkSize);
            }
            result.add(TextSegment.from(merged));
        }

        return result;
    }

    /**
     * 获取当前层级的分隔符（用于合并时还原）
     */
    private String getSeparator(int level) {
        return switch (level) {
            case 0 -> "\n\n";
            case 1 -> "\n";
            case 2 -> " ";
            default -> "";
        };
    }
}
