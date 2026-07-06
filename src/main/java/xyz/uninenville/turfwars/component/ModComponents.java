package xyz.uninenville.turfwars.component;

import eu.pb4.polymer.core.api.other.PolymerComponent;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Unit;
import xyz.uninenville.turfwars.TurfWars;
import xyz.uninenville.turfwars.component.type.BarrageComponent;

public class ModComponents {
    public static void initialize() {
        PolymerComponent.registerDataComponent(
            APPLY_TEAM_COLOR, BARRAGE_ABILITY, BARRAGE_PROJECTILES_LOADED, FLETCHING_PROJECTILE, PROJECTILE_DAMAGE_OVERRIDE
        );
    }

    public static final DataComponentType<Unit> APPLY_TEAM_COLOR = Registry.register(
        BuiltInRegistries.DATA_COMPONENT_TYPE,
        TurfWars.id("apply_team_color"),
        DataComponentType.<Unit>builder().persistent(Unit.CODEC).build()
    );

    public static final DataComponentType<BarrageComponent> BARRAGE_ABILITY = Registry.register(
        BuiltInRegistries.DATA_COMPONENT_TYPE,
        TurfWars.id("barrage_ability"),
        DataComponentType.<BarrageComponent>builder().persistent(BarrageComponent.CODEC).build()
    );

    public static final DataComponentType<Integer> BARRAGE_PROJECTILES_LOADED = Registry.register(
        BuiltInRegistries.DATA_COMPONENT_TYPE,
        TurfWars.id("barrage_projectiles_loaded"),
        DataComponentType.<Integer>builder().persistent(ExtraCodecs.NON_NEGATIVE_INT).ignoreSwapAnimation().build()
    );

    public static final DataComponentType<Unit> FLETCHING_PROJECTILE = Registry.register(
        BuiltInRegistries.DATA_COMPONENT_TYPE,
        TurfWars.id("fletching_projectile"),
        DataComponentType.<Unit>builder().persistent(Unit.CODEC).build()
    );

    public static final DataComponentType<Float> PROJECTILE_DAMAGE_OVERRIDE = Registry.register(
        BuiltInRegistries.DATA_COMPONENT_TYPE,
        TurfWars.id("projectile_damage_override"),
        DataComponentType.<Float>builder().persistent(ExtraCodecs.NON_NEGATIVE_FLOAT).build()
    );
}
