package mcvmcomputers.client.gui;

import java.awt.Color;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

import org.apache.commons.lang3.SystemUtils;
import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import mcvmcomputers.client.ClientMod;
import mcvmcomputers.entities.EntityPC;
import mcvmcomputers.item.ItemHarddrive;
import mcvmcomputers.item.ItemList;
import mcvmcomputers.networking.PacketList;
import mcvmcomputers.utils.MVCUtils;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Language;
import net.minecraft.util.math.RotationAxis;
import org.joml.Quaternionf;

/**
 * The PC case editing screen. Opens when the owner clicks their computer; lets
 * them install/remove parts (motherboard, CPU, RAM, GPU, hard drive), insert/eject
 * an ISO and power the VM on or off. The case open/close animation is purely visual.
 */
public class GuiPCEditing extends Screen{
	private float introScale;
	private float panelX;
	private EntityPC pc_case;
	private boolean openCase;
	private MinecraftClient minecraft;
	private long lastEjectCheckTime = 0;

	private final Language lang = Language.getInstance();

	private static final ItemStack CASE_NO_PANEL = new ItemStack(ItemList.PC_CASE_NO_PANEL);
	private static final ItemStack CASE_ONLY_PANEL = new ItemStack(ItemList.PC_CASE_ONLY_PANEL);
	private static final ItemStack CASE_ONLY_GLASS_PANEL = new ItemStack(ItemList.PC_CASE_GLASS_PANEL);
	private static final ItemStack MOBO = new ItemStack(ItemList.ITEM_MOTHERBOARD);
	private static final ItemStack CPU = new ItemStack(ItemList.ITEM_CPU2);
	private static final ItemStack GPU = new ItemStack(ItemList.ITEM_GPU);
	private static final ItemStack RAM = new ItemStack(ItemList.ITEM_RAM1G);
	private static final ItemStack HARD_DRIVE = new ItemStack(ItemList.ITEM_HARDDRIVE);


	public GuiPCEditing(EntityPC pc_case) {
		super(Text.translatable("text.pc_editor.title"));
		this.pc_case = pc_case;
		minecraft = MinecraftClient.getInstance();
	}

	public void renderBackgroundAndMobo(DrawContext context) {
		context.fillGradient(0, 0, this.width, this.height, new Color(0f,0f,0f,Math.max(0.5f*introScale,0)).getRGB(), new Color(0f,0f,0f,0.5f*introScale).getRGB());
		context.getMatrices().push();
		RenderSystem.enableBlend();
		RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		context.getMatrices().translate((this.width / 2), (this.height / 2)-40, 100.0F);
		context.getMatrices().multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-90f*introScale));
		context.getMatrices().multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-6f*introScale*0.1f));
		context.getMatrices().multiply(RotationAxis.POSITIVE_Z.rotationDegrees(6f*introScale*0.1f));
		context.getMatrices().scale(1.0F, -1.0F, 1.0F);
		context.getMatrices().scale(introScale, introScale, introScale);
		context.getMatrices().scale(230.0F, 230.0F, 230.0F);

		RenderSystem.enableDepthTest();
		renderItem(CASE_NO_PANEL, context);


		RenderSystem.disableDepthTest();
		context.getMatrices().push();
		context.getMatrices().multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-90f));
		context.getMatrices().multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-90f));
		context.getMatrices().scale(0.55f, 0.55f, 0.55f);
		context.getMatrices().translate(0.06f, 0.28f, -0.29f);
		if(pc_case.getMotherboardInstalled()) {
			renderItem(MOBO, context);
		}
		if(pc_case.getGpuInstalled()) {
			context.getMatrices().push();
			context.getMatrices().translate(0.24, 0.07f, -0.28f);
			renderItem(GPU, context);
			context.getMatrices().pop();
		}
		if(pc_case.getCpuDividedBy() > 0) {
			context.getMatrices().push();
			context.getMatrices().translate(0.06, 0.12f, 0.06f);
			renderItem(CPU, context);
			context.getMatrices().pop();
		}
		if(pc_case.getGigsOfRamInSlot0() > 0) {
			context.getMatrices().push();
			context.getMatrices().multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90f));
			context.getMatrices().translate(-0.22f, 0.1f, -0.285f);
			renderItem(RAM, context);
			context.getMatrices().pop();
		}
		if(pc_case.getGigsOfRamInSlot1() > 0) {
			context.getMatrices().push();
			context.getMatrices().multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90f));
			context.getMatrices().translate(-0.22f, 0.1f, -0.404f);
			renderItem(RAM, context);
			context.getMatrices().pop();
		}
		if(!pc_case.getHardDriveFileName().isEmpty()) {
			context.getMatrices().push();
			context.getMatrices().multiply(RotationAxis.POSITIVE_X.rotationDegrees(90f));
			context.getMatrices().multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90f));
			context.getMatrices().translate(-0.2f, 0, -0.6f);
			renderItem(HARD_DRIVE, context);
			context.getMatrices().pop();
		}
		context.getMatrices().pop();

		RenderSystem.enableDepthTest();
		context.getMatrices().push();
		context.getMatrices().translate(0, -panelX, 0);
		if(pc_case.getGlassSidepanel()) {
			renderItem(CASE_ONLY_GLASS_PANEL, context);
		}else{
			renderItem(CASE_ONLY_PANEL, context);
		}
		context.getMatrices().pop();
		context.getMatrices().pop();
	}

	private void renderItem(ItemStack stack, DrawContext context) {
		BakedModel mdll = minecraft.getItemRenderer().getModel(stack, null, null, 0);
		VertexConsumerProvider.Immediate immediatee = MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers();
		boolean ble = !mdll.isSideLit();
		if (ble) {
			DiffuseLighting.disableGuiDepthLighting();
		}
		this.minecraft.getItemRenderer().renderItem(stack, ModelTransformationMode.NONE, false, context.getMatrices(), immediatee, 15728640, OverlayTexture.DEFAULT_UV, mdll);
		immediatee.draw();
		if (ble) DiffuseLighting.enableGuiDepthLighting();
	}


	private void addMotherboard(boolean sixtyFour) {
		PacketByteBuf b = PacketByteBufs.create();
		b.writeBoolean(sixtyFour);
		b.writeInt(this.pc_case.getId());
		ClientPlayNetworking.send(PacketList.C2S_ADD_MOBO, b);
	}

	private void removeMotherboard() {
		PacketByteBuf b = PacketByteBufs.create();
		b.writeInt(this.pc_case.getId());
		ClientPlayNetworking.send(PacketList.C2S_REMOVE_MOBO, b);
	}

	private void addCPU(Item cpuItem, int dividedBy) {
		PacketByteBuf b = PacketByteBufs.create();
		b.writeInt(dividedBy);
		b.writeInt(this.pc_case.getId());
		ClientPlayNetworking.send(PacketList.C2S_ADD_CPU, b);
	}

	private void addGPU() {
		PacketByteBuf b = PacketByteBufs.create();
		b.writeInt(this.pc_case.getId());
		ClientPlayNetworking.send(PacketList.C2S_ADD_GPU, b);
	}

	private void addHardDrive(String fileName) {
		PacketByteBuf b = PacketByteBufs.create();
		b.writeString(fileName);
		b.writeInt(this.pc_case.getId());
		ClientPlayNetworking.send(PacketList.C2S_ADD_HARD_DRIVE, b);
	}

	private void removeHardDrive() {
		PacketByteBuf b = PacketByteBufs.create();
		b.writeInt(this.pc_case.getId());
		ClientPlayNetworking.send(PacketList.C2S_REMOVE_HARD_DRIVE, b);
	}

	private void addRamStick(Item ramItem, int megs) {
		PacketByteBuf b = PacketByteBufs.create();
		b.writeInt(megs);
		b.writeInt(this.pc_case.getId());
		ClientPlayNetworking.send(PacketList.C2S_ADD_RAM, b);
	}

	private void removeRamStick(int slot) {
		PacketByteBuf b = PacketByteBufs.create();
		b.writeInt(slot);
		b.writeInt(this.pc_case.getId());
		ClientPlayNetworking.send(PacketList.C2S_REMOVE_RAM, b);
	}
	private void removeCPU() {
		PacketByteBuf b = PacketByteBufs.create();
		b.writeInt(this.pc_case.getId());
		ClientPlayNetworking.send(PacketList.C2S_REMOVE_CPU, b);
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		if(introScale > 0.92f && openCase)
			panelX = MVCUtils.lerp(panelX, 8, delta/20f);
		if(!openCase) {
			panelX = MVCUtils.lerp(panelX, 0, delta/1.5f);
		}
		introScale = MVCUtils.lerp(introScale, 1f, delta/4f);
		this.renderBackgroundAndMobo(context);
		this.clearChildren();
		if(introScale > 0.99f) {
			if(openCase) {
				if((ClientMod.vmTurningOn || ClientMod.vmTurnedOn) && ClientMod.vmEntityID == pc_case.getId()) {
					openCase = false;
				}
				if(SystemUtils.IS_OS_MAC) {
					context.drawTextWithShadow(this.textRenderer, lang.get("mcvmcomputers.pc_editing.put_panel_back_mac"), 4, 14, -1);
					if(GLFW.glfwGetKey(minecraft.getWindow().getHandle(), GLFW.GLFW_KEY_RIGHT_ALT) == GLFW.GLFW_PRESS) {
						openCase = false;
						panelX = -panelX;
					}
				}else {
					context.drawTextWithShadow(this.textRenderer, lang.get("mcvmcomputers.pc_editing.put_panel_back"), 4, 14, -1);
					if(GLFW.glfwGetKey(minecraft.getWindow().getHandle(), GLFW.GLFW_KEY_RIGHT_CONTROL) == GLFW.GLFW_PRESS) {
						openCase = false;
						panelX = -panelX;
					}
				}
				if(pc_case.getMotherboardInstalled()) {
					this.addDrawableChild(ButtonWidget.builder(Text.literal("x"), (btn) -> this.removeMotherboard()).dimensions(this.width/2-70, this.height / 2 - 70, 10, 10).build());
					RenderSystem.disableDepthTest();
					context.getMatrices().push();
					context.getMatrices().translate(0, 0, 200);
					if(pc_case.get64Bit()) {
						context.drawTextWithShadow(this.textRenderer, lang.get("mcvmcomputers.64bit"), this.width/2 - 66, this.height/2 - 56, -1);
					}else {
						context.drawTextWithShadow(this.textRenderer, lang.get("mcvmcomputers.32bit"), this.width/2 - 66, this.height/2 - 56, -1);
					}
					context.getMatrices().pop();
					RenderSystem.enableDepthTest();
					if(pc_case.getCpuDividedBy() == 0) {
						RenderSystem.disableDepthTest();
						context.getMatrices().push();
						context.getMatrices().translate(0, 0, 200);
						context.drawTextWithShadow(this.textRenderer, lang.get("mcvmcomputers.pc_editing.add_cpu"), this.width/2 - 120, this.height/2 - 40, -1);
						context.getMatrices().pop();
						RenderSystem.enableDepthTest();
						int addCpuWidth = textRenderer.getWidth(lang.get("mcvmcomputers.pc_editing.add_cpu_btn").replace("%s", "6"));
						ButtonWidget div2 = ButtonWidget.builder(Text.literal(lang.get("mcvmcomputers.pc_editing.add_cpu_btn").replace("%s", "2")), (btn) -> this.addCPU(ItemList.ITEM_CPU2, 2)).dimensions(this.width/2 - (addCpuWidth+59), this.height / 2 - 31, addCpuWidth+4, 12).build();
						ButtonWidget div4 = ButtonWidget.builder(Text.literal(lang.get("mcvmcomputers.pc_editing.add_cpu_btn").replace("%s", "4")), (btn) -> this.addCPU(ItemList.ITEM_CPU4, 4)).dimensions(this.width/2 - (addCpuWidth+59), this.height / 2 - 18, addCpuWidth+4, 12).build();
						ButtonWidget div6 = ButtonWidget.builder(Text.literal(lang.get("mcvmcomputers.pc_editing.add_cpu_btn").replace("%s", "6")), (btn) -> this.addCPU(ItemList.ITEM_CPU6, 6)).dimensions(this.width/2 - (addCpuWidth+59), this.height / 2 - 5, addCpuWidth+4, 12).build();
						if(!minecraft.player.getInventory().contains(new ItemStack(ItemList.ITEM_CPU2))) {
							div2.active = false;
						}
						if(!minecraft.player.getInventory().contains(new ItemStack(ItemList.ITEM_CPU4))) {
							div4.active = false;
						}
						if(!minecraft.player.getInventory().contains(new ItemStack(ItemList.ITEM_CPU6))) {
							div6.active = false;
						}
						this.addDrawableChild(div2);
						this.addDrawableChild(div4);
						this.addDrawableChild(div6);
					}else {
						this.addDrawableChild(ButtonWidget.builder(Text.literal("x"), (btn) -> this.removeCPU()).dimensions(this.width/2-43, this.height / 2 - 16, 10, 10).build());
						RenderSystem.disableDepthTest();
						context.getMatrices().push();
						context.getMatrices().translate(0, 0, 200);
						context.drawTextWithShadow(this.textRenderer, "1/" + pc_case.getCpuDividedBy(), this.width/2-25, this.height/2+2, -1);
						context.getMatrices().pop();
						RenderSystem.enableDepthTest();
					}
					if(!pc_case.getGpuInstalled()) {
						int addGpuWidth = this.textRenderer.getWidth(lang.get("mcvmcomputers.pc_editing.add_gpu"));
						ButtonWidget bw = ButtonWidget.builder(Text.literal(lang.get("mcvmcomputers.pc_editing.add_gpu")), (btn) -> this.addGPU()).dimensions(this.width/2 - 64, this.height / 2 + 33, addGpuWidth+4, 12).build();
						if(!minecraft.player.getInventory().contains(new ItemStack(ItemList.ITEM_GPU)))
							bw.active = false;
						this.addDrawableChild(bw);
					}
					if(pc_case.getHardDriveFileName().isEmpty()) {
						int lastYOffset = 0;
						int xOffCount = 0;
						int lastXOffset = 0;
						int count = 0;
						RenderSystem.disableDepthTest();
						context.getMatrices().push();
						context.getMatrices().translate(0, 0, 200);
						context.drawTextWithShadow(this.textRenderer, lang.get("mcvmcomputers.pc_editing.add_vhds"), this.width/2 + 20, this.height/2 + 30, -1);
						context.getMatrices().pop();
						RenderSystem.enableDepthTest();
						for(ItemStack is : minecraft.player.getInventory().main) {
							if(is.getItem() instanceof ItemHarddrive) {
								if(is.getNbt() != null){
									if(is.getNbt().contains("vhdfile")) {
		    							String file = is.getNbt().getString("vhdfile");
		    							if(new File(ClientMod.vhdDirectory, file).exists()) {
		    								int w = Math.max(50, this.textRenderer.getWidth(file)+4);
		    								this.addDrawableChild(ButtonWidget.builder(Text.literal(file), (btn) -> this.addHardDrive(file)).dimensions(this.width/2 + 20 + lastXOffset, this.height / 2 + 40 + lastYOffset, Math.max(50, this.textRenderer.getWidth(file)+4), 12).build());
		    								lastXOffset += w+1;
											xOffCount += 1;
											if(xOffCount >= 3) {
												xOffCount = 0;
												lastXOffset = 0;
												lastYOffset += 13;
											}
											count++;
		    							}
									}
								}
							}
						}
						if(count == 0) {
							RenderSystem.disableDepthTest();
							context.getMatrices().push();
							context.getMatrices().translate(0, 0, 200);
							context.drawTextWithShadow(this.textRenderer, (char) (0xfeff00a7) + "7" + lang.get("mcvmcomputers.pc_editing.no_valid_vhd"), this.width/2 + 20, this.height/2 + 40, -1);
							context.getMatrices().pop();
							RenderSystem.enableDepthTest();
						}
					}else {
						this.addDrawableChild(ButtonWidget.builder(Text.literal("x"), (btn) -> this.removeHardDrive()).dimensions(this.width/2+30, this.height / 2 + 55, 10, 10).build());
						RenderSystem.disableDepthTest();
						context.getMatrices().push();
						context.getMatrices().translate(0, 0, 200);
						context.drawTextWithShadow(this.textRenderer, pc_case.getHardDriveFileName(), this.width/2+45, this.height/2+65, -1);
						context.getMatrices().pop();
						RenderSystem.enableDepthTest();
					}
					if(pc_case.getGigsOfRamInSlot0() == 0 || pc_case.getGigsOfRamInSlot1() == 0) {
						RenderSystem.disableDepthTest();
						context.getMatrices().push();
						context.getMatrices().translate(0, 0, 200);
						context.drawTextWithShadow(this.textRenderer, lang.get("mcvmcomputers.pc_editing.add_ram"), this.width/2 + 50, this.height/2 - 75, -1);
						context.getMatrices().pop();
						RenderSystem.enableDepthTest();
						int addMBRamWidth = textRenderer.getWidth(lang.get("mcvmcomputers.pc_editing.add_mbram_btn").replace("%s", "512"))+4;
						ButtonWidget sixfourM = ButtonWidget.builder(Text.literal(lang.get("mcvmcomputers.pc_editing.add_mbram_btn").replace("%s", "64")), (btn) -> this.addRamStick(ItemList.ITEM_RAM64M, 64)).dimensions(this.width/2 + 50, this.height / 2 - 64, addMBRamWidth, 12).build();
						ButtonWidget oneM = ButtonWidget.builder(Text.literal(lang.get("mcvmcomputers.pc_editing.add_mbram_btn").replace("%s", "128")), (btn) -> this.addRamStick(ItemList.ITEM_RAM128M, 128)).dimensions(this.width/2 + 50, this.height / 2 - 51, addMBRamWidth, 12).build();
						ButtonWidget twoM = ButtonWidget.builder(Text.literal(lang.get("mcvmcomputers.pc_editing.add_mbram_btn").replace("%s", "256")), (btn) -> this.addRamStick(ItemList.ITEM_RAM256M, 256)).dimensions(this.width/2 + 50, this.height / 2 - 38, addMBRamWidth, 12).build();
						ButtonWidget fiveM = ButtonWidget.builder(Text.literal(lang.get("mcvmcomputers.pc_editing.add_mbram_btn").replace("%s", "512")), (btn) -> this.addRamStick(ItemList.ITEM_RAM512M, 512)).dimensions(this.width/2 + 50, this.height / 2 - 25, addMBRamWidth, 12).build();
						ButtonWidget oneG = ButtonWidget.builder(Text.literal(lang.get("mcvmcomputers.pc_editing.add_ram_btn").replace("%s", "1")), (btn) -> this.addRamStick(ItemList.ITEM_RAM1G, 1024)).dimensions(this.width/2 + 50, this.height / 2 - 12, addMBRamWidth, 12).build();
						ButtonWidget twoG = ButtonWidget.builder(Text.literal(lang.get("mcvmcomputers.pc_editing.add_ram_btn").replace("%s", "2")), (btn) -> this.addRamStick(ItemList.ITEM_RAM2G, 2048)).dimensions(this.width/2 + 50, this.height / 2 + 1, addMBRamWidth, 12).build();
						ButtonWidget fourG = ButtonWidget.builder(Text.literal(lang.get("mcvmcomputers.pc_editing.add_ram_btn").replace("%s", "4")), (btn) -> this.addRamStick(ItemList.ITEM_RAM4G, 4096)).dimensions(this.width/2 + 50, this.height / 2 + 14, addMBRamWidth, 12).build();
						if(!minecraft.player.getInventory().contains(new ItemStack(ItemList.ITEM_RAM64M))) {
							sixfourM.active = false;
						}
						if(!minecraft.player.getInventory().contains(new ItemStack(ItemList.ITEM_RAM128M))) {
							oneM.active = false;
						}
						if(!minecraft.player.getInventory().contains(new ItemStack(ItemList.ITEM_RAM256M))) {
							twoM.active = false;
						}
						if(!minecraft.player.getInventory().contains(new ItemStack(ItemList.ITEM_RAM512M))) {
							fiveM.active = false;
						}
						if(!minecraft.player.getInventory().contains(new ItemStack(ItemList.ITEM_RAM1G))) {
							oneG.active = false;
						}
						if(!minecraft.player.getInventory().contains(new ItemStack(ItemList.ITEM_RAM2G))) {
							twoG.active = false;
						}
						if(!minecraft.player.getInventory().contains(new ItemStack(ItemList.ITEM_RAM4G))) {
							fourG.active = false;
						}
						this.addDrawableChild(sixfourM);
						this.addDrawableChild(oneM);
						this.addDrawableChild(twoM);
						this.addDrawableChild(fiveM);
						this.addDrawableChild(oneG);
						this.addDrawableChild(twoG);
						this.addDrawableChild(fourG);
					}
					if(pc_case.getGigsOfRamInSlot0() > 0) {
						RenderSystem.disableDepthTest();
						context.getMatrices().push();
						context.getMatrices().translate(0, 0, 200);
						if(pc_case.getGigsOfRamInSlot0() < 1000 && pc_case.getGigsOfRamInSlot0() >= 100) {
							context.drawTextWithShadow(this.textRenderer, (int) pc_case.getGigsOfRamInSlot0() + " MB", this.width/2+4, this.height/2+2, -1);
						}else if(pc_case.getGigsOfRamInSlot0() < 100) {
							context.drawTextWithShadow(this.textRenderer, (int) pc_case.getGigsOfRamInSlot0() + " MB", this.width/2+10, this.height/2+2, -1);
						}else {
							context.drawTextWithShadow(this.textRenderer, (int) pc_case.getGigsOfRamInSlot0()/1024 + " GB", this.width / 2 + 16, this.height / 2 + 2, -1);
						}
						context.getMatrices().pop();
						RenderSystem.enableDepthTest();
						this.addDrawableChild(ButtonWidget.builder(Text.literal("x"), (btn) -> this.removeRamStick(0)).dimensions(this.width/2+21, this.height / 2 - 70, 10, 10).build());
					}
					if(pc_case.getGigsOfRamInSlot1() > 0) {
						RenderSystem.disableDepthTest();
						context.getMatrices().push();
						context.getMatrices().translate(0, 0, 200);
						if(pc_case.getGigsOfRamInSlot1() < 1000) {
							context.drawTextWithShadow(this.textRenderer, (int) pc_case.getGigsOfRamInSlot1() + " MB", this.width/2+42, this.height/2+2, -1);
						}else {
							context.drawTextWithShadow(this.textRenderer, (int) pc_case.getGigsOfRamInSlot1()/1024 + " GB", this.width / 2+42, this.height / 2+2, -1);
						}
						context.getMatrices().pop();
						RenderSystem.enableDepthTest();
						this.addDrawableChild(ButtonWidget.builder(Text.literal("x"), (btn) -> this.removeRamStick(1)).dimensions(this.width/2 + 37, this.height / 2 - 70, 10, 10).build());
					}
				}else {
					int thirtytwow = textRenderer.getWidth(lang.get("mcvmcomputers.pc_editing.add_32bit_mobo"))+4;
					ButtonWidget thirtytwo = ButtonWidget.builder(Text.literal(lang.get("mcvmcomputers.pc_editing.add_32bit_mobo")), (btn) -> this.addMotherboard(false)).dimensions(this.width/2 - (thirtytwow/2), this.height / 2 - 23, thirtytwow, 14).build();
					int sixtyfourw = textRenderer.getWidth(lang.get("mcvmcomputers.pc_editing.add_64bit_mobo"))+4;
					ButtonWidget sixtyfour = ButtonWidget.builder(Text.literal(lang.get("mcvmcomputers.pc_editing.add_64bit_mobo")), (btn) -> this.addMotherboard(true)).dimensions(this.width/2 - (sixtyfourw/2), this.height / 2 - 7, sixtyfourw, 14).build();

					if(!minecraft.player.getInventory().contains(new ItemStack(ItemList.ITEM_MOTHERBOARD))) {
						thirtytwo.active = false;
					}
					if(!minecraft.player.getInventory().contains(new ItemStack(ItemList.ITEM_MOTHERBOARD64))) {
						sixtyfour.active = false;
					}

					this.addDrawableChild(thirtytwo);
					this.addDrawableChild(sixtyfour);
				}
			}else {
				boolean turnedOn = (ClientMod.vmTurningOn && ClientMod.vmEntityID == pc_case.getId()) || (ClientMod.vmTurnedOn && ClientMod.vmEntityID == pc_case.getId());
				if(turnedOn) {
					int buttonW = textRenderer.getWidth(lang.get("mcvmcomputers.pc_editing.turn_off"))+4;
					this.addDrawableChild(ButtonWidget.builder(Text.literal(lang.get("mcvmcomputers.pc_editing.turn_off")), (btn) -> this.turnOffPC(btn)).dimensions((this.width/2 + 103) - buttonW, this.height / 2 - 80, buttonW, 12).build());
				}else {
					int buttonW = textRenderer.getWidth(lang.get("mcvmcomputers.pc_editing.turn_on"))+4;
					this.addDrawableChild(ButtonWidget.builder(Text.literal(lang.get("mcvmcomputers.pc_editing.turn_on")), (btn) -> this.turnOnPC(btn)).dimensions((this.width/2 + 103) - buttonW, this.height / 2 - 80, buttonW, 12).build());
				}

				if(ClientMod.vmTurnedOn && !pc_case.getIsoFileName().isEmpty()) {
					try {
						long now = System.currentTimeMillis();
						if (now - lastEjectCheckTime > 2000) {
							lastEjectCheckTime = now;
							if(ClientMod.vbox.isMediumEjected("VmComputersVm", "IDE Controller", 1, 0)) {
								this.removeISO();
							}
						}
					}catch(Exception e) {}
				}

				if(pc_case.getIsoFileName().isEmpty()) {
					RenderSystem.disableDepthTest();
					context.getMatrices().push();
					context.getMatrices().translate(0, 0, 200);
					context.drawTextWithShadow(this.textRenderer, lang.get("mcvmcomputers.pc_editing.select_iso"), this.width/2 - 75, this.height/2 - 75, -1);
					context.getMatrices().pop();
					RenderSystem.enableDepthTest();
					int offX = 0;
					int offY = 0;
					
					File dir = ClientMod.isoDirectory;
					if (dir != null && dir.exists() && dir.isDirectory()) {
					    File[] files = dir.listFiles();
					    if (files != null) {
					        for (File f : files) {
								if(f.getName().endsWith(".iso")) {
									if((this.width/2 - 75 + offX) + this.textRenderer.getWidth(f.getName())+10 > this.width/2 + 105) {
										offX = 0;
										offY += 14;
									}
									this.addDrawableChild(ButtonWidget.builder(Text.literal(f.getName()), (btn) -> insertISO(f.getName())).dimensions(this.width/2 - 75 + offX, this.height / 2 - 62 + offY, this.textRenderer.getWidth(f.getName())+8, 12).build());
									offX += this.textRenderer.getWidth(f.getName())+10;
								}
							}
						}
					}
				}else {
					RenderSystem.disableDepthTest();
					context.getMatrices().push();
					context.getMatrices().translate(0, 0, 200);
					context.drawTextWithShadow(this.textRenderer, lang.get("mcvmcomputers.pc_editing.inserted_iso"), this.width/2 - 75, this.height/2 - 75, -1);
					context.drawTextWithShadow(this.textRenderer, (char) (0xfeff00a7) + "7" + pc_case.getIsoFileName(), this.width/2 - 75, this.height/2 - 65, -1);
					context.getMatrices().pop();
					RenderSystem.enableDepthTest();
					int ejectW = textRenderer.getWidth(lang.get("mcvmcomputers.pc_editing.eject"));
					this.addDrawableChild(ButtonWidget.builder(Text.literal(lang.get("mcvmcomputers.pc_editing.eject")), (btn) -> removeISO()).dimensions(this.width/2 - 75, this.height / 2 - 50, ejectW+4, 12).build());
				}
				int openCaseW = textRenderer.getWidth(lang.get("mcvmcomputers.pc_editing.open_case"));
				ButtonWidget bw = ButtonWidget.builder(Text.literal(lang.get("mcvmcomputers.pc_editing.open_case")), (btn) -> openCase = true).dimensions(this.width/2 - 82, this.height / 2 + 65, openCaseW+4, 12).build();
				bw.active = !turnedOn;
				this.addDrawableChild(bw);
			}
		}
		RenderSystem.disableDepthTest();
		context.getMatrices().translate(0, 0, 200);
		super.render(context, mouseX, mouseY, delta);
		context.drawTextWithShadow(this.textRenderer, lang.get("mcvmcomputers.pc_editing.close"), 4, 4, -1);
		RenderSystem.enableDepthTest();
	}

	private void removeISO() {
		if((ClientMod.vmTurningOn || ClientMod.vmTurnedOn) && ClientMod.vmEntityID == pc_case.getId()) {
			try {
				ClientMod.vbox.unmountMedium("VmComputersVm", "IDE Controller", 1, 0);
			}catch(Exception ex) {}
		}
		PacketByteBuf b = PacketByteBufs.create();
		b.writeInt(this.pc_case.getId());
		ClientPlayNetworking.send(PacketList.C2S_REMOVE_ISO, b);
	}

	private void insertISO(String name) {
		String isoPath = new File(ClientMod.isoDirectory, name).getPath();
		if(ClientMod.vmTurnedOn && ClientMod.vmEntityID == pc_case.getId()) {
			ClientMod.vbox.mountMedium("VmComputersVm", "IDE Controller", 1, 0, isoPath);
		}
		if(ClientMod.vmTurningOn && ClientMod.vmEntityID == pc_case.getId()) {
			minecraft.player.sendMessage(Text.translatable("mcvmcomputers.waitingforvmtostart").formatted(Formatting.YELLOW), false);
			synchronized (ClientMod.VM_TURNING_ON_LOCK) {
				try {
					while(ClientMod.vmTurningOn && ClientMod.vmEntityID == pc_case.getId()) {
						ClientMod.VM_TURNING_ON_LOCK.wait(5000);
					}
				} catch (InterruptedException e) {}
				ClientMod.vbox.mountMedium("VmComputersVm", "IDE Controller", 1, 0, isoPath);
			}
		}
		PacketByteBuf b = PacketByteBufs.create();
		b.writeString(name);
		b.writeInt(this.pc_case.getId());
		ClientPlayNetworking.send(PacketList.C2S_ADD_ISO, b);
	}

	public void turnOffPC(ButtonWidget wdgt) {
		ClientMod.vmTurningOff = true;
		ClientMod.vmTurnedOn = false;
		PacketByteBuf b = PacketByteBufs.create();
		ClientPlayNetworking.send(PacketList.C2S_TURN_OFF_PC, b);
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					while(ClientMod.vmTurningOn) {
						Thread.sleep(50);
					}
				} catch (InterruptedException e) {

				}
				ClientMod.vbox.powerOffVm("VmComputersVm");

				ClientMod.vmTurnedOn = false;
				ClientMod.vmTurningOff = false;
				ClientMod.vmEntityID = -1;
			}
		}, "Turn off PC").start();
	}

	public void turnOnPC(ButtonWidget wdgt) {
		if(pc_case.getCpuDividedBy() > 0 && pc_case.getGpuInstalled() && pc_case.getMotherboardInstalled() && (pc_case.getGigsOfRamInSlot0() + pc_case.getGigsOfRamInSlot1()) >= 1) {
			if(!pc_case.getHardDriveFileName().isEmpty()) {
				if(!new File(ClientMod.vhdDirectory, pc_case.getHardDriveFileName()).exists()) {
					minecraft.player.sendMessage(Text.translatable("mcvmcomputers.hdd_doesnt_exist").formatted(Formatting.RED), false);
					return;
				}
			}
			if(!pc_case.getIsoFileName().isEmpty()) {
				if(!pc_case.getIsoFileName().equals("Additions") && !new File(ClientMod.isoDirectory, pc_case.getIsoFileName()).exists()) {
					minecraft.player.sendMessage(Text.translatable("mcvmcomputers.iso_doesnt_exist").formatted(Formatting.RED), false);
					return;
				}
			}

			if(ClientMod.vmTurningOn || ClientMod.vmTurnedOn) {
				return;
			}
			ClientMod.vmTurningOn = true;
			ClientMod.vmEntityID = pc_case.getId();
			PacketByteBuf b = PacketByteBufs.create();
			b.writeInt(pc_case.getId());
			ClientPlayNetworking.send(PacketList.C2S_TURN_ON_PC, b);
			new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						boolean vmExists = ClientMod.vbox.vmExists("VmComputersVm");

						if(vmExists) {
							String state = ClientMod.vbox.getVmState("VmComputersVm");

							if("running".equals(state) || "firstonline".equals(state) || "starting".equals(state)) {
								ClientMod.vbox.powerOffVm("VmComputersVm");
							}
							if("saved".equals(state)) {
								ClientMod.vbox.discardSavedState("VmComputersVm");
							}


							String OSType = "Other";
							if(pc_case.get64Bit()) {
								OSType += "_64";
							}
							long ramMB = Math.min(ClientMod.maxRam, (pc_case.getGigsOfRamInSlot0() + pc_case.getGigsOfRamInSlot1()));
							int cpuCount = Math.max(1, ClientMod.vbox.getHostProcessorCount() / pc_case.getCpuDividedBy());

							ClientMod.vbox.modifyVmWith3dAccelFallback("VmComputersVm",
									"--ostype", OSType,
									"--memory", String.valueOf(ramMB),
									"--cpus", String.valueOf(cpuCount),
									"--vram", String.valueOf(ClientMod.videoMem),
									"--accelerate3d", "on");

							ClientMod.vbox.modifyVmSilent("VmComputersVm", "--accelerate2dvideo", "on");


							ClientMod.vbox.removeStorageController("VmComputersVm", "IDE Controller");
							ClientMod.vbox.addStorageController("VmComputersVm", "IDE Controller", "ide");

							if(!pc_case.getHardDriveFileName().isEmpty()) {
								if(new File(ClientMod.vhdDirectory, pc_case.getHardDriveFileName()).exists()) {
									String hddPath = new File(ClientMod.vhdDirectory, pc_case.getHardDriveFileName()).getPath();
									ClientMod.vbox.storageAttachSilent("VmComputersVm", "IDE Controller", 0, 0, "hdd", hddPath);
								}
							}
							if(!pc_case.getIsoFileName().isEmpty()) {
								if(pc_case.getIsoFileName().equals("Additions")) {
									String additionsPath = ClientMod.vbox.getDefaultAdditionsISO();
									if(additionsPath != null) {
										ClientMod.vbox.storageAttachSilent("VmComputersVm", "IDE Controller", 1, 0, "dvddrive", additionsPath);
									}
								}else if(new File(ClientMod.isoDirectory, pc_case.getIsoFileName()).exists()) {
									String isoPath = new File(ClientMod.isoDirectory, pc_case.getIsoFileName()).getPath();
									ClientMod.vbox.storageAttachSilent("VmComputersVm", "IDE Controller", 1, 0, "dvddrive", isoPath);
								}
							}
							if(pc_case.getIsoFileName().isEmpty()) {
								ClientMod.vbox.storageAttachSilent("VmComputersVm", "IDE Controller", 1, 0, "dvddrive", "emptydrive");
							}
						}else {

							String OSType = "Other";
							if(pc_case.get64Bit()) {
								OSType += "_64";
							}
							ClientMod.vbox.createVm("VmComputersVm", OSType);

							long ramMB = Math.min(ClientMod.maxRam, (pc_case.getGigsOfRamInSlot0() + pc_case.getGigsOfRamInSlot1()));
							int cpuCount = Math.max(1, ClientMod.vbox.getHostProcessorCount() / pc_case.getCpuDividedBy());

							ClientMod.vbox.modifyVmWith3dAccelFallback("VmComputersVm",
									"--memory", String.valueOf(ramMB),
									"--cpus", String.valueOf(cpuCount),
									"--vram", String.valueOf(ClientMod.videoMem),
									"--accelerate3d", "on");
							ClientMod.vbox.modifyVmSilent("VmComputersVm", "--accelerate2dvideo", "on");

							ClientMod.vbox.addStorageController("VmComputersVm", "IDE Controller", "ide");

							if(!pc_case.getHardDriveFileName().isEmpty()) {
								if(new File(ClientMod.vhdDirectory, pc_case.getHardDriveFileName()).exists()) {
									String hddPath = new File(ClientMod.vhdDirectory, pc_case.getHardDriveFileName()).getPath();
									ClientMod.vbox.storageAttachSilent("VmComputersVm", "IDE Controller", 0, 0, "hdd", hddPath);
								}
							}
							if(!pc_case.getIsoFileName().isEmpty()) {
								if(pc_case.getIsoFileName().equals("Additions")) {
									String additionsPath = ClientMod.vbox.getDefaultAdditionsISO();
									if(additionsPath != null) {
										ClientMod.vbox.storageAttachSilent("VmComputersVm", "IDE Controller", 1, 0, "dvddrive", additionsPath);
									}
								}else if(new File(ClientMod.isoDirectory, pc_case.getIsoFileName()).exists()) {
									String isoPath = new File(ClientMod.isoDirectory, pc_case.getIsoFileName()).getPath();
									ClientMod.vbox.storageAttachSilent("VmComputersVm", "IDE Controller", 1, 0, "dvddrive", isoPath);
								}
							}
							if(pc_case.getIsoFileName().isEmpty()) {
								ClientMod.vbox.storageAttachSilent("VmComputersVm", "IDE Controller", 1, 0, "dvddrive", "emptydrive");
							}
						}


						ClientMod.vbox.startVm("VmComputersVm");

						ClientMod.vmTurningOn = false;
						ClientMod.vmTurnedOn = true;
						synchronized (ClientMod.VM_TURNING_ON_LOCK) {
							ClientMod.VM_TURNING_ON_LOCK.notifyAll();
						}
					}catch(Exception ex) {
						ex.printStackTrace();
						minecraft.player.sendMessage(Text.translatable("mcvmcomputers.failed_to_start", ex.getMessage()).formatted(Formatting.RED), false);
						minecraft.player.sendMessage(Text.translatable("mcvmcomputers.contact_me").formatted(Formatting.RED), false);
						ClientMod.vmTurningOn = false;
						ClientMod.vmTurnedOn = false;

						PacketByteBuf b = PacketByteBufs.create();
						ClientPlayNetworking.send(PacketList.C2S_TURN_OFF_PC, b);
					}
				}
			}, "Turn on PC").start();
		}
	}

	@Override
	public boolean shouldPause() {
		return false;
	}

}
