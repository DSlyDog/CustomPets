package com.reedwellarts.custompets.client.screen.skilltree;

import com.reedwellarts.custompets.client.networking.CustomPetsClientNetworking;
import com.reedwellarts.custompets.client.networking.snapshot.PetStatsSnapshot;
import com.reedwellarts.custompets.pet.skill.PetSkillState;
import net.fabricmc.loader.impl.lib.tinyremapper.extension.mixin.hard.util.IdentityString;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ThreePartsLayoutWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.*;
import java.util.stream.Collectors;

public class PetSkillTreeScreen extends Screen {

    private static final Identifier WINDOW_TEXTURE = Identifier.ofVanilla("textures/gui/advancements/window.png");
    private static final Identifier TREE_BG_TEXTURE = Identifier.ofVanilla("textures/gui/advancements/backgrounds/stone.png");

    private static final int WINDOW_WIDTH = 252;
    private static final int WINDOW_HEIGHT = 140;
    private static final int NODE_W = 26;
    private static final int NODE_H = 26;
    private static final int TREE_X = 9;
    private static final int TREE_Y = 18;
    private static final int TREE_W = 234;
    private static final int TREE_H = 113;

    private final String petUuid;
    private final List<PetSkillNode> nodes = new ArrayList<>();

    private final Set<Identifier> selectedActive = new HashSet<>();

    private final ThreePartsLayoutWidget layout;

    private double panX = 0;
    private double panY = 0;
    private double zoom = 1.0;
    private boolean dragging = false;
    private double lastMouseX;
    private double lastMouseY;

    public static PetSkillTreeScreen fromSnapshot(PetStatsSnapshot stats){
        Set<Identifier> unlocked = stats.unlockedSkills().stream()
                .map(Identifier::tryParse)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        List<PetSkillNode> nodes = SkillTreeDefinition.ALL.stream()
                .map(def -> new PetSkillNode(
                        def.id(),
                        def.item(),
                        def.title(),
                        def.description(),
                        def.x(),
                        def.y(),
                        def.parents(),
                        unlocked.contains(def.id()),
                        def.unlockLevel()
                ))
                .toList();

        return new PetSkillTreeScreen(stats.petUuid(), nodes, stats.activeSkills());
    }

    public PetSkillTreeScreen(String petUuid, List<PetSkillNode> initialNodes, List<String> initiallyActiveSkills) {
        super(Text.literal("Pet Skills"));
        this.petUuid = petUuid;
        this.nodes.addAll(initialNodes);
        this.layout = new ThreePartsLayoutWidget(this);

        for (String raw : initiallyActiveSkills){
            Identifier id = Identifier.tryParse(raw);
            if (id != null){
                selectedActive.add(id);
            }
        }

        selectedActive.removeIf(id -> {
            PetSkillNode node = find(id);
            return node == null || !node.unlocked();
        });
    }

    @Override
    protected void init(){
        int left = (this.width - WINDOW_WIDTH) / 2;
        int top = (this.height - WINDOW_HEIGHT) / 2;

        this.layout.addHeader(getTitle(), this.textRenderer);
        int y = top + WINDOW_HEIGHT + 5;

        addDrawableChild(ButtonWidget.builder(Text.literal("Close"), b -> close())
                .dimensions(left + 8, y, 60, 20)
                .build()
        );

        addDrawableChild(ButtonWidget.builder(Text.literal("Confirm"), b -> confirm())
                .dimensions(left + WINDOW_WIDTH - 69, y, 60, 20)
                .build()
        );

        centerNodes();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        int left = (this.width - WINDOW_WIDTH) / 2;
        int top = (this.height - WINDOW_HEIGHT) / 2;

        this.drawWindow(context, left, top);
        this.drawSkillTree(context, left + TREE_X, top + TREE_Y, mouseX, mouseY);

        super.render(context, mouseX, mouseY, deltaTicks);
    }

    public void drawWindow(DrawContext context, int x, int y){
        context.drawTexture(RenderPipelines.GUI_TEXTURED, WINDOW_TEXTURE, x, y, 0.0F, 0.0F, 252, 140, 256, 256);
        context.drawText(textRenderer, getTitle(), x + 8, y + 6, -12566464, false);
    }

    private void drawSkillTree(DrawContext context, int x, int y, int mouseX, int mouseY){
        context.enableScissor(x, y, x + TREE_W, y + TREE_H);

        int centerX = x + TREE_W / 2;
        int centerY = y + TREE_H / 2;

        int tile = 16;
        int drawTile = Math.max(1, (int) Math.ceil(tile * zoom));
        double minTreeX = ((x - centerX) / zoom) - panX - tile;
        double maxTreeX = ((x + TREE_W - centerX) / zoom) - panX + tile;
        double minTreeY = ((y - centerY) / zoom) - panY - tile;
        double maxTreeY = ((y + TREE_H - centerY) / zoom) - panY + tile;

        int startTreeX = ((int) Math.floor(minTreeX / tile)) * tile;
        int endTreeX   = ((int) Math.ceil(maxTreeX / tile)) * tile;
        int startTreeY = ((int) Math.floor(minTreeY / tile)) * tile;
        int endTreeY   = ((int) Math.ceil(maxTreeY / tile)) * tile;

        for (int txTree = startTreeX; txTree <= endTreeX; txTree += tile) {
            for (int tyTree = startTreeY; tyTree <= endTreeY; tyTree += tile) {
                int sx = toScreenX(txTree, centerX);
                int sy = toScreenY(tyTree, centerY);
                context.drawTexture(
                        RenderPipelines.GUI_TEXTURED,
                        TREE_BG_TEXTURE,
                        sx,
                        sy,
                        0.0F,
                        0.0F,
                        drawTile,
                        drawTile,
                        tile,
                        tile
                );
            }
        }

        int scaledW = (int)(NODE_W * zoom);
        int scaledH = (int)(NODE_H * zoom);

        for (PetSkillNode child : nodes){
            for (Identifier parentId : child.parents()){
                PetSkillNode parent = find(parentId);
                if (parent == null) continue;

                int x1 = toScreenX(parent.x(), centerX) + scaledW / 2;
                int y1 = toScreenY(parent.y(), centerY) + scaledH / 2;
                int x2 = toScreenX(child.x(), centerX) + scaledW / 2;
                int y2 = toScreenY(child.y(), centerY) + scaledH / 2;

                context.drawHorizontalLine(Math.min(x1, x2), Math.max(x1, x2), y1, 0xFFFFFFFF);
                context.drawVerticalLine(x2, Math.min(y1, y2), Math.max(y1, y2), 0xFFFFFFFF);
            }
        }

        boolean mouseInViewport = mouseX >= x && mouseX < x + TREE_W && mouseY >= y && mouseY < y + TREE_H;

        for (PetSkillNode n : nodes){
            int sx = toScreenX(n.x(), centerX);
            int sy = toScreenY(n.y(), centerY);

            boolean selected = selectedActive.contains(n.id());
            boolean hovering = mouseInViewport && isHoveringNode(mouseX, mouseY, sx, sy, scaledW, scaledH);

            int borderSize = 3;
            int scaledBorder = (int)(borderSize * zoom);
            int borderColor = selected ? 0xFF52A535 : 0x0;
            int color = 0xFFDBDBDB;

            if (hovering && n.unlocked()){
                borderColor = selected ? borderColor : 0xFFFFFFFF;
            }

            if (!n.unlocked()){
                color = 0xFF444444;
            }

            context.fill(sx - scaledBorder, sy - scaledBorder, sx + scaledW + scaledBorder, sy + scaledH + scaledBorder, borderColor);
            context.fill(sx, sy, sx + scaledW, sy + scaledH, color);

            context.getMatrices().pushMatrix();
            float itemScale = (float) zoom;

            float centerItemX = sx + scaledW / 2f;
            float centerItemY = sy + scaledH / 2f;

            context.getMatrices().translate(centerItemX, centerItemY);
            context.getMatrices().scale(itemScale, itemScale);

            context.drawItem(n.item(), -8, -8);
            context.getMatrices().popMatrix();

            if (hovering){
                List<Text> tooltip = new ArrayList<>();
                tooltip.add(n.title());

                String[] lines = n.description().getString().split("\\n");
                for (String line : lines) {
                    tooltip.add(Text.literal(line));
                }

                if (n.unlocked()){
                    tooltip.add(Text.literal(selected ? "§aActive (click to deactivate)" : "§cInactive (click to activate)"));
                } else {
                    tooltip.add(Text.literal("§8Locked (Unlocked at level " + n.unlockLevel() + ")"));
                }

                context.drawTooltip(textRenderer, tooltip, mouseX, mouseY);
            }
        }

        context.disableScissor();
    }

    private PetSkillNode find(Identifier id){
        for (PetSkillNode n : nodes) if (n.id().equals(id)) return n;
        return null;
    }

    private int toScreenX(int treeX, int centerX){
        return (int) (centerX + (treeX + panX) * zoom);
    }

    private int toScreenY(int treeY, int centerY){
        return (int) (centerY + (treeY + panY) * zoom);
    }

    private boolean isHoveringNode(int mouseX, int mouseY, int x, int y, int w, int h){
        return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
    }

    private void confirm(){
        List<String> active = selectedActive.stream()
                .map(Identifier::toString)
                .toList();

        CustomPetsClientNetworking.setPetActiveSkills(petUuid, active);
        close();
    }

    private boolean isInTreeViewport(double mouseX, double mouseY){
        int left = (this.width - WINDOW_WIDTH) / 2;
        int top = (this.height - WINDOW_HEIGHT) / 2;

        int x1 = left + TREE_X;
        int y1 = top + TREE_Y;
        int x2 = x1 + TREE_W;
        int y2 = y1 + TREE_H;

        return mouseX >= x1 && mouseX < x2 && mouseY >= y1 && mouseY < y2;
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (click.button() == 0){
            if (tryToggleNode(click.x(), click.y())) return true;

            if (isInTreeViewport(click.x(), click.y())) {
                dragging = true;
                lastMouseX = click.x();
                lastMouseY = click.y();
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (click.button() == 0) dragging = false;
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseDragged(Click click, double offsetX, double offsetY) {
        if (dragging && click.button() == 0){
            panX += (click.x() - lastMouseX);
            panY += (click.y() - lastMouseY);
            lastMouseX = click.x();
            lastMouseY = click.y();
            return true;
        }
        return super.mouseDragged(click, offsetX, offsetY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        double factor = verticalAmount > 0 ? 1.1 : 0.9;
        zoom = Math.clamp(zoom * factor, 0.5, 2.5);
        return true;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private boolean tryToggleNode(double mouseX, double mouseY){
        if (!isInTreeViewport(mouseX, mouseY)) return false;

        int left = (this.width - WINDOW_WIDTH) / 2;
        int top = (this.height - WINDOW_HEIGHT) / 2;
        int centerX = left + TREE_X + TREE_W / 2;
        int centerY = top + TREE_Y + TREE_H / 2;
        int scaledW = (int)(NODE_W * zoom);
        int scaledH = (int)(NODE_H * zoom);

        for (PetSkillNode n : nodes){
            int sx = toScreenX(n.x(), centerX);
            int sy = toScreenY(n.y(), centerY);

            if (!isHoveringNode((int)mouseX, (int)mouseY, sx, sy, scaledW, scaledH)) continue;

            if (!n.unlocked()) return true;

            if (selectedActive.contains(n.id())){
                selectedActive.remove(n.id());
                return true;
            }

            if (selectedActive.size() >= PetSkillState.MAX_ACTIVE){
                return true;
            }

            selectedActive.add(n.id());
            return true;
        }

        return false;
    }

    private void centerNodes(){
        if (nodes.isEmpty()) return;

        List<PetSkillNode> roots = nodes.stream()
                .filter(n -> n.parents().isEmpty())
                .toList();

        int minRootY = roots.stream().mapToInt(PetSkillNode::y).min().getAsInt();
        int maxRootY = roots.stream().mapToInt(PetSkillNode::y).max().getAsInt();

        int centerRootY = (minRootY + maxRootY) / 2;

        panX = -(TREE_W / 2.0) + NODE_W;
        panY = -(centerRootY / 2.0);
    }
}
