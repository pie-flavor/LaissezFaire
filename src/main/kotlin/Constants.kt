package flavor.pie.laissezfaire

import flavor.pie.kludge.*
import org.spongepowered.api.service.permission.PermissionDescription
import org.spongepowered.api.service.permission.PermissionService

object Permissions {

    const val CREATE = "laissezfaire.create"

    internal fun register() {
        val svc by UncheckedService<PermissionService>()
        svc.newDescriptionBuilder(plugin)
            .id(CREATE)
            .description(!"The permission to create new shops")
            .assign(PermissionDescription.ROLE_USER, true)
            .assign(PermissionDescription.ROLE_STAFF, true)
            .assign(PermissionDescription.ROLE_ADMIN, true)
            .register()
    }

}