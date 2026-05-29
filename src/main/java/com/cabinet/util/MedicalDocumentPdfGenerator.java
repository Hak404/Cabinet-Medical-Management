package com.cabinet.util;

import com.cabinet.model.DocumentMedical;
import com.cabinet.model.MedicamentOrdonnance;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.ListItem;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Génération de PDF médicaux (OpenPDF) : ordonnance, analyses, compte rendu.
 */
public final class MedicalDocumentPdfGenerator {

    private static final DateTimeFormatter DISPLAY_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Font TITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
    private static final Font HEADER_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
    private static final Font BODY_FONT = FontFactory.getFont(FontFactory.HELVETICA, 11);
    private static final Font SMALL_FONT = FontFactory.getFont(FontFactory.HELVETICA, 9);

    private MedicalDocumentPdfGenerator() {
    }

    public static void generateOrdonnance(Path output,
                                         String patientNom,
                                         String medecinNom,
                                         LocalDate date,
                                         List<MedicamentOrdonnance> medicaments) throws IOException, DocumentException {
        writePdf(output, "ORDONNANCE MÉDICALE", patientNom, medecinNom, date, document -> {
            document.add(new Paragraph("Prescription :", HEADER_FONT));
            document.add(spacer());
            if (medicaments == null || medicaments.isEmpty()) {
                document.add(new Paragraph("Aucun médicament prescrit.", BODY_FONT));
                return;
            }
            PdfPTable table = new PdfPTable(3);
            table.setWidthPercentage(100);
            table.addCell(headerCell("Médicament"));
            table.addCell(headerCell("Posologie"));
            table.addCell(headerCell("Durée"));
            for (MedicamentOrdonnance m : medicaments) {
                table.addCell(bodyCell(m.getNom()));
                table.addCell(bodyCell(m.getPosologie()));
                table.addCell(bodyCell(m.getDuree()));
            }
            document.add(table);
        });
    }

    public static void generateAnalyses(Path output,
                                       String patientNom,
                                       String medecinNom,
                                       LocalDate date,
                                       List<String> codesAnalyse,
                                       Map<String, String> labels) throws IOException, DocumentException {
        writePdf(output, "DEMANDE D'ANALYSES MÉDICALES", patientNom, medecinNom, date, document -> {
            document.add(new Paragraph("Examens demandés :", HEADER_FONT));
            document.add(spacer());
            if (codesAnalyse == null || codesAnalyse.isEmpty()) {
                document.add(new Paragraph("Aucune analyse demandée.", BODY_FONT));
                return;
            }
            com.lowagie.text.List list = new com.lowagie.text.List(com.lowagie.text.List.UNORDERED);
            for (String code : codesAnalyse) {
                String label = labels != null && labels.containsKey(code) ? labels.get(code) : code;
                list.add(new ListItem(code + " — " + label, BODY_FONT));
            }
            document.add(list);
        });
    }

    public static void generateCompteRendu(Path output,
                                           String patientNom,
                                           String medecinNom,
                                           LocalDate date,
                                           String diagnostic,
                                           String remarque) throws IOException, DocumentException {
        writePdf(output, "COMPTE RENDU DE CONSULTATION", patientNom, medecinNom, date, document -> {
            document.add(new Paragraph("Diagnostic", HEADER_FONT));
            document.add(new Paragraph(nullToDash(diagnostic), BODY_FONT));
            document.add(spacer());
            document.add(new Paragraph("Remarques / compte rendu", HEADER_FONT));
            document.add(new Paragraph(nullToDash(remarque), BODY_FONT));
        });
    }

    public static String defaultTitre(DocumentMedical.TypeDocument type, LocalDate date) {
        String d = DISPLAY_DATE.format(date != null ? date : LocalDate.now());
        return switch (type) {
            case ORDONNANCE -> "Ordonnance du " + d;
            case ANALYSE -> "Analyses médicales du " + d;
            case COMPTE_RENDU -> "Compte rendu du " + d;
        };
    }

    private interface PdfContentWriter {
        void write(Document document) throws DocumentException;
    }

    private static void writePdf(Path output,
                                 String documentTitle,
                                 String patientNom,
                                 String medecinNom,
                                 LocalDate date,
                                 PdfContentWriter content) throws IOException, DocumentException {
        Files.createDirectories(output.getParent());
        try (OutputStream out = Files.newOutputStream(output)) {
            Document document = new Document();
            PdfWriter.getInstance(document, out);
            document.open();

            document.add(new Paragraph("Cabinet Médical", SMALL_FONT));
            document.add(new Paragraph(documentTitle, TITLE_FONT));
            document.add(spacer());

            PdfPTable meta = new PdfPTable(2);
            meta.setWidthPercentage(100);
            meta.addCell(metaLabel("Patient"));
            meta.addCell(metaValue(patientNom));
            meta.addCell(metaLabel("Médecin"));
            meta.addCell(metaValue(medecinNom));
            meta.addCell(metaLabel("Date"));
            meta.addCell(metaValue(DISPLAY_DATE.format(date != null ? date : LocalDate.now())));
            document.add(meta);
            document.add(spacer());

            content.write(document);

            document.add(spacer());
            document.add(new Paragraph(
                    "Document généré automatiquement — à conserver et présenter si nécessaire.",
                    SMALL_FONT));

            document.close();
        }
    }

    private static Paragraph spacer() {
        return new Paragraph(" ", BODY_FONT);
    }

    private static PdfPCell headerCell(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, HEADER_FONT));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        return cell;
    }

    private static PdfPCell bodyCell(String text) {
        return new PdfPCell(new Phrase(nullToDash(text), BODY_FONT));
    }

    private static PdfPCell metaLabel(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, HEADER_FONT));
        cell.setBorder(PdfPCell.NO_BORDER);
        return cell;
    }

    private static PdfPCell metaValue(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(nullToDash(text), BODY_FONT));
        cell.setBorder(PdfPCell.NO_BORDER);
        return cell;
    }

    private static String nullToDash(String s) {
        if (s == null || s.isBlank()) {
            return "—";
        }
        return s.trim();
    }

}
