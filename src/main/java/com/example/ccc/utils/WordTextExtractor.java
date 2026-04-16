package com.example.ccc.utils;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Component
public class WordTextExtractor {

    public String extractTextFromDocx(byte[] docxBytes) {
        if (docxBytes == null || docxBytes.length == 0) {
            return "";
        }

        try (InputStream inputStream = new ByteArrayInputStream(docxBytes);
             XWPFDocument document = new XWPFDocument(inputStream)) {

            StringBuilder text = new StringBuilder();
            List<XWPFParagraph> paragraphs = document.getParagraphs();
            for (XWPFParagraph paragraph : paragraphs) {
                String paraText = paragraph.getText();
                if (paraText != null && !paraText.trim().isEmpty()) {
                    text.append(paraText).append("\n");
                }
            }

            String result = text.toString().trim();
            if (result.isEmpty()) {
                return "[Word文档内容为空]";
            }
            return result;

        } catch (IOException e) {
            e.printStackTrace();
            return "[Word文档解析失败: " + e.getMessage() + "]";
        } catch (Exception e) {
            return "[Word文档格式错误: " + e.getMessage() + "]";
        }
    }

    public String extractTextFromDoc(byte[] docBytes) {
        if (docBytes == null || docBytes.length == 0) {
            return "";
        }

        try (InputStream inputStream = new ByteArrayInputStream(docBytes);
             HWPFDocument document = new HWPFDocument(inputStream);
             WordExtractor extractor = new WordExtractor(document)) {

            String text = extractor.getText();
            if (text == null || text.trim().isEmpty()) {
                return "[Word文档内容为空]";
            }
            return text.trim();

        } catch (IOException e) {
            e.printStackTrace();
            return "[Word文档解析失败: " + e.getMessage() + "]";
        } catch (Exception e) {
            return "[Word文档格式错误: " + e.getMessage() + "]";
        }
    }

    public String extractText(byte[] fileBytes, String fileName) {
        if (fileBytes == null || fileBytes.length == 0) {
            return "";
        }

        if (fileName == null) {
            return new String(fileBytes);
        }

        String lowerName = fileName.toLowerCase();
        if (lowerName.endsWith(".docx")) {
            return extractTextFromDocx(fileBytes);
        } else if (lowerName.endsWith(".doc")) {
            return extractTextFromDoc(fileBytes);
        } else {
            return new String(fileBytes);
        }
    }

    public boolean isWordFile(String fileName) {
        if (fileName == null) return false;
        String lower = fileName.toLowerCase();
        return lower.endsWith(".doc") || lower.endsWith(".docx");
    }
}