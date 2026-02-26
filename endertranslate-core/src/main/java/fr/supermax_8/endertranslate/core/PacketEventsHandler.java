package fr.supermax_8.endertranslate.core;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
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
import com.github.retrooper.packetevents.protocol.player.Equipment;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.protocol.score.FixedScoreFormat;
import com.github.retrooper.packetevents.protocol.score.ScoreFormat;
import com.github.retrooper.packetevents.wrapper.configuration.client.WrapperConfigClientSettings;
import com.github.retrooper.packetevents.wrapper.play.server.*;
import lombok.Getter;
import me.tofaa.entitylib.meta.EntityMeta;
import me.tofaa.entitylib.meta.Metadata;
import me.tofaa.entitylib.meta.display.TextDisplayMeta;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static com.github.retrooper.packetevents.protocol.packettype.PacketType.Play.Server.*;

public class PacketEventsHandler extends Translator {

    @Getter
    private static PacketEventsHandler instance;

    @Getter
    private final ConcurrentHashMap<UUID, ConcurrentHashMap<Integer, EntityData>> playerEntitiesData = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<PacketType.Play.Server, Consumer<PacketSendEvent>> handlers = new ConcurrentHashMap<>();

    public PacketEventsHandler() {
        instance = this;
        initPackets();
    }

    private static class EntityData {
        EntityType type;
        WrapperPlayServerEntityMetadata entityMetadata;
    }

    private Map<Integer, EntityData> getPlayerEntitiesData(UUID playerId) {
        return playerEntitiesData.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>());
    }

    private void initPackets() {
        if (PacketEvents.getAPI() == null) {
            EnderTranslate.log("Starting ET without PE API");
            return;
        }
        PacketEvents.getAPI().getEventManager().registerListener(new PacketListenerAbstract(PacketListenerPriority.HIGHEST) {
            @Override
            public void onPacketReceive(@NotNull PacketReceiveEvent e) {
                if (e.getPacketType() == PacketType.Configuration.Client.CLIENT_SETTINGS) {
                    WrapperConfigClientSettings packet = new WrapperConfigClientSettings(e);
                    EnderTranslate.log("Player local: " + packet.getLocale());
                }
            }

            public void onPacketSend(@NotNull PacketSendEvent e) {
                try {
                    handlePacket(e);
                } catch (Throwable ex) {
                    System.out.println("§cERRRRORORORORORORORORO TRANSLATION: ");
                    ex.printStackTrace();
                }
            }
        });

        rgHandler(SYSTEM_CHAT_MESSAGE, e -> {
            WrapperPlayServerSystemChatMessage packet = new WrapperPlayServerSystemChatMessage(e);
            applyTranslateOnPacketSend(e, packet::getMessage, packet::setMessage);
        });
        rgHandler(CHAT_MESSAGE, e -> {
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
        });
        rgHandler(ACTION_BAR, e -> {
            WrapperPlayServerActionBar packet = new WrapperPlayServerActionBar(e);
            applyTranslateOnPacketSend(e, packet::getActionBarText, packet::setActionBarText);
        });
        rgHandler(TITLE, e -> {
            WrapperPlayServerTitle packet = new WrapperPlayServerTitle(e);
            applyTranslateOnPacketSend(e, packet::getTitle, packet::setTitle);
            applyTranslateOnPacketSend(e, packet::getSubtitle, packet::setSubtitle);
        });
        rgHandler(SET_TITLE_TEXT, e -> {
            WrapperPlayServerSetTitleText packet = new WrapperPlayServerSetTitleText(e);
            applyTranslateOnPacketSend(e, packet::getTitle, packet::setTitle);
        });
        rgHandler(SET_TITLE_SUBTITLE, e -> {
            WrapperPlayServerSetTitleSubtitle packet = new WrapperPlayServerSetTitleSubtitle(e);
            applyTranslateOnPacketSend(e, packet::getSubtitle, packet::setSubtitle);
        });
        rgHandler(OPEN_WINDOW, e -> {
            WrapperPlayServerOpenWindow packet = new WrapperPlayServerOpenWindow(e);
            applyTranslateOnPacketSend(e, packet::getTitle, packet::setTitle);
        });
        rgHandler(SPAWN_ENTITY, e -> {
            WrapperPlayServerSpawnEntity packet = new WrapperPlayServerSpawnEntity(e);
            Map<Integer, EntityData> entityDataMap = getPlayerEntitiesData(e.getUser().getUUID());
            EntityData entityData = new EntityData();
            entityData.type = packet.getEntityType();
            entityDataMap.put(packet.getEntityId(), entityData);
        });
        rgHandler(DESTROY_ENTITIES, e -> {
            WrapperPlayServerDestroyEntities packet = new WrapperPlayServerDestroyEntities(e);
            Map<Integer, EntityData> entityDataMap = getPlayerEntitiesData(e.getUser().getUUID());
            for (int id : packet.getEntityIds())
                entityDataMap.remove(id);
        });
        rgHandler(ENTITY_METADATA, e -> {
            try {
                WrapperPlayServerEntityMetadata clone = new WrapperPlayServerEntityMetadata(e);
                WrapperPlayServerEntityMetadata packet = new WrapperPlayServerEntityMetadata(e);
                int entityId = packet.getEntityId();

                Map<Integer, EntityData> entityDataMap = getPlayerEntitiesData(e.getUser().getUUID());
                EntityData entityData = entityDataMap.get(entityId);
                if (entityData == null) return;
                EntityType entityType = entityData.type;

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
                    entityDataMap.computeIfAbsent(entityId, k -> new EntityData()).entityMetadata = clone;
                    e.setLastUsedWrapper(packet);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
        rgHandler(WINDOW_ITEMS, e -> {
            WrapperPlayServerWindowItems packet = new WrapperPlayServerWindowItems(e);
            applyTranslateOnItemStacks(e, packet.getItems());
            packet.getCarriedItem().ifPresent(itm -> applyTranslateOnItemStack(e, itm));
        });
        rgHandler(ENTITY_EQUIPMENT, e -> {
            WrapperPlayServerEntityEquipment packet = new WrapperPlayServerEntityEquipment(e);
            for (Equipment equipment : packet.getEquipment())
                applyTranslateOnItemStack(e, equipment.getItem());
        });
        rgHandler(SET_CURSOR_ITEM, e -> {
            WrapperPlayServerSetCursorItem packet = new WrapperPlayServerSetCursorItem(e);
            applyTranslateOnItemStack(e, packet.getStack());
        });
        rgHandler(SET_PLAYER_INVENTORY, e -> {
            WrapperPlayServerSetPlayerInventory packet = new WrapperPlayServerSetPlayerInventory(e);
            applyTranslateOnItemStack(e, packet.getStack());
        });
        rgHandler(SET_SLOT, e -> {
            WrapperPlayServerSetSlot packet = new WrapperPlayServerSetSlot(e);
            applyTranslateOnItemStack(e, packet.getItem());
        });
        rgHandler(DISCONNECT, e -> {
            WrapperPlayServerDisconnect packet = new WrapperPlayServerDisconnect(e);
            applyTranslateOnPacketSend(e, packet::getReason, packet::setReason);
        });
        rgHandler(BOSS_BAR, e -> {
            WrapperPlayServerBossBar packet = new WrapperPlayServerBossBar(e);
            if (packet.getAction() == WrapperPlayServerBossBar.Action.UPDATE_TITLE) {
                applyTranslateOnPacketSend(e, packet::getTitle, packet::setTitle);
            }
        });
        rgHandler(PLAYER_LIST_HEADER_AND_FOOTER, e -> {
            WrapperPlayServerPlayerListHeaderAndFooter packet = new WrapperPlayServerPlayerListHeaderAndFooter(e);
            applyTranslateOnPacketSend(e, packet::getHeader, packet::setHeader);
            applyTranslateOnPacketSend(e, packet::getFooter, packet::setFooter);
        });
        rgHandler(DISPLAY_SCOREBOARD, e -> {
            WrapperPlayServerDisplayScoreboard packet = new WrapperPlayServerDisplayScoreboard(e);
            applyTranslateOnPacketSendString(e, packet::getScoreName, packet::setScoreName);
        });
        rgHandler(SCOREBOARD_OBJECTIVE, e -> {
            WrapperPlayServerScoreboardObjective packet = new WrapperPlayServerScoreboardObjective(e);
            applyTranslateOnPacketSendString(e, packet::getName, packet::setName);
            applyTranslateOnPacketSend(e, packet::getDisplayName, packet::setDisplayName);
            applyTranslateOnScoreboardFormat(e, packet.getScoreFormat(), packet::setScoreFormat);
        });
        rgHandler(RESET_SCORE, e -> {
            WrapperPlayServerResetScore packet = new WrapperPlayServerResetScore(e);
            applyTranslateOnPacketSendString(e, packet::getObjective, packet::setObjective);
            applyTranslateOnPacketSendString(e, packet::getTargetName, packet::setTargetName);
        });
        rgHandler(UPDATE_SCORE, e -> {
            WrapperPlayServerUpdateScore packet = new WrapperPlayServerUpdateScore(e);
            applyTranslateOnPacketSendString(e, packet::getObjectiveName, packet::setObjectiveName);
            applyTranslateOnPacketSendString(e, packet::getEntityName, packet::setEntityName);
            applyTranslateOnScoreboardFormat(e, packet.getScoreFormat(), packet::setScoreFormat);
            applyTranslateOnPacketSend(e, packet::getEntityDisplayName, packet::setEntityDisplayName);
        });
        rgHandler(TEAMS, e -> {
            WrapperPlayServerTeams packet = new WrapperPlayServerTeams(e);
            applyTranslateOnPacketSendString(e, packet::getTeamName, packet::setTeamName);
            packet.getTeamInfo().ifPresent(scoreBoardTeamInfo -> {
                applyTranslateOnPacketSend(e, scoreBoardTeamInfo::getDisplayName, scoreBoardTeamInfo::setDisplayName);
                applyTranslateOnPacketSend(e, scoreBoardTeamInfo::getPrefix, scoreBoardTeamInfo::setPrefix);
                applyTranslateOnPacketSend(e, scoreBoardTeamInfo::getSuffix, scoreBoardTeamInfo::setSuffix);
            });
        });
        rgHandler(UPDATE_ADVANCEMENTS, e -> {
            WrapperPlayServerUpdateAdvancements packet = new WrapperPlayServerUpdateAdvancements(e);
            for (WrapperPlayServerUpdateAdvancements.Advancement advancement : packet.getAdvancements()) {
                WrapperPlayServerUpdateAdvancements.AdvancementDisplay advancementDisplay = advancement.getDisplay();
                applyTranslateOnPacketSend(e, advancementDisplay::getDescription, advancementDisplay::setDescription);
                applyTranslateOnPacketSend(e, advancementDisplay::getTitle, advancementDisplay::setTitle);
            }
        });
        rgHandler(SERVER_DATA, e -> {
            WrapperPlayServerServerData packet = new WrapperPlayServerServerData(e);
            applyTranslateOnPacketSend(e, packet::getMOTD, packet::setMOTD);
        });

        // Cancel handlers features
        EnderTranslateConfig.getInstance().getCancelHandlers().forEach(s -> {
            EnderTranslate.log("cancel handler §c" + s);
            handlers.keySet().removeIf(type -> type.name().equalsIgnoreCase(s));
        });
    }

    private void rgHandler(PacketType.Play.Server type, Consumer<PacketSendEvent> handler) {
        handlers.put(type, handler);
    }

    private void handlePacket(PacketSendEvent e) {
        PacketTypeCommon packetType = e.getPacketType();
        if (!(packetType instanceof PacketType.Play.Server type)) return;
        Consumer<PacketSendEvent> handler = handlers.get(type);
        if (handler != null) handler.accept(e);
    }

    public void resendEntityMetaPackets(Object playerObj) {
        User user = PacketEvents.getAPI().getPlayerManager().getUser(playerObj);
        Map<Integer, EntityData> entityDataMap = getPlayerEntitiesData(user.getUUID());
        for (EntityData entityData : entityDataMap.values())
            if (entityData.entityMetadata != null)
                user.sendPacket(entityData.entityMetadata);
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
                String translated = applyTranslateJson(playerId, page.getValue());
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
                String translated = applyTranslateJson(playerId, name);
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
                    String translated = applyTranslateJson(playerId, lineValue);
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
                Component translate = applyTranslateComponent(playerId, line);
                if (translate != null) {
                    linesItr.set(translate);
                    modified.set(true);
                }
            }
        });

        stack.getComponent(ComponentTypes.ITEM_NAME).ifPresent(name -> {
            Component translate = applyTranslateComponent(playerId, name);
            if (translate != null) {
                stack.setComponent(ComponentTypes.ITEM_NAME, translate);
                modified.set(true);
            }
        });
        stack.getComponent(ComponentTypes.CUSTOM_NAME).ifPresent(name -> {
            Component translate = applyTranslateComponent(playerId, name);
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
        String translated = applyTranslateJson(e.getUser().getUUID(), getMessage.get());
        if (translated != null) {
            setMessage.accept(translated);
            e.markForReEncode(true);
        }
    }

    public boolean applyTranslateOnPacketSend(PacketSendEvent e, Supplier<Component> getMessage, Consumer<Component> setMessage) {
        Component toTranslate = getMessage.get();
        if (toTranslate == null) return false;
        Component translated = applyTranslateComponent(e.getUser().getUUID(), toTranslate);
        if (translated != null) {
            setMessage.accept(translated);
            e.markForReEncode(true);
            return true;
        }
        return false;
    }

}