package service;

import com.eseo.steevejobs.dao.FichePayeDAO;
import com.eseo.steevejobs.dao.PlanningDAO;
import com.eseo.steevejobs.model.FichePaye;
import com.eseo.steevejobs.model.User;
import com.eseo.steevejobs.service.FichePayeService;
import com.eseo.steevejobs.service.PdfGeneratorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import service.support.MockitoJava25Support;
import service.support.TestDataFactory;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FichePayeServiceTest {

    static {
        MockitoJava25Support.enable();
    }

    @Mock
    private FichePayeDAO fichePayeDAO;
    @Mock
    private PlanningDAO planningDAO;
    @Mock
    private PdfGeneratorService pdfService;

    private FichePayeService service;

    @BeforeEach
    void setUp() {
        service = new FichePayeService(fichePayeDAO, planningDAO, pdfService);
    }

    @Test
    void genererFichePaye_donneesValides_creeLaFiche() throws SQLException {
        User employe = TestDataFactory.utilisateurActif(1, "employe@mail.fr");
        LocalDateTime mois = LocalDateTime.of(2026, 4, 1, 0, 0);

        when(fichePayeDAO.findByEmployeIdAndDate(1, mois)).thenReturn(null);
        when(planningDAO.findByUserId(1)).thenReturn(Collections.emptyList());
        when(pdfService.genererFichePaye(any(FichePaye.class), eq(2500.0), eq(0.45), eq(0L), eq(160.0), eq(15.0)))
                .thenReturn("http://pdf.test/fiche_1.pdf");
        doAnswer(invocation -> {
            FichePaye fiche = invocation.getArgument(0);
            fiche.setId(10);
            return null;
        }).when(fichePayeDAO).createFichePaye(any(FichePaye.class));

        FichePaye resultat = service.genererFichePaye(employe, mois, 2500, 0.45, 160, 15);

        assertNotNull(resultat);
        assertEquals("http://pdf.test/fiche_1.pdf", resultat.getUrl());
        verify(fichePayeDAO).updateUrl(10, "http://pdf.test/fiche_1.pdf");
    }

    @Test
    void genererFichePaye_ficheExistante_doitLeverIllegalStateException() throws SQLException {
        User employe = TestDataFactory.utilisateurActif(1, "employe@mail.fr");
        LocalDateTime mois = LocalDateTime.of(2026, 4, 1, 0, 0);
        when(fichePayeDAO.findByEmployeIdAndDate(1, mois)).thenReturn(new FichePaye(5, mois, "", employe));

        assertThrows(IllegalStateException.class,
                () -> service.genererFichePaye(employe, mois, 2500, 0.45, 160, 15));
    }

    @Test
    void genererFichePaye_salaireBrutNegatif_doitLeverException() {
        User employe = TestDataFactory.utilisateurActif(1, "employe@mail.fr");
        LocalDateTime mois = LocalDateTime.of(2026, 4, 1, 0, 0);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.genererFichePaye(employe, mois, 0, 0.45, 160, 15));
        assertEquals("Le salaire brut doit être supérieur à 0.", ex.getMessage());
    }

    @Test
    void genererFichePaye_tauxPatronalInvalide_doitLeverException() {
        User employe = TestDataFactory.utilisateurActif(1, "employe@mail.fr");
        LocalDateTime mois = LocalDateTime.of(2026, 4, 1, 0, 0);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.genererFichePaye(employe, mois, 2500, 1.2, 160, 15));
        assertEquals("Le taux de cotisations patronales doit être entre 0 et 1.", ex.getMessage());
    }

}
