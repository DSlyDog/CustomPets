package com.reedwellarts.custompets.client.screen;

import com.reedwellarts.custompets.client.networking.CustomPetsClientNetworking;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.TextWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class PetNameScreen extends Screen {

    private TextWidget title;
    private TextFieldWidget nameField;
    private final String petUuid;

    public PetNameScreen(String petUuid){
        super(Text.literal("Name pet"));
        this.petUuid = petUuid;
    }

    @Override
    protected void init() {
        title = new TextWidget(width / 2 - (textRenderer.getWidth(getTitle().asOrderedText()) / 2), height / 2 - 60, textRenderer.getWidth(getTitle().asOrderedText()), 20, getTitle(), textRenderer);
        addDrawableChild(title);

        nameField = new TextFieldWidget(textRenderer, width / 2 - 100, height / 2 - 20, 200 , 20, Text.literal("Portal name"));
        nameField.setMaxLength(32);
        nameField.setFocused(true);
        setFocused(nameField);
        addDrawableChild(nameField);

        addDrawableChild(ButtonWidget.builder(Text.literal("Confirm"), button -> confirm())
                .dimensions(width / 2 - 102, height / 2 + 8, 98, 20)
                .build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Cancel"), button -> close())
                .dimensions(width / 2 + 4, height / 2 + 8, 98, 20)
                .build());
    }

    @Override
    public boolean keyPressed(KeyInput keyInput){
        if (keyInput.getKeycode() == GLFW.GLFW_KEY_ENTER || keyInput.getKeycode() == GLFW.GLFW_KEY_KP_ENTER){
            confirm();
            return true;
        }
        return super.keyPressed(keyInput);
    }

    @Override
    public boolean shouldPause(){
        return false;
    }

    private void confirm(){
        String name = nameField.getText().trim();
        name = name.replace("&", "§");

        if (name.isEmpty()) return;

        CustomPetsClientNetworking.setEntityName(name, petUuid);
        close();
    }
}
