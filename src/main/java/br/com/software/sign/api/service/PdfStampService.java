package br.com.software.sign.api.service;

import br.com.software.sign.api.dto.FooterDTO;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDPageContentStream.AppendMode;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class PdfStampService {

    public byte[] addFooterLeftLastPage(byte[] pdfBytes, FooterDTO dto) throws IOException {
        DadosFooter f = new DadosFooter(
                dto.getInscricaoEstadual(),
                dto.getCnpj(),
                dto.getDataCorte(),
                dto.getRazaoSocial(),
                dto.getResponsavelLicenca(),
                dto.getEmailNf()
        );
        return addFooterLeftLastPage(pdfBytes, f);
    }

    // Topo-direita FIXO, texto ALINHADO À ESQUERDA, com wrap controlado
    public byte[] addFooterLeftLastPage(byte[] pdfBytes, DadosFooter f) throws IOException {
        try (PDDocument doc = PDDocument.load(pdfBytes)) {
            int last = doc.getNumberOfPages() - 1;
            PDPage page = doc.getPage(last);

            float pageW = page.getMediaBox().getWidth();
            float pageH = page.getMediaBox().getHeight();

            final float RIGHT  = 24f;  // distância da borda direita
            final float TOP    = 66f;  // distância do topo
            final float SAFETY = 4f;

            // tipografia
            final float fs = 6f;
            final float lh = 9f;
            final PDFont font = PDType1Font.HELVETICA_BOLD_OBLIQUE;

            final float WRAP_W = 240f;
            float maxWrap = Math.max(WRAP_W, 80f);
            maxWrap = Math.min(maxWrap, pageW - RIGHT - SAFETY);

            List<String> lines = new ArrayList<>();
            lines.addAll(wrap("Inscrição Estadual: " + safe(f.inscricaoEstadual()), font, fs, maxWrap));
            lines.addAll(wrap("CNPJ: "               + safe(f.cnpj()),              font, fs, maxWrap));
            lines.addAll(wrap("Data de corte: "      + safe(f.dataCorte()),         font, fs, maxWrap));
            lines.addAll(wrap("Razão Social: "       + safe(f.razaoSocial()),       font, fs, maxWrap));
            lines.addAll(wrap("Responsável Licença: "+ safe(f.responsavelLicenca()),font, fs, maxWrap));
            lines.addAll(wrap("Email NF: "           + safe(f.emailNf()),           font, fs, maxWrap));

            float xLeft = pageW - RIGHT - maxWrap;
            float firstBaselineY = pageH - TOP - fs;

            try (PDPageContentStream cs = new PDPageContentStream(
                    doc, page, AppendMode.APPEND, true, true)) {
                cs.setNonStrokingColor(0, 0, 0);
                cs.beginText();
                cs.setFont(font, fs);
                cs.setLeading(lh);
                cs.newLineAtOffset(xLeft, firstBaselineY);
                for (String ln : lines) {
                    cs.showText(ln);
                    cs.newLine();
                }
                cs.endText();
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            return out.toByteArray();
        }
    }

    private static float stringW(PDFont font, float fs, String s) throws IOException {
        return font.getStringWidth(s) / 1000f * fs;
    }

    private static List<String> wrap(String text, PDFont font, float fs, float maxWidth) throws IOException {
        List<String> out = new ArrayList<>();
        if (text == null || text.isBlank()) { out.add(""); return out; }

        String[] words = text.split("\\s+");
        StringBuilder line = new StringBuilder();

        for (String w : words) {
            String candidate = line.length() == 0 ? w : line + " " + w;
            if (stringW(font, fs, candidate) <= maxWidth) {
                line.setLength(0);
                line.append(candidate);
            } else {
                if (line.length() > 0) {
                    out.add(line.toString());
                    line.setLength(0);
                }
                if (stringW(font, fs, w) > maxWidth) {
                    int start = 0;
                    while (start < w.length()) {
                        int end = start + 1;
                        while (end <= w.length()
                                && stringW(font, fs, w.substring(start, end)) <= maxWidth) end++;
                        out.add(w.substring(start, end - 1));
                        start = end - 1;
                    }
                } else {
                    line.append(w);
                }
            }
        }
        if (line.length() > 0) out.add(line.toString());
        return out;
    }

    private String safe(String s) { return s == null ? "" : s; }

    public static record DadosFooter(
            String inscricaoEstadual,
            String cnpj,
            String dataCorte,
            String razaoSocial,
            String responsavelLicenca,
            String emailNf
    ) {}
}
