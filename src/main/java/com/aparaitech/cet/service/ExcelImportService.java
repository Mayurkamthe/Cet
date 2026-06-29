package com.aparaitech.cet.service;

import com.aparaitech.cet.entity.Role;
import com.aparaitech.cet.entity.User;
import com.aparaitech.cet.repository.RoleRepository;
import com.aparaitech.cet.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExcelImportService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    // ─── Column indexes in the Excel sheet ───
    private static final int COL_USERNAME  = 0;
    private static final int COL_FULLNAME  = 1;
    private static final int COL_EMAIL     = 2;
    private static final int COL_PHONE     = 3;
    private static final int COL_PASSWORD  = 4;

    // ─────────────────────────────────────────────────────────────────
    //  IMPORT
    // ─────────────────────────────────────────────────────────────────

    public static class ImportResult {
        public int success = 0;
        public int skipped = 0;
        public final List<String> errors = new ArrayList<>();

        public int total() { return success + skipped + errors.size(); }
        public boolean hasErrors() { return !errors.isEmpty(); }
    }

    @Transactional
    public ImportResult importStudents(MultipartFile file) throws IOException {
        ImportResult result = new ImportResult();

        Role studentRole = roleRepository.findByName("ROLE_STUDENT")
            .orElseThrow(() -> new RuntimeException("Student role not found"));

        try (InputStream is = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(is)) {

            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                result.errors.add("Excel file has no sheets.");
                return result;
            }

            // Skip header row (row 0)
            for (int rowIdx = 1; rowIdx <= sheet.getLastRowNum(); rowIdx++) {
                Row row = sheet.getRow(rowIdx);
                if (row == null || isRowEmpty(row)) continue;

                try {
                    String username = getCellString(row, COL_USERNAME);
                    String fullName = getCellString(row, COL_FULLNAME);
                    String email    = getCellString(row, COL_EMAIL);
                    String phone    = getCellString(row, COL_PHONE);
                    String password = getCellString(row, COL_PASSWORD);

                    // Validate required fields
                    if (username.isBlank()) {
                        result.errors.add("Row " + (rowIdx + 1) + ": Username is required.");
                        continue;
                    }
                    if (fullName.isBlank()) {
                        result.errors.add("Row " + (rowIdx + 1) + ": Full Name is required.");
                        continue;
                    }
                    if (email.isBlank()) {
                        result.errors.add("Row " + (rowIdx + 1) + ": Email is required.");
                        continue;
                    }
                    if (password.isBlank()) {
                        password = username + "@123"; // default password
                    }

                    // Check duplicates
                    if (userRepository.existsByUsername(username)) {
                        result.skipped++;
                        result.errors.add("Row " + (rowIdx + 1) + ": Username '" + username + "' already exists — skipped.");
                        continue;
                    }
                    if (userRepository.existsByEmail(email)) {
                        result.skipped++;
                        result.errors.add("Row " + (rowIdx + 1) + ": Email '" + email + "' already exists — skipped.");
                        continue;
                    }

                    User student = User.builder()
                        .username(username.toLowerCase().trim())
                        .password(passwordEncoder.encode(password))
                        .fullName(fullName.trim())
                        .email(email.toLowerCase().trim())
                        .phone(phone.isBlank() ? null : phone.trim())
                        .enabled(true)
                        .roles(Set.of(studentRole))
                        .build();

                    userRepository.save(student);
                    result.success++;
                    log.debug("Imported student: {}", username);

                } catch (Exception e) {
                    result.errors.add("Row " + (rowIdx + 1) + ": " + e.getMessage());
                }
            }
        }

        log.info("Excel import complete — success: {}, skipped: {}, errors: {}",
            result.success, result.skipped, result.errors.size() - result.skipped);
        return result;
    }

    // ─────────────────────────────────────────────────────────────────
    //  EXPORT — export all students to Excel
    // ─────────────────────────────────────────────────────────────────

    public byte[] exportStudents(List<User> students) throws IOException {
        try (Workbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = wb.createSheet("Students");

            // ── Styles ──
            CellStyle headerStyle = wb.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Font headerFont = wb.createFont();
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setBorderBottom(BorderStyle.THIN);

            CellStyle altStyle = wb.createCellStyle();
            altStyle.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
            altStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // ── Header row ──
            String[] headers = {"Username", "Full Name", "Email", "Phone", "Created At", "Status"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // ── Data rows ──
            int rowNum = 1;
            for (User s : students) {
                Row row = sheet.createRow(rowNum);
                if (rowNum % 2 == 0) {
                    for (int c = 0; c < headers.length; c++) {
                        row.createCell(c).setCellStyle(altStyle);
                    }
                }
                setCell(row, 0, s.getUsername());
                setCell(row, 1, s.getFullName());
                setCell(row, 2, s.getEmail() != null ? s.getEmail() : "");
                setCell(row, 3, s.getPhone() != null ? s.getPhone() : "");
                setCell(row, 4, s.getCreatedAt() != null ? s.getCreatedAt().toString().replace("T", " ").substring(0, 16) : "");
                setCell(row, 5, s.isEnabled() ? "Active" : "Disabled");
                rowNum++;
            }

            // ── Auto-size columns ──
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
                sheet.setColumnWidth(i, sheet.getColumnWidth(i) + 512); // padding
            }

            wb.write(out);
            return out.toByteArray();
        }
    }

    // ─────────────────────────────────────────────────────────────────
    //  TEMPLATE — blank import template with headers + sample row
    // ─────────────────────────────────────────────────────────────────

    public byte[] generateImportTemplate() throws IOException {
        try (Workbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = wb.createSheet("Students");

            // Header style
            CellStyle headerStyle = wb.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.ROYAL_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Font hFont = wb.createFont();
            hFont.setBold(true);
            hFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(hFont);
            headerStyle.setBorderBottom(BorderStyle.MEDIUM);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            // Required marker style
            CellStyle reqStyle = wb.createCellStyle();
            reqStyle.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
            reqStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Font reqFont = wb.createFont();
            reqFont.setItalic(true);
            reqFont.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
            reqStyle.setFont(reqFont);

            // Sample data style
            CellStyle sampleStyle = wb.createCellStyle();
            sampleStyle.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
            sampleStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // ── Headers ──
            String[] headers = {"username *", "fullName *", "email *", "phone", "password"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // ── Instruction row ──
            Row instrRow = sheet.createRow(1);
            String[] instructions = {
                "e.g. john01",
                "e.g. John Smith",
                "e.g. john@school.com",
                "e.g. 9876543210 (optional)",
                "If blank, defaults to username@123"
            };
            for (int i = 0; i < instructions.length; i++) {
                Cell cell = instrRow.createCell(i);
                cell.setCellValue(instructions[i]);
                cell.setCellStyle(reqStyle);
            }

            // ── Sample rows ──
            Object[][] samples = {
                {"student001", "Rahul Sharma",  "rahul@example.com",  "9876543210", "rahul@pass"},
                {"student002", "Priya Patil",   "priya@example.com",  "9123456789", ""},
                {"student003", "Amit Desai",    "amit@example.com",   "",           ""},
            };
            for (int r = 0; r < samples.length; r++) {
                Row row = sheet.createRow(r + 2);
                for (int c = 0; c < samples[r].length; c++) {
                    Cell cell = row.createCell(c);
                    cell.setCellValue(samples[r][c].toString());
                    cell.setCellStyle(sampleStyle);
                }
            }

            // ── Column widths ──
            int[] widths = {4000, 6000, 7000, 4500, 5500};
            for (int i = 0; i < widths.length; i++) {
                sheet.setColumnWidth(i, widths[i]);
            }

            // ── Freeze header ──
            sheet.createFreezePane(0, 1);

            wb.write(out);
            return out.toByteArray();
        }
    }

    // ─────────────────────────────────────────────────────────────────
    //  HELPERS
    // ─────────────────────────────────────────────────────────────────

    private String getCellString(Row row, int col) {
        Cell cell = row.getCell(col, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING  -> cell.getStringCellValue().trim();
            case NUMERIC -> {
                // Handle numeric cells (phone numbers stored as numbers)
                double val = cell.getNumericCellValue();
                // If it's a whole number, remove decimal point
                yield val == Math.floor(val) ? String.valueOf((long) val) : String.valueOf(val);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getCachedFormulaResultType() == CellType.STRING
                ? cell.getStringCellValue().trim()
                : String.valueOf((long) cell.getNumericCellValue());
            default -> "";
        };
    }

    private void setCell(Row row, int col, String value) {
        Cell cell = row.getCell(col);
        if (cell == null) cell = row.createCell(col);
        cell.setCellValue(value != null ? value : "");
    }

    private boolean isRowEmpty(Row row) {
        for (int c = row.getFirstCellNum(); c <= row.getLastCellNum(); c++) {
            Cell cell = row.getCell(c);
            if (cell != null && cell.getCellType() != CellType.BLANK
                && !getCellString(row, c).isBlank()) {
                return false;
            }
        }
        return true;
    }
}
