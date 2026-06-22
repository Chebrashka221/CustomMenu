package ru.blacksmith.v2.data;

import org.bukkit.configuration.file.YamlConfiguration;

import java.util.ArrayList;
import java.util.List;

public class ItemData {

    private final String id;
    private String name;
    private double price;
    private int ingots;
    private String itemsAdderId;
    private int slot;
    private String requiresPrev; // "set.item" или null
    private int maxBuys    = 1; // 0 = безлимит, 1+ = конкретный лимит
    private int resetHours = 0; // 0 = нет сброса, N = сброс каждые N часов
    private String material  = null; // ванильный материал (альтернатива IA)
    private int amount       = 1;
    private List<String> description = new ArrayList<>(); // описание товара в лоре меню
    private List<String> commands = new ArrayList<>(); // {player} = ник
    private int pointsPrice  = 0; // цена в PlayerPoints (0 = не используется)
    private int priceIncreasePercent = 0;     // % прироста цены за покупку (0 = отключено)
    private double priceIncreaseFixed = 0;    // фикс. сумма прироста за покупку (0 = отключено)
    private String priceIncreaseType = "percent"; // "percent" или "fixed"
    private String priceIncreaseMode = "player"; // "player" — только у этого игрока, "global" — для всех
    private String executableItemsId = null;  // ID предмета в ExecutableItems (приоритет над ItemsAdder)

    public ItemData(String id) {
        this.id = id;
        this.name = id;
        this.price = 100;
        this.ingots = 1;
        this.itemsAdderId = "";
        this.slot = 0;
        this.requiresPrev = null;
    }

    public void loadFrom(YamlConfiguration cfg, String path) {
        name         = cfg.getString(path + ".name", id);
        price        = cfg.getDouble(path + ".price", 100);
        ingots       = cfg.getInt(path + ".ingots", 1);
        itemsAdderId = cfg.getString(path + ".itemsadder-id", "");
        slot         = cfg.getInt(path + ".slot", 0);
        String rp    = cfg.getString(path + ".requires-prev", null);
        requiresPrev = (rp == null || rp.equalsIgnoreCase("null") || rp.isEmpty()) ? null : rp;
        maxBuys      = cfg.getInt(path + ".max-buys", 1);
        resetHours   = cfg.getInt(path + ".reset-hours", 0);
        material     = cfg.getString(path + ".material", null);
        if (material != null && material.isBlank()) material = null;
        amount       = cfg.getInt(path + ".amount", 1);
        description           = cfg.getStringList(path + ".description");
        commands              = cfg.getStringList(path + ".commands");
        pointsPrice           = cfg.getInt(path + ".points-price", 0);
        priceIncreasePercent  = cfg.getInt(path + ".price-increase-percent", 0);
        priceIncreaseFixed    = cfg.getDouble(path + ".price-increase-fixed", 0);
        priceIncreaseType     = cfg.getString(path + ".price-increase-type", "percent");
        if (!"fixed".equals(priceIncreaseType)) priceIncreaseType = "percent";
        priceIncreaseMode     = cfg.getString(path + ".price-increase-mode", "player");
        if (!"global".equals(priceIncreaseMode)) priceIncreaseMode = "player";
        executableItemsId     = cfg.getString(path + ".executable-items-id", null);
        if (executableItemsId != null && executableItemsId.isBlank()) executableItemsId = null;
    }

    public void saveTo(YamlConfiguration cfg, String path) {
        cfg.set(path + ".name", name);
        cfg.set(path + ".price", price);
        cfg.set(path + ".ingots", ingots);
        cfg.set(path + ".itemsadder-id", itemsAdderId);
        cfg.set(path + ".slot", slot);
        cfg.set(path + ".requires-prev", requiresPrev);
        cfg.set(path + ".max-buys", maxBuys);
        cfg.set(path + ".reset-hours", resetHours);
        cfg.set(path + ".material", material);
        cfg.set(path + ".amount", amount);
        cfg.set(path + ".description", description.isEmpty() ? null : description);
        cfg.set(path + ".commands", commands.isEmpty() ? null : commands);
        cfg.set(path + ".points-price", pointsPrice > 0 ? pointsPrice : null);
        boolean hasIncrease = priceIncreasePercent > 0 || priceIncreaseFixed > 0;
        cfg.set(path + ".price-increase-percent", ("percent".equals(priceIncreaseType) && priceIncreasePercent > 0) ? priceIncreasePercent : null);
        cfg.set(path + ".price-increase-fixed", ("fixed".equals(priceIncreaseType) && priceIncreaseFixed > 0) ? priceIncreaseFixed : null);
        cfg.set(path + ".price-increase-type", hasIncrease ? priceIncreaseType : null);
        cfg.set(path + ".price-increase-mode", hasIncrease ? priceIncreaseMode : null);
        cfg.set(path + ".executable-items-id", executableItemsId);
    }

    // ---- Getters / Setters ----

    public String getId() { return id; }
    public String getName() { return name; }
    public void setName(String v) { this.name = v; }
    public double getPrice() { return price; }
    public void setPrice(double v) { this.price = v; }
    public int getIngots() { return ingots; }
    public void setIngots(int v) { this.ingots = v; }
    public String getItemsAdderId() { return itemsAdderId; }
    public void setItemsAdderId(String v) { this.itemsAdderId = v; }
    public int getSlot() { return slot; }
    public void setSlot(int v) { this.slot = v; }
    public String getRequiresPrev() { return requiresPrev; }
    public void setRequiresPrev(String v) { this.requiresPrev = v; }
    public boolean hasRequirement() { return requiresPrev != null; }
    public int getMaxBuys() { return maxBuys; }
    public void setMaxBuys(int v) { this.maxBuys = v; }
    public int getResetHours() { return resetHours; }
    public void setResetHours(int v) { this.resetHours = v; }
    public String getMaterial() { return material; }
    public void setMaterial(String v) { this.material = (v == null || v.isBlank()) ? null : v.toUpperCase(); }
    public int getAmount() { return amount; }
    public void setAmount(int v) { this.amount = Math.max(1, v); }
    public List<String> getDescription() { return description; }
    public void addDescriptionLine(String line) { description.add(line); }
    public void clearDescription() { description.clear(); }
    public void removeLastDescriptionLine() { if (!description.isEmpty()) description.remove(description.size() - 1); }
    public List<String> getCommands() { return commands; }
    public void addCommand(String cmd) { commands.add(cmd); }
    public void clearCommands() { commands.clear(); }
    public void removeLastCommand() { if (!commands.isEmpty()) commands.remove(commands.size() - 1); }
    public boolean hasVanillaItem() { return material != null; }
    public boolean hasCommands() { return !commands.isEmpty(); }
    public int getPointsPrice() { return pointsPrice; }
    public void setPointsPrice(int v) { this.pointsPrice = Math.max(0, v); }
    public int getPriceIncreasePercent() { return priceIncreasePercent; }
    public void setPriceIncreasePercent(int v) { this.priceIncreasePercent = Math.max(0, v); }
    public double getPriceIncreaseFixed() { return priceIncreaseFixed; }
    public void setPriceIncreaseFixed(double v) { this.priceIncreaseFixed = Math.max(0, v); }
    public String getPriceIncreaseType() { return priceIncreaseType; }
    public void setPriceIncreaseType(String v) { this.priceIncreaseType = "fixed".equals(v) ? "fixed" : "percent"; }
    public String getPriceIncreaseMode() { return priceIncreaseMode; }
    public void setPriceIncreaseMode(String v) { this.priceIncreaseMode = "global".equals(v) ? "global" : "player"; }
    public boolean hasPriceIncrease() {
        if (price <= 0) return false;
        return "fixed".equals(priceIncreaseType) ? priceIncreaseFixed > 0 : priceIncreasePercent > 0;
    }
    public String getExecutableItemsId() { return executableItemsId; }
    public void setExecutableItemsId(String v) { this.executableItemsId = (v == null || v.isBlank()) ? null : v.trim(); }
    public boolean hasExecutableItem() { return executableItemsId != null && !executableItemsId.isEmpty(); }
}
