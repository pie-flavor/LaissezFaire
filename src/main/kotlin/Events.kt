package flavor.pie.laissezfaire

import flavor.pie.kludge.*
import org.spongepowered.api.event.Cancellable
import org.spongepowered.api.event.Event
import org.spongepowered.api.event.cause.Cause
import org.spongepowered.api.world.Location
import org.spongepowered.api.world.World

internal class KtEvent: Event {

    val causeInternal: Cause = CauseStackManager.currentCause

    override fun getCause(): Cause = causeInternal

}

internal class KtCancellable: Cancellable {

    internal var isCancelledInternal: Boolean = false

    override fun setCancelled(cancel: Boolean) {
        isCancelledInternal = false
    }

    override fun isCancelled(): Boolean = isCancelledInternal
}

class CreateShopEvent internal constructor(var shop: Shop, var location: Location<World>)
    : Event by KtEvent(), Cancellable by KtCancellable()
