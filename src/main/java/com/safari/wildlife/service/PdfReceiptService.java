package com.safari.wildlife.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class PdfReceiptService {

    public byte[] generateReceipt(String customerName, String bookingRef, String packageName, double amount) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 50, 50, 50, 50);

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, new Color(30, 58, 47));
            Font subTitleFont = FontFactory.getFont(FontFactory.HELVETICA, 11, new Color(100, 100, 100));
            Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.BLACK);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 12, Color.BLACK);
            Font accentFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, new Color(217, 107, 39));

            // Header Section: 3 Columns (Logo | Title | Badge)
            PdfPTable headerTable = new PdfPTable(3);
            headerTable.setWidthPercentage(100);
            headerTable.setWidths(new float[]{1f, 3f, 1.2f});

            // Column 1: Logo
            PdfPCell logoCell = new PdfPCell();
            logoCell.setBorder(Rectangle.NO_BORDER);
            logoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            try {
                // Use Spring's native resource loader pointing to the correct 'images' folder
                org.springframework.core.io.ClassPathResource imgFile = new org.springframework.core.io.ClassPathResource("static/images/Designer.png");

                if (imgFile.exists()) {
                    Image logo = Image.getInstance(imgFile.getContentAsByteArray());
                    logo.scaleToFit(75, 75); // Scales the logo neatly
                    logoCell.addElement(logo);
                } else {
                    System.err.println("Warning: Designer.png not found in src/main/resources/static/images/");
                    logoCell.addElement(new Paragraph(" "));
                }
            } catch (Exception e) {
                System.err.println("Error loading PDF logo: " + e.getMessage());
                logoCell.addElement(new Paragraph(" "));
            }
            headerTable.addCell(logoCell);

            // Column 2: Title
            PdfPCell titleCell = new PdfPCell();
            titleCell.setBorder(Rectangle.NO_BORDER);
            titleCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            titleCell.addElement(new Paragraph("WILD-LIFE SAFARI", titleFont));
            titleCell.addElement(new Paragraph("SRI LANKA", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, new Color(30, 58, 47))));
            titleCell.addElement(new Paragraph("Official Advance Payment & Deposit Voucher", subTitleFont));
            headerTable.addCell(titleCell);

            // Column 3: Badge
            PdfPCell badgeCell = new PdfPCell(new Phrase("PAID & CONFIRMED", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, new Color(0, 105, 92))));
            badgeCell.setBorder(Rectangle.NO_BORDER);
            badgeCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            badgeCell.setVerticalAlignment(Element.ALIGN_TOP);
            headerTable.addCell(badgeCell);

            document.add(headerTable);
            document.add(new Paragraph("\n"));

            // Separator Line
            document.add(new Paragraph(new Chunk(new com.lowagie.text.pdf.draw.LineSeparator(1f, 100f, Color.LIGHT_GRAY, Element.ALIGN_CENTER, -1f))));
            document.add(new Paragraph("\n"));

            // Metadata Section
            PdfPTable metaTable = new PdfPTable(2);
            metaTable.setWidthPercentage(100);
            metaTable.setSpacingAfter(20f);

            PdfPCell cell1 = new PdfPCell();
            cell1.setBorder(Rectangle.NO_BORDER);
            cell1.addElement(new Paragraph("Customer Name:", subTitleFont));
            cell1.addElement(new Paragraph(customerName, boldFont));
            cell1.addElement(new Paragraph(" "));
            cell1.addElement(new Paragraph("Gateway Provider:", subTitleFont));
            cell1.addElement(new Paragraph("BANCSTAC / Sampath Bank", accentFont));
            metaTable.addCell(cell1);

            PdfPCell cell2 = new PdfPCell();
            cell2.setBorder(Rectangle.NO_BORDER);
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            Paragraph pRefLabel = new Paragraph("Booking Reference:", subTitleFont);
            pRefLabel.setAlignment(Element.ALIGN_RIGHT);
            cell2.addElement(pRefLabel);

            Paragraph pRef = new Paragraph(bookingRef, boldFont);
            pRef.setAlignment(Element.ALIGN_RIGHT);
            cell2.addElement(pRef);

            cell2.addElement(new Paragraph(" "));

            Paragraph pDateLabel = new Paragraph("Date & Time Issued:", subTitleFont);
            pDateLabel.setAlignment(Element.ALIGN_RIGHT);
            cell2.addElement(pDateLabel);

            Paragraph pDate = new Paragraph(dtf.format(LocalDateTime.now()), normalFont);
            pDate.setAlignment(Element.ALIGN_RIGHT);
            cell2.addElement(pDate);

            metaTable.addCell(cell2);
            document.add(metaTable);

            // Main Details Box
            PdfPTable detailsTable = new PdfPTable(2);
            detailsTable.setWidthPercentage(100);
            detailsTable.setWidths(new float[]{3f, 1f});
            detailsTable.setSpacingBefore(10f);

            PdfPCell th1 = new PdfPCell(new Phrase("Description", boldFont));
            th1.setBackgroundColor(new Color(244, 246, 245));
            th1.setPadding(10f);
            th1.setBorderColor(Color.LIGHT_GRAY);
            detailsTable.addCell(th1);

            PdfPCell th2 = new PdfPCell(new Phrase("Amount", boldFont));
            th2.setBackgroundColor(new Color(244, 246, 245));
            th2.setPadding(10f);
            th2.setBorderColor(Color.LIGHT_GRAY);
            th2.setHorizontalAlignment(Element.ALIGN_RIGHT);
            detailsTable.addCell(th2);

            PdfPCell td1 = new PdfPCell(new Phrase("Safari Package:\n" + packageName, normalFont));
            td1.setPadding(15f);
            td1.setBorderColor(Color.LIGHT_GRAY);
            detailsTable.addCell(td1);

            PdfPCell td2 = new PdfPCell(new Phrase("$" + String.format("%.2f", amount), normalFont));
            td2.setPadding(15f);
            td2.setBorderColor(Color.LIGHT_GRAY);
            td2.setHorizontalAlignment(Element.ALIGN_RIGHT);
            detailsTable.addCell(td2);

            document.add(detailsTable);

            // Totals Area
            PdfPTable totalsTable = new PdfPTable(2);
            totalsTable.setWidthPercentage(100);
            totalsTable.setWidths(new float[]{3f, 1f});

            PdfPCell tot1 = new PdfPCell(new Phrase("Advance Deposit Paid:", boldFont));
            tot1.setBorder(Rectangle.NO_BORDER);
            tot1.setHorizontalAlignment(Element.ALIGN_RIGHT);
            tot1.setPaddingTop(15f);
            totalsTable.addCell(tot1);

            PdfPCell tot2 = new PdfPCell(new Phrase("$" + String.format("%.2f", amount), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, new Color(0, 105, 92))));
            tot2.setBorder(Rectangle.NO_BORDER);
            tot2.setHorizontalAlignment(Element.ALIGN_RIGHT);
            tot2.setPaddingTop(15f);
            totalsTable.addCell(tot2);

            document.add(totalsTable);
            document.add(new Paragraph("\n\n"));

            // Highlighted Footer Notice
            PdfPCell noticeCell = new PdfPCell(new Phrase("Notice: Please present this advance deposit receipt along with your booking reference at the park entrance gate. The remaining balance can be settled upon arrival.", FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 10, new Color(93, 64, 55))));
            noticeCell.setBackgroundColor(new Color(255, 248, 225));
            noticeCell.setBorderColor(new Color(245, 186, 66));
            noticeCell.setBorderWidthLeft(4f);
            noticeCell.setBorderWidthRight(0);
            noticeCell.setBorderWidthTop(0);
            noticeCell.setBorderWidthBottom(0);
            noticeCell.setPadding(12f);

            PdfPTable noticeTable = new PdfPTable(1);
            noticeTable.setWidthPercentage(100);
            noticeTable.addCell(noticeCell);
            document.add(noticeTable);

            document.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return out.toByteArray();
    }
}