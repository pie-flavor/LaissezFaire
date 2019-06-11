@file:Suppress("UNUSED_PARAMETER")

package flavor.pie.laissezfaire

import flavor.pie.kludge.*
import org.spongepowered.api.block.BlockState
import org.spongepowered.api.block.BlockTypes
import org.spongepowered.api.block.tileentity.carrier.TileEntityCarrier
import org.spongepowered.api.command.CommandException
import org.spongepowered.api.command.CommandResult
import org.spongepowered.api.command.CommandSource
import org.spongepowered.api.command.args.CommandContext
import org.spongepowered.api.data.key.Keys
import org.spongepowered.api.data.type.HandTypes
import org.spongepowered.api.entity.living.player.Player
import org.spongepowered.api.item.ItemType
import org.spongepowered.api.item.ItemTypes
import org.spongepowered.api.service.economy.Currency
import org.spongepowered.api.service.economy.EconomyService
import org.spongepowered.api.util.blockray.BlockRay
import org.spongepowered.api.world.Location
import org.spongepowered.api.world.World
import java.math.BigDecimal

internal object Commands {

    fun register() {
        val create = commandSpecOf {
            permission(Permissions.CREATE)
            executor(::create)
            description(!"Creates a new shop")
            arguments {
                enumValue<ShopKind>(!"kind")
                firstParsing {
                    catalogedElement<BlockState>(!"block")
                    catalogedElement<ItemType>(!"item")
                    literal(!"hand", "hand")
                }
                optionalWeak {
                    seq {
                        literal(!"buy", "buy")
                        bigDecimal(!"buy price")
                        optionalWeak { catalogedElement<Currency>(!"buy currency") }
                    }
                }
                optionalWeak {
                    seq {
                        literal(!"sell", "sell")
                        bigDecimal(!"sell price")
                        optionalWeak { catalogedElement<Currency>(!"sell currency") }
                    }
                }
            }
        }
        val base = commandSpecOf {
            child(create, "create")
        }
        CommandManager.register(plugin, base, "shop")
    }

    enum class ShopKind {
        SIGN
    }

    private fun create(src: CommandSource, args: CommandContext): CommandResult {
        if (src !is Player) {
            throw CommandException(!"Must be a player")
        }
        val svc by UncheckedService<EconomyService>()
        val shopKind: ShopKind = args.requireOne("kind")
        val sellPrice = args.getOne<BigDecimal>("sell price").unwrap()
        val sellCurrency = args.getOne<Currency>("sell currency").unwrap() ?: svc.defaultCurrency
        val buyPrice = args.getOne<BigDecimal>("buy price").unwrap()
        val buyCurrency = args.getOne<Currency>("buy currency").unwrap() ?: svc.defaultCurrency
        if (sellPrice == null && buyPrice == null) {
            throw CommandException(!"You don't have to buy " + "and".italic() + " sell but you can't do neither")
        }
        val shop: Shop
        when {
            args.hasAny("block") -> {
                val block = args.requireOne<BlockState>("block")
                shop = BlockStateShop(src.uniqueId, block, sellCurrency, sellPrice, buyCurrency, buyPrice)
            }
            args.hasAny("item") -> {
                val item = args.requireOne<ItemType>("item")
                shop = ItemTypeShop(src.uniqueId, item, sellCurrency, sellPrice, buyCurrency, buyPrice)
            }
            else -> {
                val item = src.getItemInHand(HandTypes.MAIN_HAND).unwrap()
                    ?: src.getItemInHand(HandTypes.OFF_HAND).unwrap()
                    ?: throw CommandException(!"You must be holding an item to use 'hand'")
                shop = FullItemShop(src.uniqueId, item.createSnapshot(), sellCurrency, sellPrice, buyCurrency, buyPrice)
            }
        }
        when (shopKind) {
            ShopKind.SIGN -> {
                val firstBlockHit = BlockRay.from(src)
                    .stopFilter(BlockRay.continueAfterFilter(BlockRay.onlyAirFilter(), 1))
                    .build().end().unwrap() ?: throw CommandException(!"You must be looking at a sign or container")
                val firstBlock = firstBlockHit.location
                val signBlock: Location<World>
                if (firstBlock.blockType == BlockTypes.WALL_SIGN) {
                    val direction = firstBlock[Keys.DIRECTION].get()
                    val attachedTo = firstBlock.getRelative(direction.opposite)
                    if (attachedTo.tileEntity.unwrap() !is TileEntityCarrier) {
                        throw CommandException(!"The sign must be placed on an inventory")
                    }
                    signBlock = firstBlock
                } else {
                    if (firstBlock.tileEntity.unwrap() !is TileEntityCarrier) {
                        throw CommandException(!"The sign must be placed on an inventory")
                    }
                    val direction = firstBlockHit.faces[0]
                    val free = firstBlock.getRelative(direction)
                    if (free.blockType != BlockTypes.AIR) {
                        throw CommandException(!"There is a " + free.blockType + " in the way")
                    }
                    src.storageInventory[ItemTypes.SIGN.toStack()].poll(1).unwrap()
                        ?: throw CommandException(!"You don't have any signs in your inventory")
                    free.block = blockStateOf {
                        blockType(BlockTypes.WALL_SIGN)
                        add(Keys.DIRECTION, direction)
                    }
                    signBlock = free
                }
                ShopManager.createSignShop(signBlock, shop)
            }
        }
        return CommandResult.success()
    }

}
