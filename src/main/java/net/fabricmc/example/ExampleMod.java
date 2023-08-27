package net.fabricmc.example;

import baritone.api.BaritoneAPI;
import baritone.api.Settings;
import net.fabricmc.api.ModInitializer;
import net.minecraft.item.Item;
import net.minecraft.item.Items;

import java.util.ArrayList;
import java.util.List;

public class ExampleMod implements ModInitializer {


	public static final List<Item> throwawayLightItems = new ArrayList<>();
	public static final int TORCH_AREA_PROCESS_TICKS_BETWEEN_REFRESH = 100;

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		System.out.println("Hello Fabric world!");

		//Popular throwawayLightItems
		throwawayLightItems.add(Items.TORCH);
		throwawayLightItems.add(Items.LANTERN);

	}
}
