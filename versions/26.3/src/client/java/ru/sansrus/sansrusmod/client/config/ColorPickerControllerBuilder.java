package ru.sansrus.sansrusmod.client.config;

import dev.isxander.yacl3.api.Controller;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.controller.ControllerBuilder;

public class ColorPickerControllerBuilder implements ControllerBuilder<Integer> {
    private final Option<Integer> option;

    private ColorPickerControllerBuilder(Option<Integer> option) {
        this.option = option;
    }

    public static ColorPickerControllerBuilder create(Option<Integer> option) {
        return new ColorPickerControllerBuilder(option);
    }

    @Override
    public Controller<Integer> build() {
        return new ColorPickerController(option);
    }
}
