package service;

import com.eseo.steevejobs.dao.ComposerDAO;
import com.eseo.steevejobs.dao.DocumentDAO;
import com.eseo.steevejobs.model.Document;
import com.eseo.steevejobs.model.Enum.DocumentStatut;
import com.eseo.steevejobs.service.DocumentService;
import com.eseo.steevejobs.service.PdfGeneratorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import service.support.MockitoJava25Support;
import service.support.TestDataFactory;

import java.sql.SQLException;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    static {
        MockitoJava25Support.enable();
    }

    @Mock
    private DocumentDAO documentDAO;
    @Mock
    private ComposerDAO composerDAO;
    @Mock
    private PdfGeneratorService pdfService;

    private DocumentService service;

    @BeforeEach
    void setUp() {
        service = new DocumentService(documentDAO, composerDAO, pdfService);
    }

    @Test
    void ajouterDocument_donneesValides_neDoitPasLeverException() throws SQLException {
        Document document = TestDataFactory.documentValide();

        assertDoesNotThrow(() -> service.ajouterDocument(document, Collections.emptyList()));
        verify(documentDAO).createDocument(document);
    }

    @Test
    void ajouterDocument_tiersManquant_doitLeverException() {
        Document document = TestDataFactory.documentValide();
        document.setTiers(null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.ajouterDocument(document, Collections.emptyList()));
        assertEquals("Le tiers est obligatoire.", ex.getMessage());
    }

    @Test
    void exporterPdf_documentInexistant_doitLeverException() throws SQLException {
        when(documentDAO.getById(99)).thenReturn(null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.exporterPdf(99));
        assertEquals("Document introuvable.", ex.getMessage());
    }

    @Test
    void exporterPdf_documentExistant_retourneUrl() throws SQLException {
        Document document = TestDataFactory.documentValide();
        document.setId(5);
        when(documentDAO.getById(5)).thenReturn(document);
        when(composerDAO.findByDocumentId(5)).thenReturn(Collections.emptyList());
        when(pdfService.genererDocument(document, Collections.emptyList())).thenReturn("http://pdf.test/devis_5.pdf");

        String url = service.exporterPdf(5);

        assertEquals("http://pdf.test/devis_5.pdf", url);
        verify(documentDAO).updateUrl(5, url);
    }

    @Test
    void supprimerDocument_echecBdd_doitLeverRuntimeException() throws SQLException {
        Document document = TestDataFactory.documentValide();
        document.setId(3);
        when(documentDAO.getById(3)).thenReturn(document);
        when(documentDAO.deleteDocument(3)).thenReturn(false);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.supprimerDocument(3));
        assertTrue(ex.getMessage().contains("Impossible de supprimer"));
    }

    @Test
    void updateStatut_delegueAuDao() throws SQLException {
        when(documentDAO.updateStatut(2, DocumentStatut.PAYE)).thenReturn(true);

        assertTrue(service.updateStatut(2, DocumentStatut.PAYE));
    }
}
