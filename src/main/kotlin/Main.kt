@file:Suppress("UNUSED_PARAMETER")

package flavor.pie.laissezfaire

import flavor.pie.kludge.*
import ninja.leaping.configurate.commented.CommentedConfigurationNode
import ninja.leaping.configurate.loader.ConfigurationLoader
import org.bstats.sponge.MetricsLite2
import org.spongepowered.api.asset.Asset
import org.spongepowered.api.asset.AssetId
import org.spongepowered.api.config.DefaultConfig
import org.spongepowered.api.event.Listener
import org.spongepowered.api.event.game.state.GamePreInitializationEvent
import org.spongepowered.api.plugin.Plugin
import java.nio.file.Files
import java.nio.file.Path
import javax.inject.Inject

internal lateinit var config: Config

@Plugin(
    id = Main.ID,
    name = Main.NAME,
    version = Main.VERSION,
    authors = [Main.AUTHOR],
    description = Main.DESCRIPTION
)
class Main @Inject constructor(
    @DefaultConfig(sharedRoot = true) private val loader: ConfigurationLoader<CommentedConfigurationNode>,
    @DefaultConfig(sharedRoot = true) private val file: Path,
    @AssetId("default.conf") private val asset: Asset,
    metrics: MetricsLite2
) {

    companion object {
        const val ID = "laissezfaire"
        const val NAME = "LaissezFaire"
        const val VERSION = "0.1.0"
        const val AUTHOR = "pie_flavor"
        const val DESCRIPTION = "A shop plugin"
    }

    @[Listener PublishedApi]
    internal fun onPreInit(e: GamePreInitializationEvent) {
        plugin = this
        if (!Files.exists(file)) {
            asset.copyToFile(file)
        }
        config = loader.load().getValue(typeTokenOf<Config>())!!
    }

}
