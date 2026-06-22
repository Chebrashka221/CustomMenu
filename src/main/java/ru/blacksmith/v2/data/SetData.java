package ru.blacksmith.v2.data;

import org.bukkit.configuration.file.YamlConfiguration;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SetData {

    private String id;
    private String name;
    private int requiredLevel;
    private int menuSlot;
    // Иконка сета в главном меню
    private String iconItemsAdderId;  // может быть null
    private String iconMaterial;      // запасной материал
    // Слиток
    private String ingotItemsAdderId;
    private String ingotName;
    // GUI сета
    private String menuTitle;
    private int menuRows;
    private String shopId;

    private final Map<String, ItemData> items = new LinkedHashMap<>();
    // slot → Material name (например "BLUE_STAINED_GLASS_PANE")
    private final Map<Integer, String> decorations = new LinkedHashMap<>();

    public SetData(String id) {
        this.id = id;
        this.name = id;
        this.requiredLevel = 0;
        this.menuSlot = 0;
        this.iconMaterial = "CHEST";
        this.ingotItemsAdderId = "";
        this.ingotName = "Слиток";
        this.menuTitle = "&8⚒ " + id;
        this.menuRows = 4;
        this.shopId = "default";
    }

    // ---- Serialization ----

    public void loadFrom(YamlConfiguration cfg, String path) {
        name             = cfg.getString(path + ".name", id);
        requiredLevel    = cfg.getInt(path + ".required-level", 0);
        menuSlot         = cfg.getInt(path + ".menu-slot", 0);
        iconItemsAdderId = cfg.getString(path + ".icon.itemsadder-id", null);
        iconMaterial     = cfg.getString(path + ".icon.material", "CHEST");
        ingotItemsAdderId= cfg.getString(path + ".ingot.itemsadder-id", "");
        ingotName        = cfg.getString(path + ".ingot.name", "Слиток");
        menuTitle        = cfg.getString(path + ".menu.title", "&8⚒ " + name);
        menuRows         = cfg.getInt(path + ".menu.rows", 4);
        shopId           = cfg.getString(path + ".shop-id", "default");

        decorations.clear();
        var decorSec = cfg.getConfigurationSection(path + ".decorations");
        if (decorSec != null) {
            for (String key : decorSec.getKeys(false)) {
                try {
                    String mat = decorSec.getString(key);
                    if (mat != null) decorations.put(Integer.parseInt(key), mat);
                } catch (NumberFormatException ignored) {}
            }
        }

        items.clear();
        var itemsSec = cfg.getConfigurationSection(path + ".items");
        if (itemsSec != null) {
            for (String itemId : itemsSec.getKeys(false)) {
                ItemData item = new ItemData(itemId);
                item.loadFrom(cfg, path + ".items." + itemId);
                items.put(itemId, item);
            }
        }
    }

    public void saveTo(YamlConfiguration cfg, String path) {
        cfg.set(path + ".name", name);
        cfg.set(path + ".required-level", requiredLevel);
        cfg.set(path + ".menu-slot", menuSlot);
        cfg.set(path + ".icon.itemsadder-id", iconItemsAdderId);
        cfg.set(path + ".icon.material", iconMaterial);
        cfg.set(path + ".ingot.itemsadder-id", ingotItemsAdderId);
        cfg.set(path + ".ingot.name", ingotName);
        cfg.set(path + ".menu.title", menuTitle);
        cfg.set(path + ".menu.rows", menuRows);
        cfg.set(path + ".shop-id", shopId);
        cfg.set(path + ".decorations", null);
        for (var e : decorations.entrySet()) {
            cfg.set(path + ".decorations." + e.getKey(), e.getValue());
        }
        for (ItemData item : items.values()) {
            item.saveTo(cfg, path + ".items." + item.getId());
        }
    }

    // ---- Getters / Setters ----

    public String getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getRequiredLevel() { return requiredLevel; }
    public void setRequiredLevel(int v) { this.requiredLevel = v; }
    public int getMenuSlot() { return menuSlot; }
    public void setMenuSlot(int v) { this.menuSlot = v; }
    public String getIconItemsAdderId() { return iconItemsAdderId; }
    public void setIconItemsAdderId(String v) { this.iconItemsAdderId = v; }
    public String getIconMaterial() { return iconMaterial; }
    public void setIconMaterial(String v) { this.iconMaterial = v; }
    public String getIngotItemsAdderId() { return ingotItemsAdderId; }
    public void setIngotItemsAdderId(String v) { this.ingotItemsAdderId = v; }
    public String getIngotName() { return ingotName; }
    public void setIngotName(String v) { this.ingotName = v; }
    public String getMenuTitle() { return menuTitle; }
    public void setMenuTitle(String v) { this.menuTitle = v; }
    public int getMenuRows() { return menuRows; }
    public void setMenuRows(int v) { this.menuRows = v; }
    public String getShopId() { return shopId == null ? "default" : shopId; }
    public void setShopId(String v) { this.shopId = v == null || v.isBlank() ? "default" : v; }
    public Map<String, ItemData> getItems() { return items; }
    public ItemData getItem(String id) { return items.get(id); }
    public void putItem(ItemData item) { items.put(item.getId(), item); }
    public void removeItem(String id) { items.remove(id); }

    public Map<Integer, String> getDecorations() { return decorations; }
    public void setDecoration(int slot, String material) { decorations.put(slot, material); }
    public void removeDecoration(int slot) { decorations.remove(slot); }
    public void clearDecorations() { decorations.clear(); }
}
