package com.example.ccc.utils;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Component
public class PdfTextExtractor {

    public String extractTextFromPdf(byte[] pdfBytes) {
        if (pdfBytes == null || pdfBytes.length == 0) {
            return "";
        }

        try (PDDocument document = PDDocument.load(new ByteArrayInputStream(pdfBytes))) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            return stripper.getText(document);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("PDF文本提取失败: " + e.getMessage());
        }
    }

    public String extractTextFromPdf(InputStream inputStream) {
        try (PDDocument document = PDDocument.load(inputStream)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            return stripper.getText(document);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("PDF文本提取失败: " + e.getMessage());
        }
    }

    public String extractTextFromTextFile(byte[] fileBytes) {
        if (fileBytes == null || fileBytes.length == 0) {
            return "";
        }
        return new String(fileBytes, StandardCharsets.UTF_8);
    }
}