package xyz.uninenville.turfwars.component;

import eu.pb4.polymer.core.api.other.PolymerComponent;
import net.minecraft.component.ComponentType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Unit;
import net.minecraft.util.dynamic.Codecs;
import xyz.uninenville.turfwars.TurfWars;
import xyz.uninenville.turfwars.component.type.BarrageComponent;

public class ModComponents {
    public static void initialize() {
        PolymerComponent.registerDataComponent(
            APPLY_TEAM_COLOR, BARRAGE_ABILITY, BARRAGE_PROJECTILES_LOADED, FLETCHING_PROJECTILE, PROJECTILE_DAMAGE_OVERRIDE
        );
    }

    public static final ComponentType<Unit> APPLY_TEAM_COLOR = Registry.register(
        Registries.DATA_COMPONENT_TYPE,
        TurfWars.id("apply_team_color"),
        ComponentType.<Unit>builder().codec(Unit.CODEC).build()
    );

    public static final ComponentType<BarrageComponent> BARRAGE_ABILITY = Registry.register(
        Registries.DATA_COMPONENT_TYPE,
        TurfWars.id("barrage_ability"),
        ComponentType.<BarrageComponent>builder().codec(BarrageComponent.CODEC).build()
    );

    public static final ComponentType<Integer> BARRAGE_PROJECTILES_LOADED = Registry.register(
        Registries.DATA_COMPONENT_TYPE,
        TurfWars.id("barrage_projectiles_loaded"),
        ComponentType.<Integer>builder().codec(Codecs.NON_NEGATIVE_INT).skipsHandAnimation().build()
    );

    public static final ComponentType<Unit> FLETCHING_PROJECTILE = Registry.register(
        Registries.DATA_COMPONENT_TYPE,
        TurfWars.id("fletching_projectile"),
        ComponentType.<Unit>builder().codec(Unit.CODEC).build()
    );

    public static final ComponentType<Float> PROJECTILE_DAMAGE_OVERRIDE = Registry.register(
        Registries.DATA_COMPONENT_TYPE,
        TurfWars.id("projectile_damage_override"),
        ComponentType.<Float>builder().codec(Codecs.NON_NEGATIVE_FLOAT).build()
    );
}
