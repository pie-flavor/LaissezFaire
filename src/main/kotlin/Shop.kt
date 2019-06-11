package flavor.pie.laissezfaire

import flavor.pie.kludge.*
import org.spongepowered.api.block.BlockState
import org.spongepowered.api.data.DataContainer
import org.spongepowered.api.data.DataQuery
import org.spongepowered.api.data.DataSerializable
import org.spongepowered.api.data.DataView
import org.spongepowered.api.data.persistence.AbstractDataBuilder
import org.spongepowered.api.data.persistence.InvalidDataException
import org.spongepowered.api.item.ItemType
import org.spongepowered.api.item.inventory.ItemStack
import org.spongepowered.api.item.inventory.ItemStackComparators
import org.spongepowered.api.item.inventory.ItemStackSnapshot
import org.spongepowered.api.service.economy.Currency
import org.spongepowered.api.text.translation.Translation
import java.math.BigDecimal
import java.util.Optional
import java.util.UUID

sealed class Shop(val ownerId: UUID, val sellCurrency: Currency, val sellAmount: BigDecimal?, val buyCurrency: Currency, val buyAmount: BigDecimal?) : DataSerializable {

    internal companion object : AbstractDataBuilder<Shop>(Shop::class.java, 1) {

        val ownerIdQuery: DataQuery = DataQuery.of("OwnerID")
        val sellCurrencyQuery: DataQuery = DataQuery.of("SellCurrency")
        val sellAmountQuery: DataQuery = DataQuery.of("SellAmount")
        val buyCurrencyQuery: DataQuery = DataQuery.of("BuyCurrency")
        val buyAmountQuery: DataQuery = DataQuery.of("BuyAmount")
        val shopTypeQuery: DataQuery = DataQuery.of("ShopType")
        val itemTypeQuery: DataQuery = DataQuery.of("ItemType")
        val blockStateQuery: DataQuery = DataQuery.of("BlockState")
        val fullItemQuery: DataQuery = DataQuery.of("FullItem")

        override fun buildContent(container: DataView): Optional<Shop> {
            val ownerId = container.getObject(ownerIdQuery, UUID::class.java).get()
            val sellCurrency = container.getCatalogType(sellCurrencyQuery, Currency::class.java).get()
            val buyCurrency = container.getCatalogType(buyCurrencyQuery, Currency::class.java).get()
            val sellAmount = container.getObject(sellAmountQuery, BigDecimal::class.java).unwrap()
            val buyAmount = container.getObject(buyAmountQuery, BigDecimal::class.java).unwrap()
            if (sellAmount == null && buyAmount == null) {
                throw InvalidDataException()
            }
            return when (container.getObject(shopTypeQuery, ShopType::class.java).get()) {
                ShopType.FULL_ITEM -> {
                    FullItemShop(ownerId, container.getSerializable(fullItemQuery, ItemStackSnapshot::class.java).get(), sellCurrency, sellAmount, buyCurrency, buyAmount).optional()
                }
                ShopType.BLOCK_STATE -> {
                    BlockStateShop(ownerId, container.getCatalogType(blockStateQuery, BlockState::class.java).get(), sellCurrency, sellAmount, buyCurrency, buyAmount).optional()
                }
                ShopType.ITEM_TYPE -> {
                    ItemTypeShop(ownerId, container.getCatalogType(itemTypeQuery, ItemType::class.java).get(), sellCurrency, sellAmount, buyCurrency, buyAmount).optional()
                }
            }
        }

        fun register() {
            DataManager.registerBuilder(Shop::class.java, Shop)
        }
    }

    abstract fun validateItem(stack: ItemStack): Boolean

    abstract fun validateItem(stack: ItemStackSnapshot): Boolean

    abstract val translation: Translation

    override fun toContainer(): DataContainer {
        val container = DataContainer.createNew()
            .set(ownerIdQuery, ownerId)
            .set(sellCurrencyQuery, sellCurrency)
            .set(buyCurrencyQuery, buyCurrency)
        if (sellAmount != null) {
            container.set(sellAmountQuery, sellAmount)
        }
        if (buyAmount != null) {
            container.set(buyAmountQuery, buyAmount)
        }
        return when (this) {
            is ItemTypeShop -> {
                container.set(shopTypeQuery, ShopType.ITEM_TYPE)
                    .set(itemTypeQuery, item)
            }
            is BlockStateShop -> {
                container.set(shopTypeQuery, ShopType.BLOCK_STATE)
                    .set(blockStateQuery, block)
            }
            is FullItemShop -> {
                container.set(shopTypeQuery, ShopType.FULL_ITEM)
                    .set(fullItemQuery, item)
            }
        }
    }

    override fun getContentVersion(): Int = 1

}

internal enum class ShopType {
    ITEM_TYPE, BLOCK_STATE, FULL_ITEM
}

class ItemTypeShop(ownerId: UUID, val item: ItemType, sellCurrency: Currency, sellAmount: BigDecimal?, buyCurrency: Currency, buyAmount: BigDecimal?)
    : Shop(ownerId, sellCurrency, sellAmount, buyCurrency, buyAmount) {

    override fun validateItem(stack: ItemStack): Boolean = stack.type == item

    override fun validateItem(stack: ItemStackSnapshot): Boolean = stack.type == item

    override val translation: Translation = item.translation

}

class BlockStateShop(ownerId: UUID, val block: BlockState, sellCurrency: Currency, sellAmount: BigDecimal?, buyCurrency: Currency, buyAmount: BigDecimal?)
    : Shop(ownerId, sellCurrency, sellAmount, buyCurrency, buyAmount) {

    val referenceStack: ItemStack = block.toStack()

    override fun validateItem(stack: ItemStack): Boolean =
        BlockStateItemComparator.compare(stack, referenceStack) == 0

    override fun validateItem(stack: ItemStackSnapshot): Boolean = validateItem(stack.createStack())

    override val translation: Translation = referenceStack.translation

}

class FullItemShop(ownerId: UUID, val item: ItemStackSnapshot, sellCurrency: Currency, sellAmount: BigDecimal?, buyCurrency: Currency, buyAmount: BigDecimal?)
    : Shop(ownerId, sellCurrency, sellAmount, buyCurrency, buyAmount) {

    private val stack = item.createStack()

    override fun validateItem(stack: ItemStack): Boolean =
        ItemStackComparators.IGNORE_SIZE.compare(stack, this.stack) == 0

    override fun validateItem(stack: ItemStackSnapshot): Boolean = validateItem(stack.createStack())

    override val translation: Translation = item.translation

}

object BlockStateItemComparator : Comparator<ItemStack> {

    val query: DataQuery = DataQuery.of("UnsafeDamage")

    override fun compare(o1: ItemStack, o2: ItemStack): Int {
        val typeCmp = o1.type.id.compareTo(o2.type.id)
        if (typeCmp != 0) {
            return typeCmp
        }
        return o1.toContainer().getInt(query).orElse(0).compareTo(o2.toContainer().getInt(query).orElse(0))
    }

}
