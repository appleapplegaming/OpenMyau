package myau.management;

import com.google.gson.*;
import myau.event.EventTarget;
import myau.events.KeyEvent;
import myau.util.ChatUtil;

import java.io.*;
import java.util.ArrayList;

public class KeyMessageManager {
    public final ArrayList<KeyMessageEntry> entries = new ArrayList<>();
    public final File file;

    public KeyMessageManager() {
        this.file = new File("./config/Myau/", "keymessages.json");
        try {
            file.getParentFile().mkdirs();
        } catch (Exception e) {
            // ignore
        }
    }

    @EventTarget
    public void onKey(KeyEvent event) {
        for (KeyMessageEntry entry : new ArrayList<>(entries)) {
            if (entry.key == event.getKey()) {
                ChatUtil.sendMessage(entry.message);
            }
        }
    }

    public void save() {
        try {
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            JsonArray array = new JsonArray();
            for (KeyMessageEntry entry : entries) {
                JsonObject obj = new JsonObject();
                obj.addProperty("key", entry.key);
                obj.addProperty("message", entry.message);
                array.add(obj);
            }
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
                writer.println(gson.toJson(array));
            }
        } catch (IOException e) {
            // ignore
        }
    }

    public void load() {
        if (!file.exists()) return;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            JsonElement parsed = new JsonParser().parse(reader);
            if (parsed != null && parsed.isJsonArray()) {
                entries.clear();
                for (JsonElement element : parsed.getAsJsonArray()) {
                    if (element.isJsonObject()) {
                        JsonObject obj = element.getAsJsonObject();
                        if (obj.has("key") && obj.has("message")) {
                            int key = obj.get("key").getAsInt();
                            String message = obj.get("message").getAsString();
                            entries.add(new KeyMessageEntry(key, message));
                        }
                    }
                }
            }
        } catch (Exception e) {
            // ignore
        }
    }
}
