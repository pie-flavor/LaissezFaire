package flavor.pie.laissezfaire

import flavor.pie.kludge.*
import org.spongepowered.api.data.DataContainer
import org.spongepowered.api.data.DataQuery
import org.spongepowered.api.data.DataSerializable
import org.spongepowered.api.data.DataView
import org.spongepowered.api.data.key.Keys
import org.spongepowered.api.data.persistence.AbstractDataBuilder
import org.spongepowered.api.text.Text
import org.spongepowered.api.world.Location
import org.spongepowered.api.world.World
import java.util.Optional
import java.util.UUID

object ShopManager {

    internal val locationMap: MutableMap<BlockLocation, Shop> = mutableMapOf()

    fun createSignShop(sign: Location<World>, shop: Shop) {
        val event = CreateShopEvent(shop, sign)
        EventManager.post(event)
        if (event.isCancelled) {
            return
        }
        val line1 = event.shop.translation.gold()
        val line2 = event.shop.ownerId.user()!!.name.text()
        val line3 = event.shop.buyAmount?.let { (!"BUY: " + event.shop.buyCurrency.format(it)).green() } ?: Text.EMPTY
        val line4 = event.shop.sellAmount?.let { (!"SELL: " + event.shop.sellCurrency.format(it)).red() } ?: Text.EMPTY
        event.location[Keys.SIGN_LINES] = listOf(line1, line2, line3, line4)
        locationMap[BlockLocation(event.location)] = event.shop
    }

}

data class BlockLocation(val x: Int, val y: Int, val z: Int, val worldId: UUID) : DataSerializable {

    constructor(location: Location<World>)
            : this(location.blockX, location.blockY, location.blockZ, location.extent.uniqueId)

    internal companion object : AbstractDataBuilder<BlockLocation>(BlockLocation::class.java, 1) {

        val xQuery: DataQuery = DataQuery.of("X")
        val yQuery: DataQuery = DataQuery.of("Y")
        val zQuery: DataQuery = DataQuery.of("Z")
        val idQuery: DataQuery = DataQuery.of("WorldID")

        override fun buildContent(container: DataView): Optional<BlockLocation> {
            return BlockLocation(container.getInt(xQuery).get(), container.getInt(yQuery).get(),
                container.getInt(zQuery).get(), container.getObject(idQuery, UUID::class.java).get()).optional()
        }

        fun register() {
            DataManager.registerBuilder(BlockLocation::class.java, this)
        }
    }

    override fun toContainer(): DataContainer = DataContainer.createNew()
        .set(xQuery, x)
        .set(yQuery, y)
        .set(zQuery, z)
        .set(idQuery, worldId)

    override fun getContentVersion(): Int = 1

}
