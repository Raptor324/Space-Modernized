//? if forge {
package com.space_m.datagen;

import com.space_m.SpaceMod;

import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.LanguageProvider;

/**
 * Lang-Dateien {@code assets/space_m/lang/en_us.json} und {@code ru_ru.json}. Inhalt ausschliesslich aus
 * {@link SpaceLang} (Original-Texte, gepflegt mit {@code tools/space_lang.py}).
 */
public final class SpaceLanguageProvider extends LanguageProvider {
    private final boolean russian;

    public SpaceLanguageProvider(PackOutput output, String locale, boolean russian) {
        super(output, SpaceMod.MOD_ID, locale);
        this.russian = russian;
    }

    @Override
    protected void addTranslations() {
        SpaceLang.addAll(this::add, russian);
    }
}
//?}
