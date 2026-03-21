package com.liuyue.beep_Velocity;

import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.protocol.sound.SoundCategory;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntitySoundEffect;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Inject;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.plugin.Dependency;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.translation.GlobalTranslator;
import net.kyori.adventure.translation.Translator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.sound.Sounds;

@Plugin(id = "beep_velocity", name = "Beep Velocity", version = "1.0", dependencies = {@Dependency(id = "packetevents")}, description = "Beeps when someone is mentioned in text with an @", authors = {"Liuyue_awa"})
public class BeepVelocity {
    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;
    private final Map<Locale, Map<String, String>> langCache = new HashMap<>();
    private final Gson gson = new Gson();

    @Inject
    public BeepVelocity(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        loadAllLanguages();
        server.getCommandManager().register(server.getCommandManager().metaBuilder("@").build(), new BeepCommand(false));
        server.getCommandManager().register(server.getCommandManager().metaBuilder("@@").build(), new BeepCommand(true));
    }

    private void loadAllLanguages() {
        Path langFolder = dataDirectory.resolve("languages");
        try {
            if (!Files.exists(langFolder)) {
                Files.createDirectories(langFolder);
                saveDefaultLanguageFile(langFolder, "zh_cn.json");
                saveDefaultLanguageFile(langFolder, "en_us.json");
            }
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(langFolder, "*.json")) {
                for (Path entry : stream) {
                    String fileName = entry.getFileName().toString().replace(".json", "");
                    Locale locale = Locale.forLanguageTag(fileName.replace("_", "-"));

                    try (Reader reader = Files.newBufferedReader(entry)) {
                        Map<String, String> data = gson.fromJson(reader, new TypeToken<Map<String, String>>(){}.getType());
                        langCache.put(locale, data);
                        logger.info("Loaded language: {}", locale.toLanguageTag());
                    }
                }
            }

            GlobalTranslator.translator().addSource(new Translator() {
                @Override
                public @NotNull Key name() {
                    return Key.key("igny", "beep_translator");
                }

                @Override
                public @Nullable MessageFormat translate(@NotNull String key, @NotNull Locale locale) {
                    Map<String, String> map = langCache.get(locale);
                    if (map == null) {
                        map = langCache.get(Locale.forLanguageTag(locale.getLanguage()));
                    }

                    if (map != null && map.containsKey(key)) {
                        return new MessageFormat(map.get(key));
                    }
                    return null;
                }
            });

        } catch (IOException e) {
            logger.error("Failed to load languages", e);
        }
    }

    private void saveDefaultLanguageFile(Path folder, String name) throws IOException {
        Path target = folder.resolve(name);
        if (Files.exists(target)) return;

        try (InputStream in = getClass().getResourceAsStream("/" + name)) {
            if (in != null) {
                Files.copy(in, target);
            } else {
                String content = name.contains("zh")
                        ? "{\"beep.title\":\"{0}在@你\",\"beep.subtitle\":\"\",\"beep.console\":\"控制台\"}"
                        : "{\"beep.title\":\"{0} is pinging you\",\"beep.subtitle\":\"\",\"beep.console\":\"Console\"}";
                Files.writeString(target, content);
            }
        }
    }

    private class BeepCommand implements SimpleCommand {
        private final boolean isBig;
        public BeepCommand(boolean isBig) { this.isBig = isBig; }

        @Override
        public void execute(Invocation invocation) {
            String[] args = invocation.arguments();
            if (args.length == 0) return;

            Component sender = (invocation.source() instanceof Player p)
                    ? Component.text(p.getUsername(), NamedTextColor.AQUA)
                    : Component.translatable("beep.console");

            if (args[0].equalsIgnoreCase("all")) {
                server.getAllPlayers().forEach(p -> playBeep(p, sender));
            } else if(args[0].equalsIgnoreCase("thisServer") && invocation.source() instanceof Player p) {
                p.getCurrentServer().ifPresent(serverConnection -> {
                    RegisteredServer server = serverConnection.getServer();
                    server.getPlayersConnected().forEach(player -> playBeep(player, sender));
                });
            } else {
                server.getPlayer(args[0]).ifPresentOrElse(
                        p -> playBeep(p, sender),
                        () -> invocation.source().sendMessage(Component.text("Player not found!", NamedTextColor.RED))
                );
            }
        }

        private void playBeep(Player target, Component sender) {
            Runnable sendAction = snedSoundPacket(target);
            if (isBig) {
                target.showTitle(Title.title(
                        Component.translatable("beep.title", sender),
                        Component.translatable("beep.subtitle"),
                        Title.Times.times(Duration.ofMillis(100), Duration.ofMillis(1000), Duration.ofMillis(200))
                ));

                server.getScheduler().buildTask(BeepVelocity.this, sendAction)
                        .delay(333, TimeUnit.MILLISECONDS).schedule();

                server.getScheduler().buildTask(BeepVelocity.this, sendAction)
                        .delay(666, TimeUnit.MILLISECONDS).schedule();
            }
        }

        @Override
        public List<String> suggest(Invocation invocation) {
            String[] args = invocation.arguments();
            if (args.length <= 1) {
                String prefix = args.length == 0 ? "" : args[0].toLowerCase();
                List<String> suggestions = server.getAllPlayers().stream()
                        .map(Player::getUsername)
                        .filter(name -> name.toLowerCase().startsWith(prefix))
                        .collect(Collectors.toList());
                if ("all".startsWith(prefix)) suggestions.add("all");
                if ("thisServer".startsWith(prefix)) suggestions.add("thisServer");
                return suggestions;
            }
            return List.of();
        }
    }

    private static @NonNull Runnable snedSoundPacket(Player target) {
        Runnable sendAction = () -> {
            User user = PacketEvents.getAPI().getPlayerManager().getUser(target);
            WrapperPlayServerEntitySoundEffect packet = new WrapperPlayServerEntitySoundEffect(
                    Sounds.ENTITY_ARROW_HIT_PLAYER,
                    SoundCategory.PLAYER,
                    user.getEntityId(),
                    1f,
                    1f
            );
            user.sendPacket(packet);
        };
        sendAction.run();
        return sendAction;
    }
}
