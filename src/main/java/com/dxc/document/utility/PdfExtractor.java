package com.dxc.document.utility;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PdfExtractor {


    private static final Logger logger = LoggerFactory.getLogger(PdfExtractor.class);

    public static Map<String, String> extractFieldsFromPdf(File pdfFile) throws IOException {
        logger.info("📄 Extracting fields from PDF: {}", pdfFile.getName());
        Map<String, String> data = new LinkedHashMap<>();

        try (PDDocument document = PDDocument.load(pdfFile)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String fullText = stripper.getText(document);

            // ✅ Clean junk lines (watermarks, stamps, etc.)
            fullText = fullText.replaceAll("(?m)^\\s*\\d{4,}[a-z0-9\\\\]+.*$", "");
            fullText = fullText.replaceAll("(?im)^.*Approved On:.*$", "");
            fullText = fullText.replaceAll("(?im)^.*Pfizer Confidential.*$", "");
            fullText = fullText.replaceAll("(?im)^.*JOB DESCRIPTION.*$", "");
            fullText = fullText.replaceAll("(?im)^.*Page \\d+ of \\d+.*$", "");
            fullText = fullText.replaceAll("(?m)^\\s*$", "");

            logger.debug("📘 Cleaned text:");
            logger.debug(fullText);

            // ✅ Extract key-value fields
            data.put("JOB TITLE", extractLineValue(fullText, "JOB TITLE:"));
            data.put("REPORTS TO", extractLineValue(fullText, "REPORTS TO:"));
            data.put("DIVISION", extractLineValue(fullText, "DIVISION/BUSINESS LINE:"));
            data.put("VERSION DATE", extractLineValue(fullText, "VERSION DATE:"));
            data.put("SUB DIVISION", extractLineValue(fullText, "SUB DIVISION:"));
            data.put("DEPARTMENT", extractLineValue(fullText, "DEPARTMENT NAME:"));
            data.put("LOCATION", extractLineValue(fullText, "LOCATION(S):"));

            // ✅ Extract sections with subheadings
            data.put("JOB SUMMARY", extractBulletsOrPlainSection(fullText, "JOB SUMMARY", "JOB RESPONSIBILITIES"));
            data.put("RESPONSIBILITIES", extractBulletsOrPlainSection(fullText, "JOB RESPONSIBILITIES", "QUALIFICATIONS / SKILLS"));
            data.put("QUALIFICATIONS", extractBulletsOrPlainSection(fullText, "QUALIFICATIONS / SKILLS", "ORGANIZATIONAL RELATIONSHIPS"));
//            data.put("NON-STANDARD WORK", extractBulletsOrPlainSection(fullText,
//                    "NON-STANDARD WORK SCHEDULE, TRAVEL OR ENVIRONMENT REQUIREMENTS", "ORGANIZATIONAL RELATIONSHIPS"));

            data.put("NON-STANDARD WORK", extractBulletsOrPlainSection(fullText,
                    "NON-STANDARD WORK SCHEDULE, TRAVEL OR ENVIRONMENT REQUIREMENTS",
                    "ORGANIZATIONAL RELATIONSHIPS"));


            data.put("ORGANIZATIONAL RELATIONSHIPS", extractBulletsOrPlainSection(fullText, "ORGANIZATIONAL RELATIONSHIPS", "RESOURCES MANAGED"));

            // ✅ Extract additional RESOURCES MANAGED section
            data.put("RESOURCES MANAGED", extractBulletsOrPlainSection(fullText, "RESOURCES MANAGED", ""));

            logger.info("✅ PDF extraction complete: {}", pdfFile.getName());
        } catch (Exception e) {
            logger.error("❌ Failed to extract from {}: {}", pdfFile.getName(), e.getMessage(), e);
            throw e;
        }

        return data;
    }

    private static String extractLineValue(String text, String label) {
        Pattern pattern = Pattern.compile("(?i)" + Pattern.quote(label) + "\\s*(.+)");
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            String value = matcher.group(1).trim();
            value = value.replaceFirst("^\\([^\\)]+\\)\\s*", "");
            logger.debug("✔ Extracted line [{}]: {}", label, value);
            return value;
        }
        logger.warn("⚠️ Label not found: {}", label);
        return "";
    }


    private static String extractBulletsWithHeader(String text, String startHeader, String endHeader) {
        String pattern;
        if (endHeader == null || endHeader.isEmpty()) {
            pattern = "(?i)" + Pattern.quote(startHeader) + "\\s*\\n?(.+)$";
        } else {
            pattern = "(?i)" + Pattern.quote(startHeader) + "\\s*\\n?(.+?)\\s*(?=" + Pattern.quote(endHeader) + ")";
        }

        Matcher matcher = Pattern.compile(pattern, Pattern.DOTALL).matcher(text);
        StringBuilder result = new StringBuilder();

        if (matcher.find()) {
            String raw = matcher.group(1).trim();

            String[] lines = raw.split("\\r?\\n");

           // result.append(startHeader.toUpperCase()).append("\n");

            StringBuilder currentBullet = new StringBuilder();

            for (String line : lines) {
                String clean = line.trim();

                // Skip known junk
                if (clean.matches("(?i)^.*(Pfizer Confidential|Approved On|JOB DESCRIPTION|Page \\d+ of \\d+|^\\d{4,}\\\\.*)$") || clean.length() < 2) {
                    continue;
                }

                // Check if the line is a subheading (like "Implementation Leadership:")
                if (clean.matches("^[A-Z][A-Za-z\\s&]+:$")) {
                    // Flush previous bullet if present
                    if (currentBullet.length() > 0) {
                        result.append("• ").append(currentBullet.toString().trim()).append("\n");
                        currentBullet.setLength(0);
                    }

                    // Append subheading in uppercase
                    result.append(clean.toUpperCase()).append("\n");
                    continue;
                }

                // If it's a bullet point
                if (clean.startsWith("•") || clean.startsWith("")) {
                    if (currentBullet.length() > 0) {
                        result.append("• ").append(currentBullet.toString().trim()).append("\n");
                        currentBullet.setLength(0);
                    }
                    clean = clean.replaceFirst("^[•\\s]+", "");
                    currentBullet.append(clean).append(" ");
                } else if (currentBullet.length() > 0) {
                    currentBullet.append(clean).append(" ");
                }
            }

            if (currentBullet.length() > 0) {
                result.append("• ").append(currentBullet.toString().trim());
            }
        } else {
            logger.warn("⚠️ Section not found: {} to {}", startHeader, endHeader);
        }

        return result.toString().trim();
    }

//    private static String extractBulletsOrPlainSection(String text, String startHeader, String endHeader) {
//        String pattern;
//        if (endHeader == null || endHeader.isEmpty()) {
//            pattern = "(?i)" + Pattern.quote(startHeader) + "\\s*\\n?(.+)$";
//        } else {
//            pattern = "(?i)" + Pattern.quote(startHeader) + "\\s*\\n?(.+?)\\s*(?=" + Pattern.quote(endHeader) + ")";
//        }
//
//        Matcher matcher = Pattern.compile(pattern, Pattern.DOTALL).matcher(text);
//        StringBuilder result = new StringBuilder();
//
//        if (matcher.find()) {
//            String raw = matcher.group(1).trim();
//            String[] lines = raw.split("\\r?\\n");
//
//           // result.append(startHeader.toUpperCase()).append("\n");
//
//            StringBuilder currentBullet = new StringBuilder();
//            boolean bulletFound = false;
//
//            for (String line : lines) {
//                String clean = line.trim();
//
//                // ❌ Skip known unwanted or instructional lines
//                if (clean.matches("(?i).*\\b(summary of|provide the primary groups|include any external|key role\\(s\\)).*")) {
//                    logger.debug("🟡 Skipped instructional line: {}", clean);
//                    continue;
//                }
//
//                // ❌ Skip footers and watermarks
//                if (clean.matches("(?i)^.*(approved|GMT|Pfizer Confidential|JOB DESCRIPTION|Page \\d+ of \\d+).*") || clean.length() < 3) {
//                    continue;
//                }
//
//                // ✅ Detect bullet start
//                if (clean.startsWith("•") || clean.startsWith("") || clean.matches("^[\\-–].+")) {
//                    bulletFound = true;
//                    if (currentBullet.length() > 0) {
//                        result.append("• ").append(currentBullet.toString().trim()).append("\n");
//                        currentBullet.setLength(0);
//                    }
//                    clean = clean.replaceFirst("^[•\\-–\\s]+", "");
//                    currentBullet.append(clean).append(" ");
//                }
//                // ✅ Continuation of bullet
//                else if (bulletFound && currentBullet.length() > 0) {
//                    currentBullet.append(clean).append(" ");
//                }
//                // ✅ Plain sentence
//                else {
//                    result.append(clean).append("\n");
//                }
//            }
//
//            // ✅ Final bullet
//            if (currentBullet.length() > 0) {
//                result.append("• ").append(currentBullet.toString().trim());
//            }
//        } else {
//            logger.warn("⚠️ Section not found: {} to {}", startHeader, endHeader);
//        }
//
//        return result.toString().trim();
//    }


//    private static String extractBulletsOrPlainSection(String text, String startHeader, String endHeader) {
//        String pattern;
//        if (endHeader == null || endHeader.isEmpty()) {
//            pattern = "(?i)" + Pattern.quote(startHeader) + "\\s*\\n?(.+)$";
//        } else {
//            pattern = "(?i)" + Pattern.quote(startHeader) + "\\s*\\n?(.+?)\\s*(?=" + Pattern.quote(endHeader) + ")";
//        }
//
//        Matcher matcher = Pattern.compile(pattern, Pattern.DOTALL).matcher(text);
//        StringBuilder result = new StringBuilder();
//
//        if (matcher.find()) {
//            String raw = matcher.group(1).trim();
//            String[] lines = raw.split("\\r?\\n");
//
//            StringBuilder currentBullet = new StringBuilder();
//            boolean bulletFound = false;
//            boolean subBulletDetected = false;
//
//            for (String line : lines) {
//                String clean = line.trim();
//
//                // 🔴 Skip junk, watermark, instruction lines
//                if (clean.matches("(?i).*\\b(summary of|provide the primary groups|include any external|Indicatethe primary|Summarize the primary purpose & key accountabilities of the job|ndicate qualifications and skills that are necessary for performance of responsibilities including: education, relevant experience, \n" +
//                        "licenses, certificationsand other job-related technical and managerial skills.|key role\\(s\\)).*")) {
//                    logger.info("🟡 Skipped instructional line: {}", clean);
//                    continue;
//                }
//
//                if (clean.matches("(?i)^.*(approved|GMT|Pfizer Confidential|JOB DESCRIPTION|Page \\d+ of \\d+).*") || clean.length() < 3) {
//                    continue;
//                }
//
//                // 🔹 Detect main bullet
//                if (clean.startsWith("•") || clean.startsWith("") || clean.matches("^[\\-–].+")) {
//                    bulletFound = true;
//                    subBulletDetected = false;
//                    if (currentBullet.length() > 0) {
//                        result.append("• ").append(currentBullet.toString().trim()).append("\n");
//                        currentBullet.setLength(0);
//                    }
//                    clean = clean.replaceFirst("^[•\\-–\\s]+", "");
//                    currentBullet.append(clean).append(" ");
//                }
//
//                // 🔸 Detect sub-bullets (e.g., o, ▪, →)
//                else if (clean.matches("^[o▪→]\\s+.*")) {
//                    if (currentBullet.length() > 0) {
//                        result.append("> ").append(currentBullet.toString().trim()).append("\n");
//                        currentBullet.setLength(0);
//                    }
//                    clean = clean.replaceFirst("^[o▪→\\s]+", "");
//                    result.append("   * ").append(clean.trim()).append("\n");
//                    subBulletDetected = true;
//                }
//
//                // 🧩 Continuation of bullet
//                else if (bulletFound && !subBulletDetected && currentBullet.length() > 0) {
//                    currentBullet.append(clean).append(" ");
//                }
//
//                // 🧾 Plain sentence (no bullets at all)
//                else if (!bulletFound && clean.length() > 0) {
//                    result.append(clean).append("\n");
//                }
//            }
//
//            // Flush remaining bullet
//            if (currentBullet.length() > 0) {
//                result.append("• ").append(currentBullet.toString().trim());
//            }
//
//        } else {
//            logger.warn("⚠️ Section not found: {} to {}", startHeader, endHeader);
//        }
//
//        return result.toString().trim();
//    }

    private static String extractBulletsOrPlainSection(String text, String startHeader, String endHeader) {
        String pattern;



        if (endHeader == null || endHeader.isEmpty()) {
            pattern = "(?i)" + Pattern.quote(startHeader) + "\\s*\\n?(.+)$";
        } else {
            pattern = "(?i)" + Pattern.quote(startHeader) + "\\s*\\n?(.+?)\\s*(?=" + Pattern.quote(endHeader) + ")";
        }

        Matcher matcher = Pattern.compile(pattern, Pattern.DOTALL).matcher(text);
        StringBuilder result = new StringBuilder();

        if (matcher.find()) {
            String raw = matcher.group(1).trim();
            String[] lines = raw.split("\\r?\\n");

            StringBuilder currentBullet = new StringBuilder();
            boolean bulletFound = false;
            boolean subBulletDetected = false;

            List<Pattern> instructionPatterns = Arrays.asList(
                    // 🟨 JOB SUMMARY
                    Pattern.compile("(?i)^\\s*summarize the primary purpose.*"),

                    // 🟧 JOB RESPONSIBILITIES
                    Pattern.compile("(?i)^\\s*indicate the primary responsibilities.*"),

                    // 🟩 QUALIFICATIONS / SKILLS
                    Pattern.compile("(?i)^\\s*indicate qualifications and skills.*"),
                    Pattern.compile("(?i)^\\s*licenses, certifications.*"),

                    // 🟦 ORGANIZATIONAL RELATIONSHIPS
                    Pattern.compile("(?i)^\\s*provide the primary groups.*"),
                    Pattern.compile("(?i)^\\s*include any external interactions.*"),

                    // 🟥 RESOURCES MANAGED
                    Pattern.compile("(?i)^\\s*summary of resources managed.*"),

                    // ✅ Generic fallback (optional)
                    Pattern.compile("(?i)^\\s*this section describes.*"),
                    Pattern.compile("(?i)^\\s*describe required knowledge.*"),
                    Pattern.compile("(?i)^\\s*provide a brief overview.*"),

                    Pattern.compile("(?i)^\\s*\\(not all roles will have non-standard work schedule.*"),
                    Pattern.compile("(?i)^\\s*include any work schedule, travel.*"),
                    Pattern.compile("(?i)^\\s*types of requirements.*"),
                    Pattern.compile("(?i)^\\s*any criteria indicated must be job-related.*")

            );

            for (String line : lines) {
                String clean = line.trim();


                // 🚫 Universal unwanted lines
                if (clean.matches("(?i)^.*(approved|GMT|Pfizer Confidential|JOB DESCRIPTION|Page \\d+ of \\d+).*") || clean.length() < 3) {
                    continue;
                }

//                // 🚫 Instructional phrases (section-specific patterns)
//                if (clean.matches("(?i)^\\s*(summary of|provide the primary groups|include any external|key role\\(s\\)|this role is responsible for|n/a).*")) {
//                    logger.debug("🟡 Skipping instruction: {}", clean);
//                    continue;
//                }

                String finalClean = clean;
                boolean isInstruction = instructionPatterns.stream().anyMatch(p -> p.matcher(finalClean).find());
                if (isInstruction) {
                    logger.info("🟡 Skipping instruction: {}", clean);
                    continue;
                }


                // 🔹 Subheading inside bullet (e.g., "Implementation Leadership:")
                if (clean.matches("^[A-Z].*:\\s*$")) {
                    // flush current bullet if any
                    if (currentBullet.length() > 0) {
                        result.append("• ").append(currentBullet.toString().trim()).append("\n");
                        currentBullet.setLength(0);
                    }
                    result.append("\n🔸 ").append(clean.replace(":", "").trim().toUpperCase()).append("\n");
                    bulletFound = false;
                    continue;
                }

                // 🔹 Detect main bullet
                if (clean.startsWith("•") || clean.startsWith("") || clean.matches("^[\\-–].+")) {
                    bulletFound = true;
                    subBulletDetected = false;
                    if (currentBullet.length() > 0) {
                        result.append("• ").append(currentBullet.toString().trim()).append("\n");
                        currentBullet.setLength(0);
                    }
                    clean = clean.replaceFirst("^[•\\-–\\s]+", "");
                    currentBullet.append(clean).append(" ");
                }

                // 🔸 Detect sub-bullet (e.g., o, ▪)
                else if (clean.matches("^[o▪→]\\s+.*")) {
                    if (currentBullet.length() > 0) {
                        result.append("• ").append(currentBullet.toString().trim()).append("\n");
                        currentBullet.setLength(0);
                    }
                    clean = clean.replaceFirst("^[o▪→\\s]+", "");
                    result.append("   ◦ ").append(clean.trim()).append("\n");
                    subBulletDetected = true;
                }

                // 🔄 Continuation of bullet
                else if (bulletFound && !subBulletDetected && currentBullet.length() > 0) {
                    currentBullet.append(clean).append(" ");
                }

                // 📄 Plain sentence
                else if (!bulletFound && clean.length() > 0) {
                    result.append(clean).append("\n");
                }
            }

            // Flush final bullet
            if (currentBullet.length() > 0) {
                result.append("• ").append(currentBullet.toString().trim());
            }

        } else {
            logger.warn("⚠️ Section not found: {} to {}", startHeader, endHeader);
        }

        return result.toString().trim();
    }


}
