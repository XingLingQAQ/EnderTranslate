package fr.supermax_8.endertranslate.core;

import de.themoep.minedown.adventure.MineDown;
import fr.supermax_8.endertranslate.core.player.TranslatePlayerManager;
import fr.supermax_8.endertranslate.core.translation.Translation;
import fr.supermax_8.endertranslate.core.translation.TranslationManager;
import fr.supermax_8.endertranslate.core.translation.TranslationValue;
import fr.supermax_8.endertranslate.core.utils.ComponentUtils;
import io.github.retrooper.packetevents.adventure.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.*;

public class Translator {

    private static final int MAX_TRANSLATION_DEPTH = 10;

    private static String getLanguage(UUID playerId) {
        return TranslatePlayerManager.getInstance().getPlayerLanguage(playerId);
    }

    public static String applyTranslatePlain(UUID playerId, String plaintext) {
        return applyTranslatePlain(getLanguage(playerId), plaintext);
    }

    /*public String applyTranslatePlain(String playerLanguage, String plainTextToTranslate) {
        StringBuilder sb = new StringBuilder(plainTextToTranslate);

        EnderTranslateConfig config = EnderTranslateConfig.getInstance();
        String startTag = config.getStartTag();
        String endTag = config.getEndTag();

        int startTagIndex = sb.indexOf(startTag);
        while (startTagIndex != -1) {
            int endTagIndex = sb.indexOf(endTag, startTagIndex);
            if (endTagIndex == -1) break;
            int startLangPlaceholderIndex = startTagIndex + startTag.length();

            String langPlaceholder = sb.substring(startLangPlaceholderIndex, endTagIndex);
            String endValue = translatePlaceholderPlain(langPlaceholder, playerLanguage);

            sb.replace(startTagIndex, endTagIndex + endTag.length(), endValue);
            startTagIndex = sb.indexOf(startTag);
        }

        String translatedMessage = sb.toString();
        return translatedMessage.equals(plainTextToTranslate) ? null : translatedMessage;
    }*/

    public static String applyTranslatePlain(String playerLanguage, String plainTextToTranslate) {
        EnderTranslateConfig config = EnderTranslateConfig.getInstance();
        String startTag = config.getStartTag();
        String endTag = config.getEndTag();

        if (plainTextToTranslate.equals(startTag) || plainTextToTranslate.equals(endTag)) {
            return null;
        }

        String translated = recursiveTranslate(plainTextToTranslate, playerLanguage);
        return translated.equals(plainTextToTranslate) ? null : translated;
    }

    private static String recursiveTranslate(String text, String playerLanguage) {
        return recursiveTranslate(text, playerLanguage, 0);
    }

    private static String recursiveTranslate(String text, String playerLanguage, int depth) {
        if (depth > MAX_TRANSLATION_DEPTH)
            return text;

        EnderTranslateConfig config = EnderTranslateConfig.getInstance();
        String startTag = config.getStartTag();
        String endTag = config.getEndTag();

        StringBuilder sb = new StringBuilder();
        int index = 0;

        while (index < text.length()) {
            int start = text.indexOf(startTag, index);
            if (start == -1) {
                sb.append(text.substring(index));
                break;
            }

            sb.append(text, index, start);

            int searchIndex = start + startTag.length();
            int openTags = 1;

            while (searchIndex < text.length()) {
                int nextStart = text.indexOf(startTag, searchIndex);
                int nextEnd = text.indexOf(endTag, searchIndex);

                if (nextEnd == -1) {
                    sb.append(text.substring(start));
                    return sb.toString();
                }

                if (nextStart != -1 && nextStart < nextEnd) {
                    openTags++;
                    searchIndex = nextStart + startTag.length();
                } else {
                    openTags--;
                    searchIndex = nextEnd + endTag.length();
                    if (openTags == 0) {
                        String inner = text.substring(start + startTag.length(), nextEnd);
                        String resolvedInner = recursiveTranslate(inner, playerLanguage, depth + 1);
                        String translated = translatePlaceholderPlain(resolvedInner, playerLanguage);
                        sb.append(translated);
                        index = nextEnd + endTag.length();
                        break;
                    }
                }
            }

            if (openTags > 0) {
                sb.append(text.substring(start));
                break;
            }
        }

        return sb.toString();
    }

    public static String unboxTranslationPlain(String playerLanguage, TranslationValue translationValue) {
        if (!translationValue.isContainsLangPlaceholder()) return translationValue.getValue();
        return applyTranslatePlain(playerLanguage, translationValue.getValue());
    }

    public static String applyTranslateJson(UUID playerId, String json) {
        return GsonComponentSerializer.gson().serialize(applyTranslateComponent(playerId, GsonComponentSerializer.gson().deserialize(json)));
    }

    public static Component applyTranslateComponent(UUID playerId, String json) {
        return applyTranslateComponent(playerId, GsonComponentSerializer.gson().deserialize(json));
    }

    public static Component applyTranslateComponent(UUID playerId, Component toTranslate) {
        return applyTranslateComponent(getLanguage(playerId), toTranslate);
    }

    public static Component applyTranslateComponent(String playerLanguage, Component toTranslate) {
        boolean modified = false;
        LinkedList<Component> components = ComponentUtils.componentSeparatedList(toTranslate);
        ListIterator<Component> itr = components.listIterator();

        while (itr.hasNext()) {
            Component component = itr.next();

            if (!(component instanceof TextComponent textComponent)) continue;

            HoverEvent hoverEvent = textComponent.hoverEvent();
            if (hoverEvent != null && hoverEvent.value() instanceof Component hoverText) {
                Component translatedHover = applyTranslateComponent(playerLanguage, hoverText);
                if (translatedHover != null)
                    textComponent = textComponent.hoverEvent(hoverEvent.value(translatedHover));
            }

            List<Component> resolvedParts = parseComponentRecursively(textComponent.content(), playerLanguage, textComponent);

            if (resolvedParts.size() == 1 && resolvedParts.get(0).equals(component)) continue;

            itr.remove();
            resolvedParts.forEach(itr::add);
            modified = true;
        }

        return modified ? ComponentUtils.mergeComponents(components) : null;
    }

    private static List<Component> parseComponentRecursively(String text, String language, TextComponent base) {
        return parseComponentRecursively(text, language, base, 0);
    }

    private static List<Component> parseComponentRecursively(String text, String language, TextComponent base, int depth) {
        EnderTranslateConfig config = EnderTranslateConfig.getInstance();
        String startTag = config.getStartTag();
        String endTag = config.getEndTag();

        List<Component> result = new ArrayList<>();
        if (depth > MAX_TRANSLATION_DEPTH)
            return result;
        int index = 0;

        while (index < text.length()) {
            int start = text.indexOf(startTag, index);
            if (start == -1) {
                result.add(base.content(text.substring(index)));
                break;
            }

            result.add(base.content(text.substring(index, start)));

            int search = start + startTag.length();
            int openTags = 1;

            while (search < text.length()) {
                int nextStart = text.indexOf(startTag, search);
                int nextEnd = text.indexOf(endTag, search);

                if (nextEnd == -1) {
                    result.add(base.content(text.substring(start)));
                    return result;
                }

                if (nextStart != -1 && nextStart < nextEnd) {
                    openTags++;
                    search = nextStart + startTag.length();
                } else {
                    openTags--;
                    search = nextEnd + endTag.length();
                    if (openTags == 0) {
                        String inner = text.substring(start + startTag.length(), nextEnd);
                        List<Component> resolvedInner = parseComponentRecursively(inner, language, base, depth + 1);
                        Component translated = translatePlaceholderComponent(
                                flattenComponent(resolvedInner),
                                language,
                                base.style()
                        );
                        result.add(translated);
                        index = nextEnd + endTag.length();
                        break;
                    }
                }
            }

            if (openTags > 0) {
                result.add(base.content(text.substring(start)));
                break;
            }
        }

        return result;
    }

    private static String flattenComponent(List<Component> parts) {
        StringBuilder sb = new StringBuilder();
        for (Component part : parts) {
            if (part instanceof TextComponent txt)
                sb.append(txt.content());
            else
                sb.append(part.toString()); // fallback
        }
        return sb.toString();
    }

    /**
     * Translate placeholder with params
     * @param langPlaceholder the placeholder e.g hello_chat{aaaa;bb}
     * @param playerLanguage the language used to translate
     * @return the translated text
     */
    public static String translatePlaceholderPlain(String langPlaceholder, String playerLanguage) {
        TranslationManager translationManager = TranslationManager.getInstance();
        // Load params
        int startParamIndex = langPlaceholder.indexOf("{");
        String[] params = null;
        if (startParamIndex != -1) {
            params = langPlaceholder.substring(startParamIndex + 1, langPlaceholder.length() - 1).split(";");
            langPlaceholder = langPlaceholder.substring(0, startParamIndex);
        }

        String endValue;

        getEndValue:
        {
            Translation translation = translationManager.getTranslation(langPlaceholder);
            TranslationValue translationValue;
            if (translation == null || (translationValue = translation.getTranslationValue(playerLanguage)) == null) {
                endValue = "TRANSLATION(id=" + langPlaceholder + ")_NOT_FOUND";
                break getEndValue;
            }
            String translationValueString = unboxTranslationPlain(playerLanguage, translationValue);
            if (params == null) endValue = translationValueString;
            else {
                StringBuilder translationValueBuilder = new StringBuilder(translationValueString);
                int i = 0;
                for (String param : params) {
                    int paramIndex = translationValueBuilder.indexOf("{" + i + "}");
                    if (paramIndex == -1) break;
                    Translation paramTranslation = translationManager.getTranslation(param);
                    String paramTranslationValueString;
                    if (paramTranslation == null) {
                        String translated = applyTranslatePlain(playerLanguage, param);
                        paramTranslationValueString = translated == null ? param : translated;
                    } else {
                        TranslationValue paramTranslationValue = paramTranslation.getTranslationValue(playerLanguage);
                        if (paramTranslationValue == null)
                            paramTranslationValueString = "<red>TRANSLATION(id=" + langPlaceholder + ")_NOT_FOUND</red>";
                        else paramTranslationValueString = unboxTranslationPlain(playerLanguage, paramTranslationValue);
                    }
                    translationValueBuilder.replace(paramIndex, paramIndex + 3, paramTranslationValueString);
                    i++;
                }
                endValue = translationValueBuilder.toString();
            }
        }

        return endValue;
    }

    public static Component translatePlaceholderComponent(String langPlaceholder, String playerLanguage) {
        return translatePlaceholderComponent(langPlaceholder, playerLanguage, null);
    }

    /**
     * Translate placeholder with params
     * @param langPlaceholder the placeholder e.g hello_chat{aaaa;bb}
     * @param playerLanguage
     * @return
     */
    public static Component translatePlaceholderComponent(String langPlaceholder, String playerLanguage, Style previousStyle) {
        TranslationManager translationManager = TranslationManager.getInstance();
        // Load params
        int startParamIndex = langPlaceholder.indexOf("{");
        String[] params = null;
        if (startParamIndex != -1) {
            params = langPlaceholder.substring(startParamIndex + 1, langPlaceholder.length() - 1).split(";");
            langPlaceholder = langPlaceholder.substring(0, startParamIndex);
        }

        Translation translation = translationManager.getTranslation(langPlaceholder);
        TranslationValue translationValue;
        if (translation == null || (translationValue = translation.getTranslationValue(playerLanguage)) == null)
            return Component.text("TRANSLATION(id=" + langPlaceholder + ")_NOT_FOUND").color(NamedTextColor.RED);
        String translationValueString = unboxTranslationPlain(playerLanguage, translationValue);
        if (params != null) {
            StringBuilder translationValueBuilder = new StringBuilder(translationValueString);
            int i = 0;
            for (String param : params) {
                int paramIndex = translationValueBuilder.indexOf("{" + i + "}");
                if (paramIndex == -1) break;
                Translation paramTranslation = translationManager.getTranslation(param);
                String paramTranslationValueString;
                if (paramTranslation == null) {
                    String translated = applyTranslatePlain(playerLanguage, param);
                    paramTranslationValueString = translated == null ? param : translated;
                } else {
                    TranslationValue paramTranslationValue = paramTranslation.getTranslationValue(playerLanguage);
                    if (paramTranslationValue == null)
                        paramTranslationValueString = "<red>TRANSLATION(id=" + langPlaceholder + ")_NOT_FOUND</red>";
                    else paramTranslationValueString = unboxTranslationPlain(playerLanguage, paramTranslationValue);
                }
                translationValueBuilder.replace(paramIndex, paramIndex + 3, paramTranslationValueString);
                i++;
            }
            translationValueString = translationValueBuilder.toString();
        }

        Component finalComp;
        if (translationValueString.contains("&"))
            finalComp = MineDown.parse(translationValueString);
        else if (translationValueString.contains("§"))
            finalComp = MineDown.parse(translationValueString.replace("§", "&"));
        else finalComp = MiniMessage.miniMessage().deserialize(translationValueString);

        if (previousStyle != null && !finalComp.hasStyling())
            return finalComp.style(previousStyle);
        return finalComp;
    }


}
