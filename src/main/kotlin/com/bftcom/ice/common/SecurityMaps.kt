package com.bftcom.ice.common

import com.bftcom.ice.common.maps.*
//import com.bftcom.ice.common.sign.Certificate

object AdministrationGroup : FieldSetGroup("administration", "Администрирование")
object SettingsGroup: FieldSetGroup("settings", "Настройки")

object UsersRolesGroup : FieldSetGroup("usersRoles", "Пользователи и роли", AdministrationGroup)

object AuthorizationCode : MFS<AuthorizationCode>("AuthorizationCode", "AuthorizationCode", AdministrationGroup) {
    val id = Field.id()
    val data = Field.stringNN("data")
    val expirationDate = Field.timestampNN("expirationDate")
}

object AuthorizationType : MFS<AuthorizationType>("AuthorizationType", "AuthorizationType", AdministrationGroup) {
    val id = Field.id()
    val code = Field.stringNN("code")
    val name = Field.stringNN("name")
}

object UserAccountAuthorizationType : MFS<UserAccountAuthorizationType>("UserAccountAuthorizationType",
        "UserAccountAuthorizationType", AdministrationGroup) {
    val id = Field.id()
    val userAccount = Field.referenceNN("UserAccount", UserAccount)
    val authorizationType = Field.referenceNN("AuthorizationType", AuthorizationType)
}

object UserAccount : MFS<UserAccount>("useraccount", "Пользователи", UsersRolesGroup) {
    val id = Field.id()
    val fullName = Field.stringNN("fullName") { caption = "Полное имя"; length = 255 }
    val name = Field.stringNN("name") { caption = "Логин"; length = 50 }
    val email = Field.string("email") { caption = "Эл. почта"; length = 255 }
    val phone = Field.string("phone") { caption = "Телефон"}
    val description = Field.string("description") { caption = "Примечание"; length = 2000 }
    val password = Field.stringNN("password") { caption = "Пароль"; length = 255 }
    val blocked = Field.boolean("blocked") { caption = "Заблокирован" }
    val blockedReason = Field.string("blockedReason") { caption = "Причина блокировки"; length = 1024 }
    val passwordChangeDate = Field.date("passwordChangeDate") { caption = "Дата смены пароля" }
    val passwordEnterCount = Field.int("passwordEnterCount") { caption = "Попыток ввода пароля" }
    val mustChangePassword = Field.boolean("mustChangePassword") { caption = "Потребовать смену пароля" }
    val lastLoginTime = Field.timestamp("lastLoginTime") { caption = "Время последнего входа" }
    val userPolicy = Field.referenceNN("userPolicy", UserPolicy) { caption = "Политика безопасности" }
    val userRoles = Field.list("userRoles", UserRole) { caption = "Роли" }
    //val loginCertificate = Field.reference("loginCertificate", Certificate) { caption = "Сертификат для логина" }
    val authorizationTypes = Field.list("UserAccountAuthorizationTypes", UserAccountAuthorizationType) { caption = "Разрешенные типы аутентификации" }
    val recieveEmails = Field.booleanNN("recieveEmails") { caption = "Получать электонные письма" }
    val recieveSms = Field.booleanNN("recieveSms") { caption = "Получать СМС" }

    override val nativeKey by lazy { listOf(name) }
}

data class Initials(val name: String?, val surname: String, val thirdName: String?) {

    override fun toString(): String {
        return "${surname.trim()}${name?.trim()?.let { " $it" }.orEmpty()}${thirdName?.trim()?.let { " $it" }.orEmpty()}"
    }

    companion object {
        fun fromString(value: String): Initials {
            val parts = value.splitToSequence(' ')
            // surname-name-third policy
            val surname = parts.first()
            val name = parts.drop(1).firstOrNull() ?: ""
            val third = parts.drop(2).ifEmpty { sequenceOf("") }.reduce { a,b -> "$a $b" }.ifEmpty { null }
            return Initials(name, surname, third)
        }
    }
}

object UserRole : MFS<UserRole>("userrole", "Роли пользователя", UsersRolesGroup) {
    val id = Field.id()
    val user = Field.referenceNN("user", UserAccount) { caption = "Пользователь" }
    val role = Field.referenceNN("role", Role) { caption = "Роль" }

    override val nativeKey by lazy { listOf(user, role) }
}

object Role : MFS<Role>("Role", "Роли", UsersRolesGroup) {
    val id = Field.id()
    val code = Field.stringNN("code") { caption = "Код"; length = 50 }
    val name = Field.stringNN("name") { caption = "Наименование"; length = 255 }
    val description = Field.string("description") { caption = "Описание"; length = 1024 }
    val isSystem = Field.boolean("isSystem") { caption = "Системная" }
    val permissions = Field.list("rolePermissions", RolePermission) { caption = "Полномочия роли" }
    val parents = Field.list("parents", RoleParent) { caption = "Включённые роли"; thatJoinColumn = "roleId"; tooltip = "Включение настроек доступа из указанных ролей" }
    val userRoles = Field.list("userRoles", UserRole) { caption = "Пользователи"; thatJoinColumn = "roleId" }

    override val nativeKey by lazy { listOf(code) }
}

object RoleParent : MFS<RoleParent>(caption = "Включённые роли", group = UsersRolesGroup) {
    val id = Field.id()
    val role = Field.referenceNN("role", Role) { caption = "Роль"; }
    val parent = Field.referenceNN("parent", Role) { caption = "Включённая роль" }
}

object RolePermission : MFS<RolePermission>("RolePermission", "Полномочия роли", UsersRolesGroup) {
    val id = Field.id()
    val role = Field.referenceNN("role", Role) { caption = "Роль" }
    val appObj = Field.stringNN("appObj") { caption = "Объект приложения"; length = 50 }
    val fieldName = Field.string("fieldName") { caption = "Атрибут объекта"; length = 50 }
    val canCreate = Field.boolean("canCreate") { caption = "Создание" }
    val canRead = Field.boolean("canRead") { caption = "Чтение" }
    val canWrite = Field.boolean("canWrite") { caption = "Изменение" }
    val canDelete = Field.boolean("canDelete") { caption = "Удаление" }
}

object UserPolicy : MFS<UserPolicy>("userpolicy", "Политики безопасности", UsersRolesGroup) {
    val id = Field.id()
    val name = Field.stringNN("name") { caption = "Наименование"; length = 50 }
    val sessionExpirationTime = Field.int("sessionExpirationTime") { caption = "Попыток ввода пароля" }
    val passwordExpirationDays = Field.int("passwordExpirationDays") { caption = "Срок действия пароля, дн."; max = 365000 }
    val passwordEnterCount = Field.int("passwordEnterCount") { caption = "Кол-во неудачных попыток ввода пароля"; max = 1000 }
    val passwordMinLength = Field.int("passwordMinLength") { caption = "Минимальная длина пароля"; max = 1000 }

    override val nativeKey by lazy { listOf(name) }
}

fun DataMapF<RolePermission>.getPermittedActions(): Set<PermissionAction> {
    return PermissionAction.values().filter { this[it.permissionField] == true }.toSet()
}

enum class PermissionAction(val permissionField: Field<*, Boolean?>) {
    CREATE(RolePermission.canCreate),
    READ(RolePermission.canRead),
    WRITE(RolePermission.canWrite),
    DELETE(RolePermission.canDelete)
}

data class LoginRequest(var username: String?, var password: String?)

data class LoginResponse(
        var accessToken: String?,
        var tokenType: String? = "Bearer",
        var mustChangePassword: Boolean?,
        var onlySSOEnabled: Boolean? = null,
        var useAlternativeSso: Boolean? = null,
        var oauth2Error: String? = null,
        var redirectToGatewayAfterLogout: Boolean? = null,
        var gateWayLinksPageUrl: String? = null
)

data class ChangePasswordRequest(var oldPassword: String?, var newPassword: String?)