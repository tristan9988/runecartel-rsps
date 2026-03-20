package com.osroyale;

import com.osroyale.skeletal.SkeletalFrame;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Jire
 */
public interface Frame {

    FrameBase getFrameBase();

    boolean hasAlphaTransform();

    Map<Integer, Frame[]> allFrames = new ConcurrentHashMap<>();

    static Frame getFrame(final int frameId) {
        if (frameId == -1) {
            return null;
        }

        final int groupId = frameId >>> 16;

        Frame[] frames = allFrames.get(groupId);
        if (frames == null) {
            Client.instance.onDemandFetcher.loadData(1, groupId);
            // Re-check after loading (works for synchronous local cache loading)
            frames = allFrames.get(groupId);
            if (frames == null) {
                return null;
            }
        }

        final int fileId = frameId & 0xFFFF;
        if (fileId < 0 || fileId >= frames.length) {
            return null;
        }
        return frames[fileId];
    }

    static Frame[] loadFrames(final int groupId, final byte[] data) {
        if (data == null || data.length < 2) {
            allFrames.remove(groupId);
            return null;
        }

        final Buffer buffer = new Buffer(data);

        final int highestFileId = buffer.readUnsignedShort();
        Frame[] frames = new Frame[Math.max(highestFileId + 1, 1)];

        while (buffer.position < data.length) {
            if (buffer.position + 5 > data.length) {
                break;
            }

            final int fileId = buffer.readUnsignedShort();
            final int fileSize = buffer.readMedium();

            if (fileId < 0 || fileSize < 0 || buffer.position + fileSize > data.length) {
                break;
            }

            if (fileId >= frames.length) {
                final Frame[] resized = new Frame[fileId + 1];
                System.arraycopy(frames, 0, resized, 0, frames.length);
                frames = resized;
            }

            final byte[] fileData = new byte[fileSize];
            System.arraycopy(buffer.array, buffer.position, fileData, 0, fileSize);
            buffer.position += fileSize;

            final int frameId = (groupId << 16) | fileId;

            final Frame frame =
                    SkeletalFrame.skeletalFrameIds.contains(frameId)
                            ? new SkeletalFrame(fileData)
                            : new NormalFrame(groupId, fileData);
            frames[fileId] = frame;
        }

        allFrames.put(groupId, frames);
        return frames;
    }

    static boolean hasAlphaTransform(final int frameID) {
        return frameID == -1;
    }

}
