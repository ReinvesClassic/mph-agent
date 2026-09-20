package com.cdc.agent.service.rag;

import dev.langchain4j.data.document.BlankDocumentException;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

/**
 * 基于 Apache Tika 的万能文档解析器
 * 适配 LangChain4j DocumentParser 接口，支持 PDF / Word / Excel / PPT / HTML / TXT 等格式
 */
public class TikaDocumentParser implements DocumentParser {

    private static final Logger log = LoggerFactory.getLogger(TikaDocumentParser.class);

    private final Tika tika;

    public TikaDocumentParser() {
        this.tika = new Tika();
    }

    @Override
    public Document parse(InputStream inputStream) {
        try {
            String text = tika.parseToString(inputStream, new Metadata());
            if (text == null || text.isBlank()) {
                throw new BlankDocumentException();
            }
            return Document.from(text.trim());
        } catch (TikaException | IOException e) {
            log.error("[Tika] 文档解析失败: {}", e.getMessage(), e);
            throw new RuntimeException("文档解析失败: " + e.getMessage(), e);
        }
    }
}
