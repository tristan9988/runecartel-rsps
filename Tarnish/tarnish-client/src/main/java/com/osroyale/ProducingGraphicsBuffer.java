package com.osroyale;

import net.runelite.rs.api.RSRasterProvider;

import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.image.*;
import java.util.Hashtable;

public class ProducingGraphicsBuffer extends AbstractRasterProvider implements RSRasterProvider {
    private Component component;
	private Image image;

    public ProducingGraphicsBuffer(int width, int height, Component component) {

        super.width = width;
        super.height = height;
        super.pixels = new int[height * width + 1];
        // Use 24-bit depth (matches TYPE_INT_RGB) for Java2D hardware-accelerated blitting.
        // 32-bit creates TYPE_CUSTOM which forces slow software blit paths.
        DataBufferInt var4 = new DataBufferInt(super.pixels, width * height);
        DirectColorModel var5 = new DirectColorModel(24, 0x00ff0000, 0x0000ff00, 0x000000ff);
        WritableRaster var6 = Raster.createWritableRaster(var5.createCompatibleSampleModel(super.width, super.height), var4, (Point) null);
        this.image = new BufferedImage(var5, var6, false, new Hashtable());
        this.setComponent(component);
        this.apply();
        this.init(this.width, this.height, component);
    }

    public void init(int width, int height, Component canvas) {
        if (!Client.instance.isGpu()) {
            return;
        }

        final int[] pixels = getPixels();

        // we need to make our own buffered image for the client with the alpha channel enabled in order to
        // have alphas for the overlays applied correctly
        DataBufferInt dataBufferInt = new DataBufferInt(pixels, pixels.length);
        DirectColorModel directColorModel = new DirectColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB),
                32, 0xff0000, 0xff00, 0xff, 0xff000000,
                true, DataBuffer.TYPE_INT);
        WritableRaster writableRaster = Raster.createWritableRaster(directColorModel.createCompatibleSampleModel(width, height), dataBufferInt, null);
        BufferedImage bufferedImage = new BufferedImage(directColorModel, writableRaster, true, new Hashtable());

        setImage(bufferedImage);
    }

    public final void setComponent(Component var1) {
		this.component = var1;
	}

    public void drawFull(int var1, int var2) {
        Graphics g = this.component.getGraphics();
        if (g != null) {
            this.drawFull0(g, var1, var2);
        }
    }

    public final void draw(int var1, int var2, int var3, int var4) {
        Graphics g = this.component.getGraphics();
        if (g != null) {
            this.draw0(g, var1, var2, var3, var4);
        }
    }

    private final void drawFull0(Graphics var1, int var2, int var3) {
        if (Client.DISABLE_RUNELITE_RENDER_HOOKS) {
            // Fast path: direct blit to canvas, bypassing the entire Hooks.draw() chain.
            // Skips overlay rendering, processDrawComplete, image copy, and extra Graphics2D allocations.
            try {
                var1.drawImage(this.image, var2, var3, null);
            } catch (Exception e) {
                // Canvas may have been disposed during resize
            }
            return;
        }
        Client.instance.getCallbacks().draw(this, var1, var2, var3);
    }

    final void draw0(Graphics var1, int var2, int var3, int var4, int var5) {
        try {
            Shape var6 = var1.getClip();
            var1.clipRect(var2, var3, var4, var5);
            var1.drawImage(this.image, 0, 0, null);
            var1.setClip(var6);
        } catch (Exception var7) {
            this.component.repaint();
        }
    }

    @Override
    public int[] getPixels() {
        return pixels;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public void setRaster() {
        Rasterizer2D.initDrawingArea(pixels,width,height);
    }

    @Override
    public Image getImage() {
        return image;
    }

    @Override
    public void setImage(Image image) {
        this.image = image;
    }

    @Override
    public Component getCanvas() {
        return component;
    }
}
