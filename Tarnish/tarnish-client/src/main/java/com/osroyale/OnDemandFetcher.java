package com.osroyale;

import org.jire.swiftfup.client.FileRequest;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;

public final class OnDemandFetcher {

    private static final boolean FILE_DEBUG_LOGGING = false;

    private final Client client;

    public int mapAmount = 0;

    public void start(StreamLoader versionList) {
/*		if (Configuration.USE_UPDATE_SERVER) {
			FileChecksumsRequest fileChecksumsRequest = client.fileClient.requestChecksums();
			try {
				fileChecksumsRequest.get(30, TimeUnit.SECONDS);
			} catch (InterruptedException | ExecutionException | TimeoutException e) {
				e.printStackTrace();
				return;
			}
		}*/

        byte[] array = versionList.getFile("map_index");
        /*try {
            array = Files.readAllBytes(Path.of("map_index"));
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }*/
        Buffer stream = new Buffer(array);
        int size = stream.readUnsignedShort();

        regions = new int[size];
        mapFiles = new int[size];
        landscapes = new int[size];
        // shit retarded? u sure u packed that map_index yh lol can redo if u want

        for (int index = 0; index < size; index++) {
            regions[index] = stream.readUnsignedShort();
            mapFiles[index] = stream.readUnsignedShort();
            landscapes[index] = stream.readUnsignedShort();
            mapAmount++;
        }
    }

    public int getNodeCount() {
        return 0;
    }

    public int getVersionCount(int j) {
        return versions[j].length;
    }

    public boolean loadData(int indexID, int fileID) {
        return loadData(indexID, fileID, true);
    }

    public static void debugWrite(String msg) {
        if (!FILE_DEBUG_LOGGING) {
            return;
        }
        try {
            java.io.FileWriter fw = new java.io.FileWriter(System.getProperty("user.home") + "/.runecartel/debug.txt", true);
            fw.write(msg + "\n");
            fw.close();
        } catch (Exception e) {}
    }

    public boolean loadData(int indexID, int fileID, boolean flush) {
        // Only use fileClient if update server is enabled and fileClient is initialized
        if (!Configuration.USE_UPDATE_SERVER || client.fileClient == null) {
            // Read directly from local cache file stores
            int fileStoreIndex = indexID + 1;
            if (fileStoreIndex >= 0 && fileStoreIndex < client.fileStores.length && client.fileStores[fileStoreIndex] != null) {
                byte[] data = client.fileStores[fileStoreIndex].readFile(fileID);
                data = tryGunzip(data);
                switch (indexID) {
                    case 0: // Models
                        Model.loadModel(data, fileID);
                        break;
                    case 1: // Animations
                        if (data != null) {
                            Frame.loadFrames(fileID, data);
                        }
                        break;
                    // case 2: Music - not needed for loading
                    // case 3: Maps - handled in Client.method54
                }
            } else {
                debugWrite("loadData idx=" + indexID + " file=" + fileID + " FILESTORE NULL");
            }
            return true;
        }
        //System.out.println("REQUESTED " + indexID + ":" + fileID + " (flush=" + flush + ")");
        FileRequest request = client.fileClient.request(indexID + 1, fileID);
        if (flush && !request.isDone()) {
            client.fileClient.flush();
            //System.out.println("FLUSHED for " + indexID+":"+fileID+" / " + flush);
        }
        return true;
    }

    /**
     * Just a reversed argument version of @{@link #loadData(int, int)}
     */
    public void writeRequest(int fileID, int indexID) {
        loadData(indexID, fileID, true);
    }

    public int resolve(int type, int regionX, int regionY) {
        int regionId = (regionX << 8) | regionY;
        for (int area = 0; area < regions.length; area++) {
            if (regions[area] == regionId) {
                if (type == 0) {
                    return mapFiles[area] > 9999 ? -1 : mapFiles[area];
                } else {
                    return landscapes[area] > 9999 ? -1 : landscapes[area];
                }
            }
        }

        return -1;
    }

    public void loadData(int id) {
        loadData(0, id);
    }

    public boolean method564(int i) {
        for (int k = 0; k < regions.length; k++)
            if (landscapes[k] == i)
                return true;
        return false;
    }

    public OnDemandFetcher(final Client client) {
        this.client = client;
        versions = new int[4][];
    }

    private static byte[] tryGunzip(byte[] data) {
        if (data == null || data.length < 2) {
            return data;
        }

        if ((data[0] & 0xFF) != 0x1F || (data[1] & 0xFF) != 0x8B) {
            return data;
        }

        try (GZIPInputStream gzipInputStream = new GZIPInputStream(new ByteArrayInputStream(data));
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream(data.length)) {
            byte[] buffer = new byte[4096];
            int read;
            while ((read = gzipInputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }
            return outputStream.toByteArray();
        } catch (IOException e) {
            debugWrite("loadData gunzip failed: " + e);
            return null;
        }
    }

    private int[] landscapes;
    private int[] mapFiles;
    private final int[][] versions;
    private int[] regions;

}
