package com.mjr.planetprogression.client.gui.screen;

import java.nio.FloatBuffer;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import micdoodle8.mods.galacticraft.api.event.client.CelestialBodyRenderEvent;
import micdoodle8.mods.galacticraft.api.galaxies.CelestialBody;
import micdoodle8.mods.galacticraft.api.galaxies.GalaxyRegistry;
import micdoodle8.mods.galacticraft.api.galaxies.IChildBody;
import micdoodle8.mods.galacticraft.api.galaxies.Moon;
import micdoodle8.mods.galacticraft.api.galaxies.Planet;
import micdoodle8.mods.galacticraft.api.galaxies.Satellite;
import micdoodle8.mods.galacticraft.api.galaxies.SolarSystem;
import micdoodle8.mods.galacticraft.api.galaxies.Star;
import micdoodle8.mods.galacticraft.core.client.gui.screen.GuiCelestialSelection;
import micdoodle8.mods.galacticraft.core.util.PlayerUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.FMLClientHandler;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mjr.planetprogression.client.handlers.capabilities.CapabilityStatsClientHandler;
import com.mjr.planetprogression.client.handlers.capabilities.IStatsClientCapability;

public class CustomGuiCelestialSelection extends GuiCelestialSelection {

	public CustomGuiCelestialSelection(boolean mapMode, List<CelestialBody> possibleBodies, boolean canCreateStations) {
		super(mapMode, possibleBodies, canCreateStations);
	}

	@Override
	public void drawCircles() {
		final Minecraft minecraft = FMLClientHandler.instance().getClient();
		final EntityPlayerSP player = minecraft.thePlayer;
		final EntityPlayerSP playerBaseClient = PlayerUtil.getPlayerBaseClientFromPlayer(player, false);

		IStatsClientCapability stats = null;

		if (player != null) {
			stats = playerBaseClient.getCapability(CapabilityStatsClientHandler.PP_STATS_CLIENT_CAPABILITY, null);
		}

		GL11.glColor4f(1, 1, 1, 1);
		GL11.glLineWidth(3);
		int count = 0;

		final float theta = (float) (2 * Math.PI / 90);
		final float cos = (float) Math.cos(theta);
		final float sin = (float) Math.sin(theta);

		for (Planet planet : GalaxyRegistry.getRegisteredPlanets().values()) {
			if (stats.getUnlockedPlanets().contains(planet)) {
				if (planet.getParentSolarSystem() != null) {
					Vector3f systemOffset = this.getCelestialBodyPosition(planet.getParentSolarSystem().getMainStar());

					float x = this.getScale(planet);
					float y = 0;

					float alpha = 1.0F;

					if ((this.selectedBody instanceof IChildBody && ((IChildBody) this.selectedBody).getParentPlanet() != planet) || (this.selectedBody instanceof Planet && this.selectedBody != planet && this.isZoomed())) {
						if (this.lastSelectedBody == null && !(this.selectedBody instanceof IChildBody) && !(this.selectedBody instanceof Satellite)) {
							alpha = 1.0F - Math.min(this.ticksSinceSelection / 25.0F, 1.0F);
						} else {
							alpha = 0.0F;
						}
					}

					if (alpha != 0) {
						switch (count % 2) {
						case 0:
							GL11.glColor4f(0.0F / 1.4F, 0.6F / 1.4F, 1.0F / 1.4F, alpha / 1.4F);
							break;
						case 1:
							GL11.glColor4f(0.4F / 1.4F, 0.9F / 1.4F, 1.0F / 1.4F, alpha / 1.4F);
							break;
						}

						CelestialBodyRenderEvent.CelestialRingRenderEvent.Pre preEvent = new CelestialBodyRenderEvent.CelestialRingRenderEvent.Pre(planet, systemOffset);
						MinecraftForge.EVENT_BUS.post(preEvent);

						if (!preEvent.isCanceled()) {
							GL11.glTranslatef(systemOffset.x, systemOffset.y, systemOffset.z);

							GL11.glBegin(GL11.GL_LINE_LOOP);

							float temp;
							for (int i = 0; i < 90; i++) {
								GL11.glVertex2f(x, y);

								temp = x;
								x = cos * x - sin * y;
								y = sin * temp + cos * y;
							}

							GL11.glEnd();

							GL11.glTranslatef(-systemOffset.x, -systemOffset.y, -systemOffset.z);

							count++;
						}

						CelestialBodyRenderEvent.CelestialRingRenderEvent.Post postEvent = new CelestialBodyRenderEvent.CelestialRingRenderEvent.Post(planet);
						MinecraftForge.EVENT_BUS.post(postEvent);
					}
				}
			}
		}

		count = 0;

		if (this.selectedBody != null) {
			Vector3f planetPos = this.getCelestialBodyPosition(this.selectedBody);

			if (this.selectedBody instanceof IChildBody) {
				planetPos = this.getCelestialBodyPosition(((IChildBody) this.selectedBody).getParentPlanet());
			} else if (this.selectedBody instanceof Satellite) {
				planetPos = this.getCelestialBodyPosition(((Satellite) this.selectedBody).getParentPlanet());
			}

			GL11.glTranslatef(planetPos.x, planetPos.y, 0);

			for (Moon moon : GalaxyRegistry.getRegisteredMoons().values()) {
				if (stats.getUnlockedPlanets().contains(moon)) {
					if ((moon.getParentPlanet() == this.selectedBody && this.selectionState != EnumSelection.SELECTED) || moon == this.selectedBody || getSiblings(this.selectedBody).contains(moon)) {
						if (this.drawCircle(moon, count, sin, cos)) {
							count++;
						}
					}
				}
			}

			for (Satellite satellite : GalaxyRegistry.getRegisteredSatellites().values()) {
				if (this.possibleBodies != null && this.possibleBodies.contains(satellite)) {
					if ((satellite.getParentPlanet() == this.selectedBody && this.selectionState != EnumSelection.SELECTED) && this.ticksSinceSelection > 24 || satellite == this.selectedBody || this.lastSelectedBody instanceof IChildBody) {
						if (this.drawCircle(satellite, count, sin, cos)) {
							count++;
						}
					}
				}
			}
		}

		GL11.glLineWidth(1);
	}

	@Override
	protected boolean drawCircle(CelestialBody body, int count, float sin, float cos) {
		float x = this.getScale(body);
		float y = 0;

		float alpha = 1;

		if (this.isZoomed()) {
			alpha = this.selectedBody instanceof IChildBody ? 1.0F : Math.min(Math.max((this.ticksSinceSelection - 30) / 15.0F, 0.0F), 1.0F);

			if (this.lastSelectedBody instanceof Moon && body instanceof Moon) {
				if (GalaxyRegistry.getMoonsForPlanet(((Moon) this.lastSelectedBody).getParentPlanet()).contains(body)) {
					alpha = 1.0F;
				}
			} else if (this.lastSelectedBody instanceof Satellite && body instanceof Satellite) {
				if (GalaxyRegistry.getSatellitesForCelestialBody(((Satellite) this.lastSelectedBody).getParentPlanet()).contains(body)) {
					alpha = 1.0F;
				}
			}
		}

		if (alpha != 0) {
			switch (count % 2) {
			case 0:
				GL11.glColor4f(0.0F, 0.6F, 1.0F, alpha);
				break;
			case 1:
				GL11.glColor4f(0.4F, 0.9F, 1.0F, alpha);
				break;
			}

			CelestialBodyRenderEvent.CelestialRingRenderEvent.Pre preEvent = new CelestialBodyRenderEvent.CelestialRingRenderEvent.Pre(body, new Vector3f(0.0F, 0.0F, 0.0F));
			MinecraftForge.EVENT_BUS.post(preEvent);

			if (!preEvent.isCanceled()) {
				GL11.glBegin(GL11.GL_LINE_LOOP);

				float temp;
				for (int i = 0; i < 90; i++) {
					GL11.glVertex2f(x, y);

					temp = x;
					x = cos * x - sin * y;
					y = sin * temp + cos * y;
				}

				GL11.glEnd();
				return true;
			}

			CelestialBodyRenderEvent.CelestialRingRenderEvent.Post postEvent = new CelestialBodyRenderEvent.CelestialRingRenderEvent.Post(body);
			MinecraftForge.EVENT_BUS.post(postEvent);
		}

		return false;
	}

	@Override
	protected List<CelestialBody> getChildren(Object object) {
		List<CelestialBody> bodyList = Lists.newArrayList();
		final Minecraft minecraft = FMLClientHandler.instance().getClient();
		final EntityPlayerSP player = minecraft.thePlayer;
		final EntityPlayerSP playerBaseClient = PlayerUtil.getPlayerBaseClientFromPlayer(player, false);

		IStatsClientCapability stats = null;

		if (player != null) {
			stats = playerBaseClient.getCapability(CapabilityStatsClientHandler.PP_STATS_CLIENT_CAPABILITY, null);
		}

		if (object instanceof Planet) {
			List<Moon> moons = GalaxyRegistry.getMoonsForPlanet((Planet) object);
			for (Moon moon : moons)
				if (stats.getUnlockedPlanets().contains(moon))
					bodyList.add(moon);
		} else if (object instanceof SolarSystem) {
			List<Planet> planets = GalaxyRegistry.getPlanetsForSolarSystem((SolarSystem) object);
			for (Planet planet : planets)
				if (stats.getUnlockedPlanets().contains(planet))
					bodyList.add(planet);
		}

		Collections.sort(bodyList);

		return bodyList;
	}

	@Override
	public HashMap<CelestialBody, Matrix4f> drawCelestialBodies(Matrix4f worldMatrix) {
		final Minecraft minecraft = FMLClientHandler.instance().getClient();
		final EntityPlayerSP player = minecraft.thePlayer;
		final EntityPlayerSP playerBaseClient = PlayerUtil.getPlayerBaseClientFromPlayer(player, false);

		IStatsClientCapability stats = null;

		if (player != null) {
			stats = playerBaseClient.getCapability(CapabilityStatsClientHandler.PP_STATS_CLIENT_CAPABILITY, null);
		}

		GL11.glColor3f(1, 1, 1);
		FloatBuffer fb = BufferUtils.createFloatBuffer(16 * Float.SIZE);
		HashMap<CelestialBody, Matrix4f> matrixMap = Maps.newHashMap();

		for (SolarSystem solarSystem : GalaxyRegistry.getRegisteredSolarSystems().values()) {
			Star star = solarSystem.getMainStar();

			if (star != null && star.getBodyIcon() != null) {
				GL11.glPushMatrix();
				Matrix4f worldMatrix0 = new Matrix4f(worldMatrix);

				Matrix4f.translate(this.getCelestialBodyPosition(star), worldMatrix0, worldMatrix0);

				Matrix4f worldMatrix1 = new Matrix4f();
				Matrix4f.rotate((float) Math.toRadians(45), new Vector3f(0, 0, 1), worldMatrix1, worldMatrix1);
				Matrix4f.rotate((float) Math.toRadians(-55), new Vector3f(1, 0, 0), worldMatrix1, worldMatrix1);
				worldMatrix1 = Matrix4f.mul(worldMatrix0, worldMatrix1, worldMatrix1);

				fb.rewind();
				worldMatrix1.store(fb);
				fb.flip();
				GL11.glMultMatrix(fb);

				float alpha = 1.0F;

				if (this.selectedBody != null && this.selectedBody != star && this.isZoomed()) {
					alpha = 1.0F - Math.min(this.ticksSinceSelection / 25.0F, 1.0F);
				}

				if (this.selectedBody != null && this.isZoomed()) {
					if (star != this.selectedBody) {
						alpha = 1.0F - Math.min(this.ticksSinceSelection / 25.0F, 1.0F);

						if (!(this.lastSelectedBody instanceof Star) && this.lastSelectedBody != null) {
							alpha = 0.0F;
						}
					}
				}

				if (alpha != 0) {
					CelestialBodyRenderEvent.Pre preEvent = new CelestialBodyRenderEvent.Pre(star, star.getBodyIcon(), 8);
					MinecraftForge.EVENT_BUS.post(preEvent);

					GL11.glColor4f(1, 1, 1, alpha);
					if (preEvent.celestialBodyTexture != null) {
						this.mc.renderEngine.bindTexture(preEvent.celestialBodyTexture);
					}

					if (!preEvent.isCanceled()) {
						int size = getWidthForCelestialBodyStatic(star);
						if (star == this.selectedBody && this.selectionState == EnumSelection.SELECTED) {
							size /= 2;
							size *= 3;
						}
						this.drawTexturedModalRect(-size / 2, -size / 2, size, size, 0, 0, preEvent.textureSize, preEvent.textureSize, false, false, preEvent.textureSize, preEvent.textureSize);
						matrixMap.put(star, worldMatrix1);
					}

					CelestialBodyRenderEvent.Post postEvent = new CelestialBodyRenderEvent.Post(star);
					MinecraftForge.EVENT_BUS.post(postEvent);
				}

				fb.clear();
				GL11.glPopMatrix();
			}
		}

		for (Planet planet : GalaxyRegistry.getRegisteredPlanets().values()) {
			if (stats.getUnlockedPlanets().contains(planet)) {
				if (planet.getBodyIcon() != null) {
					GL11.glPushMatrix();
					Matrix4f worldMatrix0 = new Matrix4f(worldMatrix);

					Matrix4f.translate(this.getCelestialBodyPosition(planet), worldMatrix0, worldMatrix0);

					Matrix4f worldMatrix1 = new Matrix4f();
					Matrix4f.rotate((float) Math.toRadians(45), new Vector3f(0, 0, 1), worldMatrix1, worldMatrix1);
					Matrix4f.rotate((float) Math.toRadians(-55), new Vector3f(1, 0, 0), worldMatrix1, worldMatrix1);
					worldMatrix1 = Matrix4f.mul(worldMatrix0, worldMatrix1, worldMatrix1);

					fb.rewind();
					worldMatrix1.store(fb);
					fb.flip();
					GL11.glMultMatrix(fb);

					float alpha = 1.0F;

					if ((this.selectedBody instanceof IChildBody && ((IChildBody) this.selectedBody).getParentPlanet() != planet) || (this.selectedBody instanceof Planet && this.selectedBody != planet && this.isZoomed())) {
						if (this.lastSelectedBody == null && !(this.selectedBody instanceof IChildBody)) {
							alpha = 1.0F - Math.min(this.ticksSinceSelection / 25.0F, 1.0F);
						} else {
							alpha = 0.0F;
						}
					}

					if (alpha != 0) {
						CelestialBodyRenderEvent.Pre preEvent = new CelestialBodyRenderEvent.Pre(planet, planet.getBodyIcon(), 12);
						MinecraftForge.EVENT_BUS.post(preEvent);

						GL11.glColor4f(1, 1, 1, alpha);
						if (preEvent.celestialBodyTexture != null) {
							this.mc.renderEngine.bindTexture(preEvent.celestialBodyTexture);
						}

						if (!preEvent.isCanceled()) {
							int size = getWidthForCelestialBodyStatic(planet);
							this.drawTexturedModalRect(-size / 2, -size / 2, size, size, 0, 0, preEvent.textureSize, preEvent.textureSize, false, false, preEvent.textureSize, preEvent.textureSize);
							matrixMap.put(planet, worldMatrix1);
						}

						CelestialBodyRenderEvent.Post postEvent = new CelestialBodyRenderEvent.Post(planet);
						MinecraftForge.EVENT_BUS.post(postEvent);
					}

					fb.clear();
					GL11.glPopMatrix();
				}
			}
		}

		if (this.selectedBody != null) {
			Matrix4f worldMatrix0 = new Matrix4f(worldMatrix);

			for (Moon moon : GalaxyRegistry.getRegisteredMoons().values()) {
				if (stats.getUnlockedPlanets().contains(moon)) {
					if ((moon == this.selectedBody || (moon.getParentPlanet() == this.selectedBody && this.selectionState != EnumSelection.SELECTED))
							&& (this.ticksSinceSelection > 35 || this.selectedBody == moon || (this.lastSelectedBody instanceof Moon && GalaxyRegistry.getMoonsForPlanet(((Moon) this.lastSelectedBody).getParentPlanet()).contains(moon)))
							|| getSiblings(this.selectedBody).contains(moon)) {
						GL11.glPushMatrix();
						Matrix4f worldMatrix1 = new Matrix4f(worldMatrix0);
						Matrix4f.translate(this.getCelestialBodyPosition(moon), worldMatrix1, worldMatrix1);

						Matrix4f worldMatrix2 = new Matrix4f();
						Matrix4f.rotate((float) Math.toRadians(45), new Vector3f(0, 0, 1), worldMatrix2, worldMatrix2);
						Matrix4f.rotate((float) Math.toRadians(-55), new Vector3f(1, 0, 0), worldMatrix2, worldMatrix2);
						Matrix4f.scale(new Vector3f(0.25F, 0.25F, 1.0F), worldMatrix2, worldMatrix2);
						worldMatrix2 = Matrix4f.mul(worldMatrix1, worldMatrix2, worldMatrix2);

						fb.rewind();
						worldMatrix2.store(fb);
						fb.flip();
						GL11.glMultMatrix(fb);

						CelestialBodyRenderEvent.Pre preEvent = new CelestialBodyRenderEvent.Pre(moon, moon.getBodyIcon(), 8);
						MinecraftForge.EVENT_BUS.post(preEvent);

						GL11.glColor4f(1, 1, 1, 1);
						if (preEvent.celestialBodyTexture != null) {
							this.mc.renderEngine.bindTexture(preEvent.celestialBodyTexture);
						}

						if (!preEvent.isCanceled()) {
							int size = getWidthForCelestialBodyStatic(moon);
							this.drawTexturedModalRect(-size / 2, -size / 2, size, size, 0, 0, preEvent.textureSize, preEvent.textureSize, false, false, preEvent.textureSize, preEvent.textureSize);
							matrixMap.put(moon, worldMatrix1);
						}

						CelestialBodyRenderEvent.Post postEvent = new CelestialBodyRenderEvent.Post(moon);
						MinecraftForge.EVENT_BUS.post(postEvent);
						fb.clear();
						GL11.glPopMatrix();
					}
				}
			}
		}

		if (this.selectedBody != null) {
			Matrix4f worldMatrix0 = new Matrix4f(worldMatrix);

			for (Satellite satellite : GalaxyRegistry.getRegisteredSatellites().values()) {
				if (this.possibleBodies != null && this.possibleBodies.contains(satellite)) {
					if ((satellite == this.selectedBody || (satellite.getParentPlanet() == this.selectedBody && this.selectionState != EnumSelection.SELECTED))
							&& (this.ticksSinceSelection > 35 || this.selectedBody == satellite || (this.lastSelectedBody instanceof Satellite && GalaxyRegistry.getSatellitesForCelestialBody(((Satellite) this.lastSelectedBody).getParentPlanet())
									.contains(satellite)))) {
						GL11.glPushMatrix();
						Matrix4f worldMatrix1 = new Matrix4f(worldMatrix0);
						Matrix4f.translate(this.getCelestialBodyPosition(satellite), worldMatrix1, worldMatrix1);

						Matrix4f worldMatrix2 = new Matrix4f();
						Matrix4f.rotate((float) Math.toRadians(45), new Vector3f(0, 0, 1), worldMatrix2, worldMatrix2);
						Matrix4f.rotate((float) Math.toRadians(-55), new Vector3f(1, 0, 0), worldMatrix2, worldMatrix2);
						Matrix4f.scale(new Vector3f(0.25F, 0.25F, 1.0F), worldMatrix2, worldMatrix2);
						worldMatrix2 = Matrix4f.mul(worldMatrix1, worldMatrix2, worldMatrix2);

						fb.rewind();
						worldMatrix2.store(fb);
						fb.flip();
						GL11.glMultMatrix(fb);

						CelestialBodyRenderEvent.Pre preEvent = new CelestialBodyRenderEvent.Pre(satellite, satellite.getBodyIcon(), 8);
						MinecraftForge.EVENT_BUS.post(preEvent);

						GL11.glColor4f(1, 1, 1, 1);
						this.mc.renderEngine.bindTexture(preEvent.celestialBodyTexture);

						if (!preEvent.isCanceled()) {
							int size = getWidthForCelestialBodyStatic(satellite);
							this.drawTexturedModalRect(-size / 2, -size / 2, size, size, 0, 0, preEvent.textureSize, preEvent.textureSize, false, false, preEvent.textureSize, preEvent.textureSize);
							matrixMap.put(satellite, worldMatrix1);
						}

						CelestialBodyRenderEvent.Post postEvent = new CelestialBodyRenderEvent.Post(satellite);
						MinecraftForge.EVENT_BUS.post(postEvent);
						fb.clear();
						GL11.glPopMatrix();
					}
				}
			}
		}

		return matrixMap;
	}
}
