package com.mhyddnn.mubarokappmobile.core.datastore

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UserSessionTest {

    private fun session(vararg perms: String, superAdmin: Boolean = false) =
        UserSession("u", null, null, perms.toSet(), superAdmin)

    @Test
    fun has_exactWildcardAndSuper() {
        assertTrue(session("sales-kanvas:read").has("sales-kanvas:read"))
        assertTrue(session("sales-kanvas:*").has("sales-kanvas:write"))
        assertTrue(session("*").has("apa-saja:read"))
        assertTrue(session(superAdmin = true).has("sales-kanvas:write"))
    }

    @Test
    fun has_deniesOtherModuleAndOtherAction() {
        assertFalse(session("sales-kanvas:read").has("sales-kanvas:write"))
        assertFalse(session("sales-kanvas-plan:*").has("sales-kanvas:read"))
        assertFalse(session().has("sales-kanvas:read"))
    }

    @Test
    fun isSuperAdminRole_nameVariantsOrRoleId1() {
        assertTrue(isSuperAdminRole(" Super Admin ", 5))
        assertTrue(isSuperAdminRole("superadmin", null))
        assertTrue(isSuperAdminRole("super-admin", null))
        assertTrue(isSuperAdminRole("Sales", 1))
        assertFalse(isSuperAdminRole("Sales", 2))
        assertFalse(isSuperAdminRole(null, null))
    }
}
