package com.reedwellarts.custompets.client.screen;

import com.reedwellarts.custompets.client.networking.CustomPetsClientNetworking;
import com.reedwellarts.custompets.client.networking.snapshot.PetStatsSnapshot;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.*;

public class PetRosterScreen extends Screen {

    private static final int WINDOW_W = 320;
    private static final int WINDOW_H = 180;
    private static final int SLOT = 18;
    private static final int PAD = 8;
    private static final int STATS_H = 58;
    private static final int TAMED_COLS = 8;
    private static final int TAMED_ROWS = 3;
    private static final int ACTIVE_COLS = 2;
    private static final int ACTIVE_ROWS = 2;

    private final List<PetStatsSnapshot> pets;
    private final Map<Integer, String> activeSlots = new HashMap<>();

    private final int maxActive;

    private int left;
    private int top;

    private String selectedPetUuid;
    private String draggingPetUuid;
    private int draggingFromActiveIndex = -1;


    public PetRosterScreen(List<PetStatsSnapshot> pets, int maxActive) {
        super(Text.literal("Pet Roster"));
        this.pets = new ArrayList<>(pets);
        this.maxActive = Math.clamp(maxActive, 1, ACTIVE_COLS * ACTIVE_ROWS);

        List<String> initiallyActive = this.pets.stream()
                .filter(PetStatsSnapshot::active)
                .map(PetStatsSnapshot::petUuid)
                .sorted()
                .toList();

        for (int i = 0; i < Math.min(maxActive, initiallyActive.size()); i++){
            activeSlots.put(i, initiallyActive.get(i));
        }

        if (!this.pets.isEmpty()){
            this.selectedPetUuid = this.pets.getFirst().petUuid();
        }
    }

    @Override
    protected void init(){
        this.left = (this.width - WINDOW_W) / 2;
        this.top = (this.height - WINDOW_H) / 2;

        addDrawableChild(ButtonWidget.builder(Text.literal("Close"), b -> close())
                .dimensions(left + WINDOW_W - 68, top + WINDOW_H + 6, 60, 20)
                .build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Confirm"), b -> confirm())
                .dimensions(left + WINDOW_W - 136, top + WINDOW_H + 6, 60, 20)
                .build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        context.fill(left, top, left + WINDOW_W, top + WINDOW_H, 0xCC1E1E1E);
        drawBorder(context, left, top, WINDOW_W, WINDOW_H, 0xFFFFFFFF);

        drawStatsPanel(context);
        drawTamedGrid(context, mouseX, mouseY);
        drawActiveGrid(context, mouseX, mouseY);

        if (draggingPetUuid != null){
            PetStatsSnapshot snapshot = findByUuid(draggingPetUuid);
            if (snapshot != null){
                context.fill(mouseX - 9, mouseY - 9, mouseX + 9, mouseY + 9, 0xD0000000);
                drawBorder(context, mouseX - 9, mouseY - 9, 18, 18, 0xFFFFFFFF);
                drawPetEgg(context, snapshot, mouseX - 8, mouseY - 8);
            }
        }

        super.render(context, mouseX, mouseY, deltaTicks);
    }

    private void drawBorder(DrawContext context, int x, int y, int w, int h, int color){
        context.fill(x, y, x + w, y + 1, color);
        context.fill(x, y + h - 1, x + w, y + h, color);
        context.fill(x, y, x + 1, y + h, color);
        context.fill(x + w - 1, y, x + w, y + h, color);
    }

    private void drawStatsPanel(DrawContext context){
        int x = left + PAD;
        int y = top + 18;
        int w = WINDOW_W - PAD * 2;

        context.fill(x, y, x + w, y + STATS_H, 0x88252525);
        drawBorder(context, x, y, w, STATS_H, 0xFF555555);

        PetStatsSnapshot selected = selectedPetUuid == null ? null : findByUuid(selectedPetUuid);
        if (selected == null){
            context.drawText(textRenderer, Text.literal("Select a pet"), x + 6, y + 6, 0xFFAAAAAA, false);
            return;
        }

        context.drawText(textRenderer,
                Text.literal(selected.name() + " Lv." + selected.level()),
                x + 6, y + 6, 0xFFFFFFFF, false);

        int barX = x + 6;
        int barW = w - 12;

        drawBar(context, barX, y + 26, barW, 8,
                selected.health(), Math.max(1, selected.maxHealth()),
                0xFF2A2A2A, 0xFFD94A4A,
                "HP " + selected.health() + "/" + selected.maxHealth());

        drawBar(context, barX, y + 46, barW, 8,
                selected.xp(), Math.max(1, selected.xpToNextLevel()),
                0xFF2A2A2A, 0xFF3C9C28,
                "XP " + selected.xp() + "/" + selected.xpToNextLevel());
    }

    private void drawTamedGrid(DrawContext context, int mouseX, int mouseY){
        int x = left + PAD;
        int y = top + 24 + STATS_H + PAD;

        context.drawText(textRenderer, Text.literal("Tamed"), x, y - 10, 0xFFFFFFFF, false);

        List<PetStatsSnapshot> sorted = pets.stream()
                .sorted(Comparator.comparing(PetStatsSnapshot::name))
                .toList();

        for (int i = 0; i < TAMED_COLS * TAMED_ROWS; i++){
            int sx = x + (i % TAMED_COLS) * (SLOT + 2);
            int sy = y + (i / TAMED_COLS) * (SLOT + 2);
            drawSlot(context, sx, sy, false);

            if (i >= sorted.size()) continue;

            PetStatsSnapshot snapshot = sorted.get(i);
            drawPetEgg(context, snapshot, sx + 1, sy + 1);

            if (isInside(mouseX, mouseY, sx, sy, SLOT, SLOT)){
                selectedPetUuid = snapshot.petUuid();
            }
        }
    }

    private void drawActiveGrid(DrawContext context, int mouseX, int mouseY){
        int x = left + WINDOW_W - PAD - (ACTIVE_COLS * (SLOT + 2));
        int y = top + 24 + STATS_H + PAD;

        context.drawText(textRenderer, Text.literal("Active"), x, y - 10, 0xFFFFFFFF, false);

        for (int i=0; i < ACTIVE_COLS * ACTIVE_ROWS; i++){
            int sx = x + (i % ACTIVE_COLS) * (SLOT + 2);
            int sy = y + (i / ACTIVE_COLS) * (SLOT + 2);
            drawSlot(context, sx, sy, i >= maxActive);

            if (i < maxActive){
                String petId = activeSlots.get(i);
                if (petId != null){
                    PetStatsSnapshot snapshot = findByUuid(petId);
                    if (snapshot != null){
                        drawPetEgg(context, snapshot, sx + 1, sy + 1);
                        if (isInside(mouseX, mouseY, sx, sy, SLOT, SLOT)){
                            selectedPetUuid = snapshot.petUuid();
                        }
                    }
                }
            }
        }
    }

    private void drawSlot(DrawContext context, int x, int y, boolean disabled){
        int fill = disabled ? 0x55333333 : 0xAA1A1A1A;
        int border = disabled ? 0xFF444444 : 0xFF888888;
        context.fill(x, y, x + SLOT, y + SLOT, fill);
        drawBorder(context, x, y, SLOT, SLOT, border);
    }

    private void drawBar(DrawContext context, int x, int y, int w, int h,
                         int value, int max, int bg, int fg, String label){
        context.fill(x, y, x + w, y + h, bg);
        int fill = (int) ((Math.clamp(value, 0, max) / (double) max) * w);
        context.fill(x, y, x + fill, y + h, fg);
        drawBorder(context, x, y, w, h,0xFF000000);
        context.drawText(textRenderer, Text.literal(label), x + 2, y - 9, 0xFFDDDDDD, false);
    }

    private void drawPetEgg(DrawContext context, PetStatsSnapshot snapshot, int x, int y){
        ItemStack stack = getSpawnEggStack(snapshot.petType());
        context.drawItem(stack, x, y);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (click.button() != 0) return super.mouseClicked(click, doubled);

        String tamedHit = getTamedHit(click.x(), click.y());
        if (tamedHit != null){
            draggingPetUuid = tamedHit;
            draggingFromActiveIndex = indexOfActiveSlot(tamedHit);
            selectedPetUuid = tamedHit;
            return true;
        }

        int activeIndex = getActiveSlotIndex(click.x(), click.y());
        if (activeIndex >= 0 && activeIndex < maxActive){
            String id = activeSlots.get(activeIndex);
            if (id != null) {
                draggingPetUuid = id;
                draggingFromActiveIndex = activeIndex;
                selectedPetUuid = id;
                return true;
            }
        }

        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseDragged(Click click, double offsetX, double offsetY) {
        if (draggingPetUuid != null) return true;
        return super.mouseDragged(click, offsetX, offsetY);
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (click.button() != 0 || draggingPetUuid == null){
            return super.mouseReleased(click);
        }

        int dropIndex = getActiveSlotIndex(click.x(), click.y());

        removePetFromActiveSlots(draggingPetUuid);

        if (dropIndex >= 0 && dropIndex < maxActive){
            activeSlots.put(dropIndex, draggingPetUuid);
        }else if (draggingFromActiveIndex >= 0){
            activeSlots.remove(draggingFromActiveIndex);
        }

        draggingPetUuid = null;
        draggingFromActiveIndex = -1;
        return true;
    }

    private String getTamedHit(double mouseX, double mouseY){
        int x = left + PAD;
        int y = top + 24 + STATS_H + PAD;

        List<PetStatsSnapshot> sorted = pets.stream()
                .sorted(Comparator.comparing(PetStatsSnapshot::name))
                .toList();

        for (int i = 0; i < sorted.size(); i++){
            int sx = x + (i % TAMED_COLS) * (SLOT + 2);
            int sy = y + (i / TAMED_COLS) * (SLOT + 2);
            if (isInside(mouseX, mouseY, sx, sy, SLOT, SLOT)){
                return sorted.get(i).petUuid();
            }
        }
        return null;
    }

    private int getActiveSlotIndex(double mouseX, double mouseY){
        int x = left + WINDOW_W - PAD - (ACTIVE_COLS * (SLOT + 2));
        int y = top + 24 + STATS_H + PAD;

        for (int i = 0; i < ACTIVE_COLS * ACTIVE_ROWS; i++){
            int sx = x + (i % ACTIVE_COLS) * (SLOT + 2);
            int sy = y + (i / ACTIVE_COLS) * (SLOT + 2);
            if (isInside(mouseX, mouseY, sx, sy, SLOT, SLOT)) return i;
        }

        return - 1;
    }

    private int indexOfActiveSlot(String petUuid){
        for (int i = 0; i < maxActive; i++){
            String id = activeSlots.get(i);
            if (petUuid.equals(id)) return i;
        }
        return -1;
    }

    private void removePetFromActiveSlots(String petUuid){
        Integer keyToRemove = null;
        for (Map.Entry<Integer, String> entry : activeSlots.entrySet()){
            if (petUuid.equals(entry.getValue())){
                keyToRemove = entry.getKey();
                break;
            }
        }

        if (keyToRemove != null) activeSlots.remove(keyToRemove);
    }

    private boolean isInside(double mouseX, double mouseY, int x, int y, int w, int h){
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }

    private PetStatsSnapshot findByUuid(String uuid){
        for (PetStatsSnapshot snapshot : pets){
            if (snapshot.petUuid().equals(uuid)) return snapshot;
        }
        return null;
    }

    private ItemStack getSpawnEggStack(String petType){
        Identifier entityId = Identifier.tryParse(petType);
        if (entityId == null){
            return new ItemStack(Items.BARRIER);
        }

        Identifier eggId = Identifier.of(entityId.getNamespace(), entityId.getPath() + "_spawn_egg");
        Item item = Registries.ITEM.get(eggId);

        if (item instanceof SpawnEggItem){
            return new ItemStack(item);
        }

        return new ItemStack(Items.BARRIER);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private void confirm(){
        List<String> petUuids = new ArrayList<>();

        for (Map.Entry<Integer, String> entry : activeSlots.entrySet()){
            petUuids.add(entry.getValue());
        }

        CustomPetsClientNetworking.setActivePets(petUuids);
    }
}
