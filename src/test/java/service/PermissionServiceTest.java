package service;

import com.eseo.steevejobs.dao.PermissionDAO;
import com.eseo.steevejobs.service.PermissionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import service.support.MockitoJava25Support;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires de {@link com.eseo.steevejobs.service.PermissionService}.
 * <p>
 * Couvre lecture des permissions utilisateur, affectation et révocation par rôle, garde-fous SuperAdmin.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class PermissionServiceTest {

    static {
        MockitoJava25Support.enable();
    }

    @Mock
    private PermissionDAO permissionDAO;

    private PermissionService service;

    @BeforeEach
    void setUp() {
        service = new PermissionService(permissionDAO);
    }

    @Test
    void getUserPermissions_idValide_retourneLesCodes() {
        when(permissionDAO.getPermissionCodesByUserId(1)).thenReturn(List.of("APP_STOCK_VIEW", "APP_RH_VIEW"));

        List<String> permissions = service.getUserPermissions(1);

        assertEquals(2, permissions.size());
        verify(permissionDAO).getPermissionCodesByUserId(1);
    }

    @Test
    void getUserPermissions_idInvalide_doitLeverException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.getUserPermissions(0));
        assertEquals("L'ID utilisateur est invalide.", ex.getMessage());
    }

    @Test
    void assignPermissionToRole_superAdmin_doitEchouer() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.assignPermissionToRole("SuperAdmin", 5));
        assertTrue(ex.getMessage().contains("SuperAdmin"));
        verify(permissionDAO, never()).insertRolePermission(anyString(), anyInt());
    }

    @Test
    void assignPermissionToRole_parametresValides_doitReussir() {
        when(permissionDAO.insertRolePermission("ADMIN", 3)).thenReturn(true);

        assertTrue(service.assignPermissionToRole("ADMIN", 3));
        verify(permissionDAO).insertRolePermission("ADMIN", 3);
    }

    @Test
    void assignPermissionToRole_idPermissionInvalide_doitEchouer() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.assignPermissionToRole("ADMIN", 0));
        assertEquals("L'ID de la permission est invalide.", ex.getMessage());
        verify(permissionDAO, never()).insertRolePermission(anyString(), anyInt());
    }

    @Test
    void revokePermissionFromRole_doitAppelerLeDao() {
        when(permissionDAO.deleteRolePermission("RH", 2)).thenReturn(true);

        assertTrue(service.revokePermissionFromRole("RH", 2));
        verify(permissionDAO).deleteRolePermission("RH", 2);
    }

}
