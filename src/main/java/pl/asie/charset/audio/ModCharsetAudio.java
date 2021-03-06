package pl.asie.charset.audio;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerAboutToStartEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppedEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.oredict.RecipeSorter;
import net.minecraftforge.oredict.ShapedOreRecipe;

import mcmultipart.multipart.MultipartRegistry;
import pl.asie.charset.api.audio.IDataStorage;
import pl.asie.charset.audio.note.BlockIronNote;
import pl.asie.charset.audio.note.PacketNoteParticle;
import pl.asie.charset.audio.note.TileIronNote;
import pl.asie.charset.audio.storage.DataStorageImpl;
import pl.asie.charset.audio.storage.DataStorageManager;
import pl.asie.charset.audio.storage.DataStorageStorage;
import pl.asie.charset.audio.tape.ItemPartTapeDrive;
import pl.asie.charset.audio.tape.ItemTape;
import pl.asie.charset.audio.tape.ItemTapeReel;
import pl.asie.charset.audio.tape.PacketDriveAudio;
import pl.asie.charset.audio.tape.PacketDriveCounter;
import pl.asie.charset.audio.tape.PacketDriveRecord;
import pl.asie.charset.audio.tape.PacketDriveState;
import pl.asie.charset.audio.tape.PacketDriveStop;
import pl.asie.charset.audio.tape.PartTapeDrive;
import pl.asie.charset.audio.tape.RecipeTape;
import pl.asie.charset.audio.tape.RecipeTapeReel;
import pl.asie.charset.lib.ModCharsetLib;
import pl.asie.charset.lib.network.PacketRegistry;

@Mod(modid = ModCharsetAudio.MODID, name = ModCharsetAudio.NAME, version = ModCharsetAudio.VERSION,
		dependencies = ModCharsetLib.DEP_MCMP, updateJSON = ModCharsetLib.UPDATE_URL)
public class ModCharsetAudio {
	public static final String MODID = "CharsetAudio";
	public static final String NAME = "♫";
	public static final String VERSION = "@VERSION@";

	@SidedProxy(clientSide = "pl.asie.charset.audio.ProxyClient", serverSide = "pl.asie.charset.audio.ProxyCommon")
	public static ProxyCommon proxy;

	@Mod.Instance(MODID)
	public static ModCharsetAudio instance;

	@CapabilityInject(IDataStorage.class)
	public static Capability<IDataStorage> CAP_STORAGE;

	public static Logger logger;

	public static PacketRegistry packet;
	public static DataStorageManager storage;

	public static BlockIronNote ironNoteBlock;
	public static ItemPartTapeDrive partTapeDriveItem;
	public static ItemTape tapeItem;
	public static Item magneticTapeItem, tapeReelItem;

	@Mod.EventHandler
	public void preInit(FMLPreInitializationEvent event) {
		logger = LogManager.getLogger(ModCharsetAudio.MODID);

		partTapeDriveItem = new ItemPartTapeDrive();
		GameRegistry.registerItem(partTapeDriveItem, "tapeDrive");

		tapeItem = new ItemTape();
		GameRegistry.registerItem(tapeItem, "tape");

		magneticTapeItem = new Item().setCreativeTab(ModCharsetLib.CREATIVE_TAB).setUnlocalizedName("charset.tapeitem");
		tapeReelItem = new ItemTapeReel().setHasSubtypes(true).setCreativeTab(ModCharsetLib.CREATIVE_TAB).setUnlocalizedName("charset.tapereel");
		GameRegistry.registerItem(magneticTapeItem, "tapeMagnetic");
		GameRegistry.registerItem(tapeReelItem, "tapeReel");

		MultipartRegistry.registerPart(PartTapeDrive.class, "charsetaudio:tapedrive");

		ModCharsetLib.proxy.registerItemModel(partTapeDriveItem, 0, "charsetaudio:tapedrive");
		ModCharsetLib.proxy.registerItemModel(tapeItem, 0, "charsetaudio:tape");
		ModCharsetLib.proxy.registerItemModel(magneticTapeItem, 0, "charsetaudio:tapeMagnetic");
		for (int i = 0; i <= 128; i++) {
			String s = "charsetaudio:tapeReel#inventory";
			if (i >= 112) {
				s += "_4";
			} else if (i >= 48) {
				s += "_3";
			} else if (i >= 16) {
				s += "_2";
			} else if (i > 0) {
				s += "_1";
			}
			ModCharsetLib.proxy.registerItemModel(tapeReelItem, i, s);
		}

		CapabilityManager.INSTANCE.register(IDataStorage.class, new DataStorageStorage(), DataStorageImpl.class);

		ironNoteBlock = new BlockIronNote();
		GameRegistry.registerBlock(ironNoteBlock, "ironnoteblock");
		ModCharsetLib.proxy.registerItemModel(ironNoteBlock, 0, "charsetaudio:ironnoteblock");
	}

	@Mod.EventHandler
	public void init(FMLInitializationEvent event) {
		packet = new PacketRegistry(ModCharsetAudio.MODID);
		packet.registerPacket(0x01, PacketNoteParticle.class);

		packet.registerPacket(0x10, PacketDriveState.class);
		packet.registerPacket(0x11, PacketDriveAudio.class);
		packet.registerPacket(0x12, PacketDriveStop.class);
		packet.registerPacket(0x13, PacketDriveRecord.class);
		packet.registerPacket(0x14, PacketDriveCounter.class);

		GameRegistry.registerTileEntity(TileIronNote.class, "charset:ironnoteblock");

		GameRegistry.addRecipe(new ShapedOreRecipe(new ItemStack(ironNoteBlock), "iii", "iNi", "iii", 'i', "ingotIron", 'N', Blocks.noteblock));

		GameRegistry.addRecipe(new ShapedOreRecipe(new ItemStack(tapeReelItem), " i ", "ipi", " i ", 'i', "ingotIron", 'p', Items.paper));
		GameRegistry.addRecipe(new ShapedOreRecipe(new ItemStack(magneticTapeItem, 32), "ddd", "rir", "ddd", 'd', "dyeBlack", 'r', Items.redstone, 'i', "ingotIron"));
		GameRegistry.addRecipe(new ShapedOreRecipe(new ItemStack(partTapeDriveItem), "igi", "rRr", "ipi", 'g', "blockGlass", 'p', Blocks.piston, 'R', new ItemStack(tapeReelItem, 1, 0), 'r', Items.redstone, 'i', "ingotIron"));

		NetworkRegistry.INSTANCE.registerGuiHandler(this, new GuiHandlerAudio());

		GameRegistry.addRecipe(new RecipeTapeReel());
		RecipeSorter.register("charsetaudio:tapeReel", RecipeTapeReel.class, RecipeSorter.Category.SHAPELESS, "after:minecraft:shapeless");

		GameRegistry.addRecipe(new RecipeTape());
		RecipeSorter.register("charsetaudio:tape", RecipeTape.class, RecipeSorter.Category.SHAPED, "after:minecraft:shaped");

		proxy.init();
	}

	@Mod.EventHandler
	public void postInit(FMLPostInitializationEvent event) {
		if (Loader.isModLoaded("NotEnoughCodecs")) {
			logger.info("NotEnoughCodecs present, MP3 and MP4 support available");
		}
	}

	@Mod.EventHandler
	public void serverStart(FMLServerAboutToStartEvent event) {
		storage = new DataStorageManager();
	}

	@Mod.EventHandler
	public void serverStop(FMLServerStoppedEvent event) {
		storage = null;
		proxy.onServerStop();
	}
}
