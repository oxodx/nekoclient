/*
 * Copyright (c) NekoClient.
 */

package nl.oxod.nekoclient.gui.themes.catppuccin;

import meteordevelopment.meteorclient.gui.DefaultSettingsWidgetFactory;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WidgetScreen;
import meteordevelopment.meteorclient.gui.utils.AlignmentX;
import meteordevelopment.meteorclient.gui.widgets.*;
import meteordevelopment.meteorclient.gui.widgets.containers.WSection;
import meteordevelopment.meteorclient.gui.widgets.containers.WView;
import meteordevelopment.meteorclient.gui.widgets.containers.WWindow;
import meteordevelopment.meteorclient.gui.widgets.input.WDropdown;
import meteordevelopment.meteorclient.gui.widgets.input.WSlider;
import meteordevelopment.meteorclient.gui.widgets.input.WTextBox;
import meteordevelopment.meteorclient.gui.widgets.pressable.*;
import meteordevelopment.meteorclient.gui.renderer.packer.GuiTexture;
import meteordevelopment.meteorclient.gui.utils.CharFilter;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.accounts.Account;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import nl.oxod.nekoclient.gui.themes.catppuccin.widgets.*;
import nl.oxod.nekoclient.gui.themes.catppuccin.widgets.input.WCatppuccinDropdown;
import nl.oxod.nekoclient.gui.themes.catppuccin.widgets.input.WCatppuccinSlider;
import nl.oxod.nekoclient.gui.themes.catppuccin.widgets.input.WCatppuccinTextBox;
import nl.oxod.nekoclient.gui.themes.catppuccin.widgets.pressable.*;

import java.util.function.Supplier;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class CatppuccinGuiTheme extends GuiTheme {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgAccent = settings.createGroup("Accent");
    private final SettingGroup sgStarscript = settings.createGroup("Starscript");

    // General

    public final Setting<Double> scale = sgGeneral.add(new DoubleSetting.Builder()
        .name("scale")
        .description("Scale of the GUI.")
        .defaultValue(1)
        .min(0.75)
        .sliderRange(0.75, 4)
        .onSliderRelease()
        .onChanged(_ -> {
            if (mc.gui.screen() instanceof WidgetScreen widgetScreen) widgetScreen.invalidate();
        })
        .build()
    );

    public final Setting<AlignmentX> moduleAlignment = sgGeneral.add(new EnumSetting.Builder<AlignmentX>()
        .name("module-alignment")
        .description("How module titles are aligned.")
        .defaultValue(AlignmentX.Center)
        .build()
    );

    public final Setting<Boolean> categoryIcons = sgGeneral.add(new BoolSetting.Builder()
        .name("category-icons")
        .description("Adds item icons to module categories.")
        .defaultValue(false)
        .build()
    );

    public final Setting<Boolean> modulesHelpText = sgGeneral.add(new BoolSetting.Builder()
        .name("modules-help-text")
        .description("Toggle help text in the modules screen.")
        .defaultValue(true)
        .build()
    );

    public final Setting<Boolean> hideHUD = sgGeneral.add(new BoolSetting.Builder()
        .name("hide-HUD")
        .description("Hide HUD when in GUI.")
        .defaultValue(false)
        .onChanged(v -> {
            if (mc.gui.screen() instanceof WidgetScreen) {
                mc.gameRenderer.gameRenderState().guiRenderState.isHudHidden = v;
            }
        })
        .build()
    );

    // Flavor & accent

    public final Setting<CatppuccinFlavor> flavor = sgAccent.add(new EnumSetting.Builder<CatppuccinFlavor>()
        .name("flavor")
        .description("The Catppuccin flavor (palette) to use.")
        .defaultValue(CatppuccinFlavor.Mocha)
        .build()
    );

    public final Setting<CatppuccinAccent> accent = sgAccent.add(new EnumSetting.Builder<CatppuccinAccent>()
        .name("accent")
        .description("The main accent color used throughout the GUI.")
        .defaultValue(CatppuccinAccent.Mauve)
        .build()
    );

    // Three state colors, derived live from the selected flavor

    public final ThreeStateColor backgroundColor = new ThreeStateColor(
        this::baseColor,
        this::surface0Color,
        this::surface1Color
    );

    public final ThreeStateColor outlineColor = new ThreeStateColor(
        this::overlay0Color,
        this::overlay1Color,
        this::overlay2Color
    );

    public final ThreeStateColor scrollbarColor = new ThreeStateColor(
        this::surface0Color,
        this::surface1Color,
        this::surface2Color
    );

    public final ThreeStateColor sliderHandle = new ThreeStateColor(
        this::accentColor,
        this::accentColor,
        this::accentColor
    );

    // Starscript

    private final Setting<SettingColor> starscriptText = color(sgStarscript, "starscript-text", "Color of text in Starscript code.", new SettingColor(169, 183, 198));
    private final Setting<SettingColor> starscriptBraces = color(sgStarscript, "starscript-braces", "Color of braces in Starscript code.", new SettingColor(150, 150, 150));
    private final Setting<SettingColor> starscriptParenthesis = color(sgStarscript, "starscript-parenthesis", "Color of parenthesis in Starscript code.", new SettingColor(169, 183, 198));
    private final Setting<SettingColor> starscriptDots = color(sgStarscript, "starscript-dots", "Color of dots in starscript code.", new SettingColor(169, 183, 198));
    private final Setting<SettingColor> starscriptCommas = color(sgStarscript, "starscript-commas", "Color of commas in starscript code.", new SettingColor(169, 183, 198));
    private final Setting<SettingColor> starscriptOperators = color(sgStarscript, "starscript-operators", "Color of operators in Starscript code.", new SettingColor(169, 183, 198));
    private final Setting<SettingColor> starscriptStrings = color(sgStarscript, "starscript-strings", "Color of strings in Starscript code.", new SettingColor(106, 135, 89));
    private final Setting<SettingColor> starscriptNumbers = color(sgStarscript, "starscript-numbers", "Color of numbers in Starscript code.", new SettingColor(104, 141, 187));
    private final Setting<SettingColor> starscriptKeywords = color(sgStarscript, "starscript-keywords", "Color of keywords in Starscript code.", new SettingColor(204, 120, 50));
    private final Setting<SettingColor> starscriptAccessedObjects = color(sgStarscript, "starscript-accessed-objects", "Color of accessed objects (before a dot) in Starscript code.", new SettingColor(152, 118, 170));

    public CatppuccinGuiTheme() {
        super("Catppuccin");

        settingsFactory = new DefaultSettingsWidgetFactory(this);
    }

    private Setting<SettingColor> color(SettingGroup group, String name, String description, SettingColor color) {
        return group.add(new ColorSetting.Builder()
            .name(name + "-color")
            .description(description)
            .defaultValue(color)
            .build());
    }

    // Palette accessors

    public SettingColor accentColor() {
        return accent.get().get(flavor.get());
    }

    public SettingColor greenColor() {
        return get(CatppuccinColor.Green);
    }

    public SettingColor yellowColor() {
        return get(CatppuccinColor.Yellow);
    }

    public SettingColor redColor() {
        return get(CatppuccinColor.Red);
    }

    public SettingColor baseColor() {
        return get(CatppuccinColor.Base);
    }

    public SettingColor mantleColor() {
        return get(CatppuccinColor.Mantle);
    }

    public SettingColor crustColor() {
        return get(CatppuccinColor.Crust);
    }

    public SettingColor surface0Color() {
        return get(CatppuccinColor.Surface0);
    }

    public SettingColor surface1Color() {
        return get(CatppuccinColor.Surface1);
    }

    public SettingColor surface2Color() {
        return get(CatppuccinColor.Surface2);
    }

    public SettingColor overlay0Color() {
        return get(CatppuccinColor.Overlay0);
    }

    public SettingColor overlay1Color() {
        return get(CatppuccinColor.Overlay1);
    }

    public SettingColor overlay2Color() {
        return get(CatppuccinColor.Overlay2);
    }

    public SettingColor get(CatppuccinColor color) {
        return flavor.get().get(color);
    }

    public SettingColor placeholderColor() {
        SettingColor color = get(CatppuccinColor.Text);
        return new SettingColor(color.r, color.g, color.b, 20);
    }

    public SettingColor textHighlightColor() {
        SettingColor color = get(CatppuccinColor.Blue);
        return new SettingColor(color.r, color.g, color.b, 100);
    }

    // Widgets

    @Override
    public WWindow window(WWidget icon, String title) {
        return w(new WCatppuccinWindow(icon, title));
    }

    @Override
    public WLabel label(String text, boolean title, double maxWidth) {
        if (maxWidth == 0 && !text.contains("\n")) return w(new WCatppuccinLabel(text, title));
        return w(new WCatppuccinMultiLabel(text, title, maxWidth));
    }

    @Override
    public WHorizontalSeparator horizontalSeparator(String text) {
        return w(new WCatppuccinHorizontalSeparator(text));
    }

    @Override
    public WVerticalSeparator verticalSeparator() {
        return w(new WCatppuccinVerticalSeparator());
    }

    @Override
    protected WButton button(String text, GuiTexture texture) {
        return w(new WCatppuccinButton(text, texture));
    }

    @Override
    protected WConfirmedButton confirmedButton(String text, String confirmText, GuiTexture texture) {
        return w(new WCatppuccinConfirmedButton(text, confirmText, texture));
    }

    @Override
    public WMinus minus() {
        return w(new WCatppuccinMinus());
    }

    @Override
    public WConfirmedMinus confirmedMinus() {
        return w(new WCatppuccinConfirmedMinus());
    }

    @Override
    public WPlus plus() {
        return w(new WCatppuccinPlus());
    }

    @Override
    public WCheckbox checkbox(boolean checked) {
        return w(new WCatppuccinCheckbox(checked));
    }

    @Override
    public WSlider slider(double value, double min, double max) {
        return w(new WCatppuccinSlider(value, min, max));
    }

    @Override
    public WTextBox textBox(String text, String placeholder, CharFilter filter, Class<? extends WTextBox.Renderer> renderer) {
        return w(new WCatppuccinTextBox(text, placeholder, filter, renderer));
    }

    @Override
    public <T> WDropdown<T> dropdown(T[] values, T value) {
        return w(new WCatppuccinDropdown<>(values, value));
    }

    @Override
    public WTriangle triangle() {
        return w(new WCatppuccinTriangle());
    }

    @Override
    public WTooltip tooltip(String text) {
        return w(new WCatppuccinTooltip(text));
    }

    @Override
    public WView view() {
        return w(new WCatppuccinView());
    }

    @Override
    public WSection section(String title, boolean expanded, WWidget headerWidget) {
        return w(new WCatppuccinSection(title, expanded, headerWidget));
    }

    @Override
    public WAccount account(WidgetScreen screen, Account<?> account) {
        return w(new WCatppuccinAccount(screen, account));
    }

    @Override
    public WWidget module(Module module, String title) {
        return w(new WCatppuccinModule(module, title));
    }

    @Override
    public WQuad quad(Color color) {
        return w(new WCatppuccinQuad(color));
    }

    @Override
    public WTopBar topBar() {
        return w(new WCatppuccinTopBar());
    }

    @Override
    public WFavorite favorite(boolean checked) {
        return w(new WCatppuccinFavorite(checked));
    }

    // Colors - abstract overrides

    @Override
    public Color textColor() {
        return get(CatppuccinColor.Text);
    }

    @Override
    public Color textSecondaryColor() {
        return get(CatppuccinColor.Subtext0);
    }

    // Starscript

    @Override
    public Color starscriptTextColor() {
        return starscriptText.get();
    }

    @Override
    public Color starscriptBraceColor() {
        return starscriptBraces.get();
    }

    @Override
    public Color starscriptParenthesisColor() {
        return starscriptParenthesis.get();
    }

    @Override
    public Color starscriptDotColor() {
        return starscriptDots.get();
    }

    @Override
    public Color starscriptCommaColor() {
        return starscriptCommas.get();
    }

    @Override
    public Color starscriptOperatorColor() {
        return starscriptOperators.get();
    }

    @Override
    public Color starscriptStringColor() {
        return starscriptStrings.get();
    }

    @Override
    public Color starscriptNumberColor() {
        return starscriptNumbers.get();
    }

    @Override
    public Color starscriptKeywordColor() {
        return starscriptKeywords.get();
    }

    @Override
    public Color starscriptAccessedObjectColor() {
        return starscriptAccessedObjects.get();
    }

    // Other

    @Override
    public TextRenderer textRenderer() {
        return TextRenderer.get();
    }

    @Override
    public double scale(double value) {
        return value * scale.get();
    }

    @Override
    public boolean categoryIcons() {
        return categoryIcons.get();
    }

    @Override
    public boolean modulesHelpText() {
        return modulesHelpText.get();
    }

    @Override
    public boolean hideHUD() {
        return hideHUD.get();
    }

    public class ThreeStateColor {
        private final Supplier<SettingColor> normal, hovered, pressed;

        public ThreeStateColor(Supplier<SettingColor> normal, Supplier<SettingColor> hovered, Supplier<SettingColor> pressed) {
            this.normal = normal;
            this.hovered = hovered;
            this.pressed = pressed;
        }

        public SettingColor get() {
            return normal.get();
        }

        public SettingColor get(boolean pressed, boolean hovered, boolean bypassDisableHoverColor) {
            if (pressed) return this.pressed.get();
            return (hovered && (bypassDisableHoverColor || !disableHoverColor)) ? this.hovered.get() : this.normal.get();
        }

        public SettingColor get(boolean pressed, boolean hovered) {
            return get(pressed, hovered, false);
        }
    }
}