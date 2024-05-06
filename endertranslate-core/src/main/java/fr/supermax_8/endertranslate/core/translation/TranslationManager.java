package fr.supermax_8.endertranslate.core.translation;

import com.google.gson.Gson;
import fr.supermax_8.endertranslate.core.EnderTranslate;
import fr.supermax_8.endertranslate.core.language.LanguageManager;
import lombok.Getter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TranslationManager {

    @Getter
    private static TranslationManager instance;

    @Getter
    private final ConcurrentHashMap<String, Translation> translations = new ConcurrentHashMap<>();
    private final File translationFolder;

    public TranslationManager(File translationFolder) {
        this.translationFolder = translationFolder;
        instance = this;
        EnderTranslate.log("Loading translations...");
        try {
            Files.walk(translationFolder.toPath()).filter(p -> p.toFile().getName().endsWith(".json")).forEach(path -> {
                try {
                    load(path.toFile());
                    EnderTranslate.log("Loading translation file: " + path.toFile().getName());
                } catch (Exception e) {
                    EnderTranslate.log("§cCan't load file " + path);
                    e.printStackTrace();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
        EnderTranslate.log(translations.size() + " translation loaded");
        getAllFilesPaths();
    }

    private void load(File file) throws FileNotFoundException {
        TranslationFile translationFile = new Gson().fromJson(new FileReader(file), TranslationFile.class);
        translationFile.entries.forEach((entry) -> {
            // Fill the map with all languages
            HashMap<String, String> translationsMap = new LinkedHashMap<>();
            for (String lId : LanguageManager.getInstance().getLanguages()) translationsMap.put(lId, null);

            translationsMap.putAll(entry.values);

            // Get the translation and add them in the same order as the languages config
            ArrayList<String> translations = new ArrayList<>();
            for (String lId : LanguageManager.getInstance().getLanguages())
                translations.add(translationsMap.get(lId));

            Translation translation = new Translation(translations);
            this.translations.put(entry.id, translation);
        });
    }

    public Translation getTranslation(String placeholder) {
        return translations.get(placeholder);
    }

    public List<String> getAllFilesPaths() {
        List<String> paths = new ArrayList<>();
        try {
            Files.walk(translationFolder.toPath()).filter(p -> p.toFile().getName().endsWith(".json") || p.toFile().isDirectory() && !p.equals(translationFolder.toPath())).forEach(path -> {
                String absolutePath = path.toFile().getAbsolutePath();
                try {
                    String cleanPath = absolutePath.substring(absolutePath.indexOf("translations") + "translations/".length());
                    paths.add(cleanPath);
                } catch (Exception e) {
                    System.out.println(absolutePath);
                    System.out.println(absolutePath.indexOf("translations"));
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
        return paths;
    }

    public TranslationFile getTranslationFileFromRelativePath(String pageRelativePath) throws FileNotFoundException {
        File f = new File(translationFolder, pageRelativePath);
        if (f.exists())
            return EnderTranslate.getGson().fromJson(new FileReader(f), TranslationFile.class);
        f.getParentFile().mkdirs();
        TranslationFile tf = new TranslationFile(List.of());
        String json = EnderTranslate.getGson().toJson(tf);
        try (FileWriter writer = new FileWriter(f)) {
            writer.write(json);
        } catch (Exception e) {
        }
        return tf;
    }

    public void saveFile(String relativePath, String data) {
        File f = new File(translationFolder, relativePath);
        if (!f.getParentFile().exists()) f.getParentFile().mkdirs();

        try (FileWriter writer = new FileWriter(f)) {
            writer.write(data);
        } catch (Exception e) {
        }
    }

    public void renameFile(String path, String newPath) {
        File f = new File(translationFolder, path);
        f.renameTo(new File(translationFolder, newPath));
    }

    public void moveFile(String fromPath, String toPath) {
        File fromPathFile = new File(translationFolder, fromPath);
        File toPathFile = new File(translationFolder, toPath);
        try {
            Files.move(fromPathFile.toPath(), toPathFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void deleteFile(String relativePath) {
        File f = new File(translationFolder, relativePath);
        f.delete();
    }

    public void createFile(String relativePath) {
        File f = new File(translationFolder, relativePath);
        if (f.getName().endsWith(".json")) {
            try (FileWriter writer = new FileWriter(f)) {
                writer.write(EnderTranslate.getGson().toJson(new TranslationFile(List.of())));
            } catch (Exception e) {
            }
        } else f.mkdirs();
    }

    public static class TranslationFile {
        private final List<TranslationEntry> entries;

        public TranslationFile(List<TranslationEntry> entries) {
            this.entries = entries;
        }


        public static class TranslationEntry {
            private String id;
            private Map<String, String> values;

            public TranslationEntry(String id, Map<String, String> values) {
                this.id = id;
                this.values = values;
            }
        }
    }

}