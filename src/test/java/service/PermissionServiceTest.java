package service;

import com.eseo.steevejobs.dao.PermissionDao;
import com.eseo.steevejobs.model.Permission;
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

@ExtendWith(MockitoExtension.class)
class PermissionServiceTest {

    static {
        MockitoJava25Support.enable();
    }

    @Mock
    private PermissionDao permissionDao;

    private PermissionService service;

    @BeforeEach
    void setUp() {
        service = new PermissionService(permissionDao);
    }

    @Test
    void getUserPermissions_idValide_retourneLesCodes() {
        when(permissionDao.getPermissionCodesByUserId(1)).thenReturn(List.of("APP_STOCK_VIEW", "APP_RH_VIEW"));

        List<String> permissions = service.getUserPermissions(1);

        assertEquals(2, permissions.size());
        verify(permissionDao).getPermissionCodesByUserId(1);
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
        verify(permissionDao, never()).insertRolePermission(anyString(), anyInt());
    }

    @Test
    void assignPermissionToRole_parametresValides_doitReussir() {
        when(permissionDao.insertRolePermission("ADMIN", 3)).thenReturn(true);

        assertTrue(service.assignPermissionToRole("ADMIN", 3));
        verify(permissionDao).insertRolePermission("ADMIN", 3);
    }

    @Test
    void assignPermissionToRole_idPermissionInvalide_doitEchouer() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.assignPermissionToRole("ADMIN", 0));
        assertEquals("L'ID de la permission est invalide.", ex.getMessage());
        verify(permissionDao, never()).insertRolePermission(anyString(), anyInt());
    }

    @Test
    void revokePermissionFromRole_doitAppelerLeDao() {
        when(permissionDao.deleteRolePermission("RH", 2)).thenReturn(true);

        assertTrue(service.revokePermissionFromRole("RH", 2));
        verify(permissionDao).deleteRolePermission("RH", 2);
    }

    @Test
    void createNewPermission_codeVide_doitEchouer() {
        assertFalse(service.createNewPermission("", "Description"));
        verify(permissionDao, never()).createPermission(anyString(), anyString());
    }

    @Test
    void getPermissionIdsByRole_nomRoleVide_retourneListeVide() {
        assertThrows(IllegalArgumentException.class, () -> service.getPermissionIdsByRole("   "));
        verify(permissionDao, never()).getPermissionIdsByRole(anyString());
    }

    @Test
    void getAllPermissions_delegueAuDao() {
        when(permissionDao.getAllPermissions()).thenReturn(List.of(new Permission(1, "APP_ADMINPANEL_VIEW", "Admin")));

        assertEquals(1, service.getAllPermissions().size());
    }
}
