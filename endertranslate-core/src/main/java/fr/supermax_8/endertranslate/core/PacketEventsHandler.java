package fr.supermax_8.endertranslate.core;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.chat.message.ChatMessage;
import com.github.retrooper.packetevents.protocol.chat.message.ChatMessage_v1_19;
import com.github.retrooper.packetevents.protocol.chat.message.ChatMessage_v1_19_1;
import com.github.retrooper.packetevents.protocol.chat.message.ChatMessage_v1_19_3;
import com.github.retrooper.packetevents.protocol.component.ComponentTypes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.nbt.*;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.protocol.score.FixedScoreFormat;
import com.github.retrooper.packetevents.protocol.score.ScoreFormat;
import com.github.retrooper.packetevents.util.adventure.AdventureSerializer;
import com.github.retrooper.packetevents.wrapper.configuration.client.WrapperConfigClientSettings;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientUpdateSign;
import com.github.retrooper.packetevents.wrapper.play.server.*;
import de.themoep.minedown.adventure.MineDown;
import fr.supermax_8.endertranslate.core.player.TranslatePlayerManager;
import fr.supermax_8.endertranslate.core.translation.Translation;
import fr.supermax_8.endertranslate.core.translation.TranslationManager;
import fr.supermax_8.endertranslate.core.utils.ComponentUtils;
import lombok.Getter;
import me.tofaa.entitylib.meta.EntityMeta;
import me.tofaa.entitylib.meta.Metadata;
import me.tofaa.entitylib.meta.display.TextDisplayMeta;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class PacketEventsHandler {

    @Getter
    private static PacketEventsHandler instance;

    private final ConcurrentHashMap<Integer, EntityType> entitiesType = new ConcurrentHashMap<>();
    @Getter
    private final ConcurrentHashMap<UUID, ConcurrentHashMap<Integer, WrapperPlayServerEntityMetadata>> entitiesMetaData = new ConcurrentHashMap<>();

    public PacketEventsHandler() {
        instance = this;
        initPackets();
    }

    private void initPackets() {
        PacketEvents.getAPI().getEventManager().registerListener(new PacketListenerAbstract() {
            @Override
            public void onPacketReceive(PacketReceiveEvent e) {
                if (e.getPacketType() == PacketType.Configuration.Client.CLIENT_SETTINGS) {
                    WrapperConfigClientSettings packet = new WrapperConfigClientSettings(e);
                    EnderTranslate.log("Player local: " + packet.getLocale());
                }
            }

            public void onPacketSend(PacketSendEvent e) {
                try {
                    handlePacket(e);
                } catch (Throwable ex) {
                    System.out.println("§cERRRRORORORORORORORORO TRANSLATION: ");
                    ex.printStackTrace();
                }
            }
        });
    }

    private void handlePacket(PacketSendEvent e) {
        PacketTypeCommon packetType = e.getPacketType();
        if (!(packetType instanceof PacketType.Play.Server type)) return;
        switch (type) {
            case SYSTEM_CHAT_MESSAGE -> {
                WrapperPlayServerSystemChatMessage packet = new WrapperPlayServerSystemChatMessage(e);
                applyTranslateOnPacketSend(e, packet::getMessage, packet::setMessage);
            }
            case CHAT_MESSAGE -> {
                WrapperPlayServerChatMessage packet = new WrapperPlayServerChatMessage(e);
                ChatMessage message = packet.getMessage();
                if (message instanceof ChatMessage_v1_19_3 cm) {
                    Optional<Component> unsigned = cm.getUnsignedChatContent();
                    if (unsigned.isEmpty())
                        applyTranslateOnPacketSend(e, message::getChatContent, message::setChatContent);
                    else applyTranslateOnPacketSend(e, unsigned::get, cm::setUnsignedChatContent);
                } else if (message instanceof ChatMessage_v1_19_1 cm) {
                    Component unsigned = cm.getUnsignedChatContent();
                    if (unsigned == null)
                        applyTranslateOnPacketSend(e, message::getChatContent, message::setChatContent);
                    applyTranslateOnPacketSend(e, cm::getUnsignedChatContent, cm::setUnsignedChatContent);
                } else if (message instanceof ChatMessage_v1_19 cm) {
                    Component unsigned = cm.getUnsignedChatContent();
                    if (unsigned == null)
                        applyTranslateOnPacketSend(e, message::getChatContent, message::setChatContent);
                    applyTranslateOnPacketSend(e, cm::getUnsignedChatContent, cm::setUnsignedChatContent);
                } else
                    applyTranslateOnPacketSend(e, message::getChatContent, message::setChatContent);
            }
            case ACTION_BAR -> {
                WrapperPlayServerActionBar packet = new WrapperPlayServerActionBar(e);
                applyTranslateOnPacketSend(e, packet::getActionBarText, packet::setActionBarText);
            }
            case TITLE -> {
                WrapperPlayServerTitle packet = new WrapperPlayServerTitle(e);
                applyTranslateOnPacketSend(e, packet::getTitle, packet::setTitle);
                applyTranslateOnPacketSend(e, packet::getSubtitle, packet::setSubtitle);
            }
            case SET_TITLE_TEXT -> {
                WrapperPlayServerSetTitleText packet = new WrapperPlayServerSetTitleText(e);
                applyTranslateOnPacketSend(e, packet::getTitle, packet::setTitle);
            }
            case SET_TITLE_SUBTITLE -> {
                WrapperPlayServerSetTitleSubtitle packet = new WrapperPlayServerSetTitleSubtitle(e);
                applyTranslateOnPacketSend(e, packet::getSubtitle, packet::setSubtitle);
            }
            case OPEN_WINDOW -> {
                WrapperPlayServerOpenWindow packet = new WrapperPlayServerOpenWindow(e);
                applyTranslateOnPacketSend(e, packet::getTitle, packet::setTitle);
            }
            case SPAWN_ENTITY -> {
                WrapperPlayServerSpawnEntity packet = new WrapperPlayServerSpawnEntity(e);
                entitiesType.put(packet.getEntityId(), packet.getEntityType());
            }
            case DESTROY_ENTITIES -> {
                WrapperPlayServerDestroyEntities packet = new WrapperPlayServerDestroyEntities(e);
                ConcurrentHashMap<Integer, WrapperPlayServerEntityMetadata> entityDataSent = entitiesMetaData.computeIfAbsent(e.getUser().getUUID(), k -> new ConcurrentHashMap<>());
                for (int id : packet.getEntityIds()) {
                    entitiesType.remove(id);
                    entityDataSent.remove(id);
                }
            }
            case ENTITY_METADATA -> {
                try {
                    WrapperPlayServerEntityMetadata clone = new WrapperPlayServerEntityMetadata(e);
                    WrapperPlayServerEntityMetadata packet = new WrapperPlayServerEntityMetadata(e);
                    int entityId = packet.getEntityId();
                    EntityType entityType = entitiesType.get(entityId);

                    Metadata meta = new Metadata(entityId);
                    meta.setMetaFromPacket(packet);
                    boolean translated = false;
                    if (entityType == EntityTypes.TEXT_DISPLAY) {
                        TextDisplayMeta textDisplayMeta = new TextDisplayMeta(entityId, meta);
                        translated = applyTranslateOnPacketSend(e, textDisplayMeta::getText, comp -> {
                            textDisplayMeta.setText(comp);
                            packet.setEntityMetadata(textDisplayMeta.createPacket().getEntityMetadata());
                        });
                    } else {
                        EntityMeta entityMeta = new EntityMeta(entityId, meta);
                        TextComponent name = (TextComponent) entityMeta.getCustomName();
                        if (name != null) {
                            translated = applyTranslateOnPacketSend(e, () -> name, comp -> {
                                entityMeta.setCustomName(comp);
                                packet.setEntityMetadata(entityMeta.createPacket().getEntityMetadata());
                            });
                        }
                    }
                    if (translated) {
                        entitiesMetaData.computeIfAbsent(e.getUser().getUUID(), k -> new ConcurrentHashMap<>()).put(entityId, clone);
                        e.setLastUsedWrapper(packet);
                    }
                } catch (Exception ex) {
                }
            }
            case WINDOW_ITEMS -> {
                WrapperPlayServerWindowItems packet = new WrapperPlayServerWindowItems(e);
                applyTranslateOnItemStacks(e, packet.getItems());
                packet.getCarriedItem().ifPresent(itm -> applyTranslateOnItemStack(e, itm));
            }
            case SET_SLOT -> {
                WrapperPlayServerSetSlot packet = new WrapperPlayServerSetSlot(e);
                applyTranslateOnItemStack(e, packet.getItem());
            }
            case DISCONNECT -> {
                WrapperPlayServerDisconnect packet = new WrapperPlayServerDisconnect(e);
                applyTranslateOnPacketSend(e, packet::getReason, packet::setReason);
            }
            case BOSS_BAR -> {
                WrapperPlayServerBossBar packet = new WrapperPlayServerBossBar(e);
                if (packet.getAction() == WrapperPlayServerBossBar.Action.UPDATE_TITLE) {
                    applyTranslateOnPacketSend(e, packet::getTitle, packet::setTitle);
                }
            }
            case PLAYER_LIST_HEADER_AND_FOOTER -> {
                WrapperPlayServerPlayerListHeaderAndFooter packet = new WrapperPlayServerPlayerListHeaderAndFooter(e);
                applyTranslateOnPacketSend(e, packet::getHeader, packet::setHeader);
                applyTranslateOnPacketSend(e, packet::getFooter, packet::setFooter);
            }
            case DISPLAY_SCOREBOARD -> {
                WrapperPlayServerDisplayScoreboard packet = new WrapperPlayServerDisplayScoreboard(e);
                applyTranslateOnPacketSendString(e, packet::getScoreName, packet::setScoreName);
            }
            case SCOREBOARD_OBJECTIVE -> {
                WrapperPlayServerScoreboardObjective packet = new WrapperPlayServerScoreboardObjective(e);
                applyTranslateOnPacketSendString(e, packet::getName, packet::setName);
                applyTranslateOnPacketSend(e, packet::getDisplayName, packet::setDisplayName);
                applyTranslateOnScoreboardFormat(e, packet.getScoreFormat(), packet::setScoreFormat);
            }
            case RESET_SCORE -> {
                WrapperPlayServerResetScore packet = new WrapperPlayServerResetScore(e);
                applyTranslateOnPacketSendString(e, packet::getObjective, packet::setObjective);
                applyTranslateOnPacketSendString(e, packet::getTargetName, packet::setTargetName);
            }
            case UPDATE_SCORE -> {
                WrapperPlayServerUpdateScore packet = new WrapperPlayServerUpdateScore(e);
                applyTranslateOnPacketSendString(e, packet::getObjectiveName, packet::setObjectiveName);
                applyTranslateOnPacketSendString(e, packet::getEntityName, packet::setEntityName);
                applyTranslateOnScoreboardFormat(e, packet.getScoreFormat(), packet::setScoreFormat);
                applyTranslateOnPacketSend(e, packet::getEntityDisplayName, packet::setEntityDisplayName);
            }
            case TEAMS -> {
                WrapperPlayServerTeams packet = new WrapperPlayServerTeams(e);
                applyTranslateOnPacketSendString(e, packet::getTeamName, packet::setTeamName);
                packet.getTeamInfo().ifPresent(scoreBoardTeamInfo -> {
                    applyTranslateOnPacketSend(e, scoreBoardTeamInfo::getDisplayName, scoreBoardTeamInfo::setDisplayName);
                    applyTranslateOnPacketSend(e, scoreBoardTeamInfo::getPrefix, scoreBoardTeamInfo::setPrefix);
                    applyTranslateOnPacketSend(e, scoreBoardTeamInfo::getSuffix, scoreBoardTeamInfo::setSuffix);
                });
            }
        }
    }

    public void resendEntityMetaPackets(Object playerObj) {
        User user = PacketEvents.getAPI().getPlayerManager().getUser(playerObj);
        ConcurrentHashMap<Integer, WrapperPlayServerEntityMetadata> entityMetadataSent = entitiesMetaData.computeIfAbsent(user.getUUID(), k -> new ConcurrentHashMap<>());
        entityMetadataSent.values().forEach(user::sendPacket);
    }

    public void applyTranslateOnScoreboardFormat(PacketSendEvent e, ScoreFormat format, Consumer<ScoreFormat> setFormat) {
        if (!(format instanceof FixedScoreFormat fixedScoreFormat)) return;
        applyTranslateOnPacketSend(e, fixedScoreFormat::getValue, component ->
                setFormat.accept(new FixedScoreFormat(component)));
    }

    public void applyTranslateOnItemStacks(PacketSendEvent e, Collection<ItemStack> stacks) {
        for (ItemStack stack : stacks)
            applyTranslateOnItemStack(e, stack);
    }

    public void applyTranslateOnNBT(PacketSendEvent e, ItemStack stack) {
        boolean modified = false;
        NBTCompound nbt = stack.getNBT();
        if (nbt == null) return;
        Map<String, NBT> tags = nbt.getTags();

        UUID playerId = e.getUser().getUUID();

        // Translate book pages for signed books
        if (tags.containsKey("pages")) {
            NBTList<NBTString> newPages = null;
            NBTList<NBTString> pages = (NBTList<NBTString>) tags.get("pages");
            for (NBTString page : pages.getTags()) {
                String translated = applyTranslate(playerId, page.getValue());
                if (translated == null) continue;
                if (newPages == null) newPages = new NBTList<>(NBTType.STRING);
                newPages.addTag(new NBTString(translated));
            }
            if (newPages != null) nbt.setTag("pages", newPages);
            modified = true;
        }

        NBTCompound display = (NBTCompound) tags.get("display");
        if (display != null) {
            // Translate name
            Map<String, NBT> displayTags = display.getTags();
            if (displayTags.containsKey("Name")) {
                String name = ((NBTString) displayTags.get("Name")).getValue();
                String translated = applyTranslate(playerId, name);
                if (translated != null) {
                    modified = true;
                    display.setTag("Name", new NBTString(translated));
                }
            }

            // Translate lore
            if (displayTags.containsKey("Lore")) {
                NBTList<NBTString> lore = (NBTList<NBTString>) displayTags.get("Lore");
                NBTList<NBTString> newLore = new NBTList<>(NBTType.STRING);
                boolean loreModified = false;
                for (NBTString line : lore.getTags()) {
                    String lineValue = line.getValue();
                    String translated = applyTranslate(playerId, lineValue);
                    if (translated == null)
                        newLore.addTag(line);
                    else {
                        newLore.addTag(new NBTString(translated));
                        loreModified = true;
                    }
                }
                if (loreModified) {
                    display.setTag("Lore", newLore);
                    modified = true;
                }
            }
        }

        if (modified) e.markForReEncode(true);
    }


    public void applyTranslateOnPatchableComponents(PacketSendEvent e, ItemStack stack) {
        UUID playerId = e.getUser().getUUID();
        AtomicBoolean modified = new AtomicBoolean(false);
        stack.getComponent(ComponentTypes.LORE).ifPresent(lore -> {
            ListIterator<Component> linesItr = lore.getLines().listIterator();
            while (linesItr.hasNext()) {
                Component line = linesItr.next();
                Component translate = applyTranslate(playerId, line);
                if (translate != null) {
                    linesItr.set(translate);
                    modified.set(true);
                }
            }
        });

        stack.getComponent(ComponentTypes.ITEM_NAME).ifPresent(name -> {
            Component translate = applyTranslate(playerId, name);
            if (translate != null) {
                stack.setComponent(ComponentTypes.ITEM_NAME, translate);
                modified.set(true);
            }
        });
        stack.getComponent(ComponentTypes.CUSTOM_NAME).ifPresent(name -> {
            Component translate = applyTranslate(playerId, name);
            if (translate != null) {
                stack.setComponent(ComponentTypes.CUSTOM_NAME, translate);
                modified.set(true);
            }
        });
        if (modified.get()) e.markForReEncode(true);
    }

    public void applyTranslateOnItemStack(PacketSendEvent e, ItemStack stack) {
        if (stack.getNBT() != null) applyTranslateOnNBT(e, stack);
        else applyTranslateOnPatchableComponents(e, stack);
    }

    public void applyTranslateOnPacketSendString(PacketSendEvent e, Supplier<String> getMessage, Consumer<String> setMessage) {
        String translated = applyTranslate(e.getUser().getUUID(), getMessage.get());
        if (translated != null) {
            setMessage.accept(translated);
            e.markForReEncode(true);
        }
    }

    public boolean applyTranslateOnPacketSend(PacketSendEvent e, Supplier<Component> getMessage, Consumer<Component> setMessage) {
        Component toTranslate = getMessage.get();
        if (toTranslate == null) return false;
        String message = AdventureSerializer.toJson(toTranslate);
        String translated = applyTranslate(e.getUser().getUUID(), message);
        if (translated != null) {
            setMessage.accept(AdventureSerializer.parseComponent(translated));
            e.markForReEncode(true);
            return true;
        }
        return false;
    }

    public String applyTranslate(UUID playerId, String message) {
        return AdventureSerializer.toJson(applyTranslate(playerId, AdventureSerializer.parseComponent(message)));
    }

    public Component applyTranslate(UUID playerId, Component toTranslate) {
        String playerLanguage = TranslatePlayerManager.getInstance().getPlayerLanguage(playerId);
        return applyTranslate(playerLanguage, toTranslate);
    }

    // bite [lang]a[/lang] ee

    // bite [lang]a[/lang] ee


    public Component applyTranslate(String playerLanguage, Component toTranslate) {
        boolean modified = false;
        // I make the component children thing simple, by making a simple list with all the component in order
        LinkedList<Component> components = ComponentUtils.componentSeparatedList(toTranslate);
        ListIterator<Component> itr = components.listIterator();

        // I iterate on the list and I will add more component / replace if I need
        while (itr.hasNext()) {
            Component component = itr.next();
            // It seems I can get some TranslatableComponent here so I need to check
            if (!(component instanceof TextComponent textComponent)) {
                /*System.out.println("NOT A TEXT COMP ????" + AdventureSerializer.toJson(component));*/
                continue;
            }
            StringBuilder sb = new StringBuilder(textComponent.content());

            EnderTranslateConfig config = EnderTranslateConfig.getInstance();
            String startTag = config.getStartTag();
            String endTag = config.getEndTag();

            int startTagIndex = sb.indexOf(startTag);
            int endTagIndex;
            int leftIndex = 0;
            if (startTagIndex != -1) {
                // Split the component in multiple parts to insert the translation component result
                LinkedList<Component> splits = new LinkedList<>();
                while (startTagIndex != -1) {
                    endTagIndex = sb.indexOf(endTag, startTagIndex);
                    if (endTagIndex == -1) break;
                    int startLangPlaceholderIndex = startTagIndex + startTag.length();
                    String langPlaceholder = sb.substring(startLangPlaceholderIndex, endTagIndex);
                    Component endValue = translatePlaceholder(langPlaceholder, playerLanguage);

                    // Add left side text
                    splits.add(textComponent.content(sb.substring(leftIndex, startTagIndex)));
                    // Add translation comp
                    splits.add(endValue);

                    leftIndex = endTagIndex + endTag.length();
                    startTagIndex = sb.indexOf(startTag, endTagIndex);
                    modified = true;
                }
                // Add the right part of the translation
                if (leftIndex <= sb.length())
                    splits.add(textComponent.content(sb.substring(leftIndex, sb.length())));
                itr.remove();
                for (Component c : splits)
                    itr.add(c);
            }
        }

        // I merge back the list to 1 component containing all the others text components
        return modified ? ComponentUtils.mergeComponents(components) : null;
    }

    /**
     * Translate placeholder with params
     * @param langPlaceholder the placeholder e.g hello_chat{aaaa;bb}
     * @param playerLanguage
     * @return
     */
    public Component translatePlaceholder(String langPlaceholder, String playerLanguage) {
        TranslationManager translationManager = TranslationManager.getInstance();

        // Load params
        int startParamIndex = langPlaceholder.indexOf("{");
        String[] params = null;
        if (startParamIndex != -1) {
            params = langPlaceholder.substring(startParamIndex + 1, langPlaceholder.length() - 1).split(";");
            /*System.out.println("PARAMS " + params);
            for (int i = 0; i < params.length; i++) {
                System.out.println("PAAAAARAM " + params[i]);
                String translate = applyTranslate(playerLanguage, params[i]);
                if (translate != null) params[i] = translate;
            }*/
            langPlaceholder = langPlaceholder.substring(0, startParamIndex);
        }

        Translation translation = translationManager.getTranslation(langPlaceholder);
        String translationValue;
        if (translation == null || (translationValue = translation.getTranslation(playerLanguage)) == null)
            return Component.text("TRANSLATION(id=" + langPlaceholder + ")_NOT_FOUND").color(NamedTextColor.RED);
        if (params != null) {
            StringBuilder translationValueBuilder = new StringBuilder(translationValue);
            int i = 0;
            for (String param : params) {
                int paramIndex = translationValueBuilder.indexOf("{" + i + "}");
                if (paramIndex == -1) break;
                Translation paramTranslation = translationManager.getTranslation(param);
                translationValueBuilder.replace(paramIndex, paramIndex + 3, paramTranslation == null ? param : paramTranslation.getTranslation(playerLanguage));
                i++;
            }
            translationValue = translationValueBuilder.toString();
        }

        if (translationValue.contains("&"))
            return MineDown.parse(translationValue);
        if (translationValue.contains("§"))
            return MineDown.parse(translationValue.replaceAll("§", "&"));
        return MiniMessage.miniMessage().deserialize(translationValue);
    }

}