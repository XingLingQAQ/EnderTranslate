package fr.supermax_8.endertranslate.spigot;

import fr.supermax_8.endertranslate.core.EnderTranslate;
import fr.supermax_8.endertranslate.core.EnderTranslateConfig;
import fr.supermax_8.endertranslate.core.communication.ServerWebSocketClient;
import fr.supermax_8.endertranslate.core.communication.packets.ReloadPluginPacket;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class EnderTranslateCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if (!commandSender.hasPermission("endertranslate.admin")) return false;
        try {
            switch (args[0].toLowerCase()) {
                case "reload" -> {
                    CompletableFuture.runAsync(() -> {
                        commandSender.sendMessage("§7Reload...");
                        if (!EnderTranslateConfig.getInstance().isMainServer())
                            ServerWebSocketClient.getInstance().sendPacket(new ReloadPluginPacket());

                        EndertranslateSpigot.getInstance().getScheduler().runLaterAsync(() -> {
                            EnderTranslate.getInstance().reload();
                            commandSender.sendMessage("§aEnderTranslate Reloaded !");
                        }, 2);
                    });
                }
                case "editor" -> {
                    EnderTranslateConfig config = EnderTranslateConfig.getInstance();
                    String url = config.getEditorURL() + "?ws=" + config.getWsUrl() + "&secret=" + EnderTranslate.getInstance().getEditorSecret();
                    TextComponent text = Component.text("§6Click here to open the Editor !")
                            .decorate(TextDecoration.UNDERLINED)
                            .decorate(TextDecoration.BOLD)
                            .clickEvent(ClickEvent.openUrl(url));
                    BukkitAudiences.create(EnderTranslateSpigotPlugin.getInstance()).player((Player) commandSender).sendMessage(text);
                    EnderTranslate.log("Sending editor link " + url);
                }
                default -> sendHelp(commandSender);
            }
        } catch (Exception e) {
            sendHelp(commandSender);
        }
        return false;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(new String[]{
                "§8[§d§lEnderTranslate§8] " + (EnderTranslate.isVerified() ? "§6§lPremium" : "§aFree (limited to 15 translation)"),
                "§8Made by SuperMax_8",
                "§7- /endertranslate reload §fReload the plugin & the main server plugin if proxy config",
                "§7- /endertranslate editor §fSend a link to the editor (data is save automatically)"
        });
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        return List.of("reload", "editor");
    }

}