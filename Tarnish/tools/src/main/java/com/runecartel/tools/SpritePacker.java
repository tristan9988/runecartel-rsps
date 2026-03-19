package com.runecartel.tools;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.file.*;

/**
 * Tool to add/replace sprites in the cache.
 * Usage: java SpritePacker <sprite_id> <image_path> <cache_dir>
 */
public class SpritePacker {
    
    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            System.out.println("Usage: java SpritePacker <sprite_id> <image_path> <cache_dir>");
            System.out.println("Example: java SpritePacker 57 background.png ./Cache");
            return;
        }
        
        int spriteId = Integer.parseInt(args[0]);
        String imagePath = args[1];
        String cacheDir = args[2];
        
        packSprite(spriteId, imagePath, cacheDir);
    }
    
    public static void packSprite(int spriteId, String imagePath, String cacheDir) throws Exception {
        System.out.println("Packing sprite ID " + spriteId + " from " + imagePath);
        
        // Read the image
        BufferedImage image = ImageIO.read(new File(imagePath));
        if (image == null) {
            throw new Exception("Could not read image: " + imagePath);
        }
        
        // Convert to PNG bytes
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        byte[] imageData = baos.toByteArray();
        
        System.out.println("Image size: " + image.getWidth() + "x" + image.getHeight());
        System.out.println("Data size: " + imageData.length + " bytes");
        
        // Read existing cache files
        String datFile = cacheDir + File.separator + "main_file_sprites.dat";
        String idxFile = cacheDir + File.separator + "main_file_sprites.idx";
        
        byte[] datData = Files.readAllBytes(Paths.get(datFile));
        byte[] idxData = Files.readAllBytes(Paths.get(idxFile));
        
        int entryCount = idxData.length / 10;
        System.out.println("Current sprite count: " + entryCount);
        
        if (spriteId >= entryCount) {
            // Need to expand the index
            byte[] newIdxData = new byte[(spriteId + 1) * 10];
            System.arraycopy(idxData, 0, newIdxData, 0, idxData.length);
            idxData = newIdxData;
            entryCount = spriteId + 1;
        }
        
        // Get current position info for this sprite
        int p = spriteId * 10;
        int oldPos = ((idxData[p] & 0xFF) << 16) | ((idxData[p + 1] & 0xFF) << 8) | (idxData[p + 2] & 0xFF);
        int oldLen = ((idxData[p + 3] & 0xFF) << 16) | ((idxData[p + 4] & 0xFF) << 8) | (idxData[p + 5] & 0xFF);
        
        // Append new image data to end of dat file
        int newPos = datData.length;
        int newLen = imageData.length;
        
        // Create new dat file with appended data
        byte[] newDatData = new byte[datData.length + imageData.length];
        System.arraycopy(datData, 0, newDatData, 0, datData.length);
        System.arraycopy(imageData, 0, newDatData, datData.length, imageData.length);
        
        // Update index entry
        idxData[p] = (byte) ((newPos >> 16) & 0xFF);
        idxData[p + 1] = (byte) ((newPos >> 8) & 0xFF);
        idxData[p + 2] = (byte) (newPos & 0xFF);
        idxData[p + 3] = (byte) ((newLen >> 16) & 0xFF);
        idxData[p + 4] = (byte) ((newLen >> 8) & 0xFF);
        idxData[p + 5] = (byte) (newLen & 0xFF);
        // Offset X and Y (keep as 0)
        idxData[p + 6] = 0;
        idxData[p + 7] = 0;
        idxData[p + 8] = 0;
        idxData[p + 9] = 0;
        
        // Write updated files
        Files.write(Paths.get(datFile), newDatData);
        Files.write(Paths.get(idxFile), idxData);
        
        System.out.println("Sprite " + spriteId + " packed successfully!");
        System.out.println("New position: " + newPos + ", length: " + newLen);
    }
}

