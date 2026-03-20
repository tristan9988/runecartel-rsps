package com.osroyale;

import net.runelite.api.Point;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import net.runelite.rs.api.RSIterableNodeDeque;
import net.runelite.rs.api.RSNPC;
import net.runelite.rs.api.RSNPCComposition;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.awt.image.BufferedImage;

public final class Npc extends Entity implements RSNPC {

    private Model getAnimatedModel() {
        try {
            final boolean tween = Client.instance.isInterpolateNpcAnimations();

            int primaryId = -1;
            int idleAnim = -1;
            int nextId = -1;
            int currCycle = -1;
            int nextCycle = -1;
            //System.err.println("NPC " + definition.name + " has " + primarySeqID + "/" + secondarySeqID + " and stand " + seqStandID);
            if (super.primarySeqID >= 0 && super.primarySeqDelay == 0) {
				final Animation primarySeq = Animation.lookup(super.primarySeqID);
				if (primarySeq == null) {
					super.primarySeqID = -1;
				} else {
                final boolean skeletal = primarySeq.isSkeletalAnimation();
                Animation secondarySeq = null;
                if (!skeletal) {
                    if (tween && primarySeq.primaryFrameIds != null && super.nextAnimationFrame < primarySeq.primaryFrameIds.length) {
                        nextId = primarySeq.primaryFrameIds[super.nextAnimationFrame];
                        if (primarySeq.frameDelays != null && super.primarySeqFrame < primarySeq.frameDelays.length) {
                            currCycle = primarySeq.frameDelays[super.primarySeqFrame];
                        }
                        nextCycle = super.primarySeqCycle;
                    }
                }
                if (primarySeq.isSkeletalAnimation()) {
                    primaryId = primarySeq.getSkeletalFrameId();
                } else if (primarySeq.primaryFrameIds != null && super.primarySeqFrame < primarySeq.primaryFrameIds.length) {
                    primaryId = primarySeq.primaryFrameIds[super.primarySeqFrame];
                }
                if (super.secondarySeqID >= 0 && super.secondarySeqID != super.seqStandID) {
					secondarySeq = Animation.lookup(super.secondarySeqID);
					if (secondarySeq == null) {
						super.secondarySeqID = super.seqStandID;
					} else {
                    if (secondarySeq.isSkeletalAnimation()) {
                        idleAnim = secondarySeq.getSkeletalFrameId();
                    } else if (secondarySeq.primaryFrameIds != null && super.secondarySeqFrame < secondarySeq.primaryFrameIds.length) {
                        idleAnim = secondarySeq.primaryFrameIds[super.secondarySeqFrame];
                    }
					}
                }
                // double anim
                if (primarySeq.isSkeletalAnimation() || (secondarySeq != null && secondarySeq.isSkeletalAnimation())) {
                    //System.err.println("primarySeq=" + primarySeq.getSkeletalFrameId() + ",secondarySeq=" + secondarySeq);
                    return definition.getAnimatedModel(idleAnim, primaryId, nextId, primarySeqFrame, secondarySeqFrame,
                            primarySeq.interleaveOrder, true);
                }
                /*return definition.getAnimatedModel(idleAnim, primaryId, nextId, currCycle, nextCycle,
                        primarySeq.interleaveOrder, false);*/
				} // end else (primarySeq != null)
            }
            Animation secondarySeq = null;
            if (primaryId == -1 && super.secondarySeqID >= 0) {
        secondarySeq = Animation.lookup(super.secondarySeqID);
        if (secondarySeq == null) {
          super.secondarySeqID = super.seqStandID;
        } else {
                if (secondarySeq.isSkeletalAnimation()) {
                    //System.err.println("secondarySeq=" + secondarySeq.getSkeletalFrameId() + ", which is for " + secondarySeqID + " cycle " + primarySeqFrame + " / " + secondarySeqFrame);
                    return definition.getAnimatedModel(secondarySeq.getSkeletalFrameId(), -1, nextId,
                            primarySeqFrame, secondarySeqFrame, null, true);
                }
                if (secondarySeq.primaryFrameIds != null && super.secondarySeqFrame < secondarySeq.primaryFrameIds.length) {
                    primaryId = secondarySeq.primaryFrameIds[super.secondarySeqFrame];
                }
                if (tween && secondarySeq.primaryFrameIds != null && super.nextIdleFrame < secondarySeq.primaryFrameIds.length) {
                    nextId = secondarySeq.primaryFrameIds[super.nextIdleFrame];
                    if (secondarySeq.frameDelays != null && super.secondarySeqFrame < secondarySeq.frameDelays.length) {
                        currCycle = secondarySeq.frameDelays[super.secondarySeqFrame];
                    }
                    nextCycle = super.secondarySeqCycle;
                }
        } // end else (secondarySeq != null)
            }
            return definition.getAnimatedModel(idleAnim, primaryId, nextId, currCycle, nextCycle,
					primaryId != -1 && idleAnim != -1 && Animation.lookup(super.primarySeqID) != null ? Animation.lookup(super.primarySeqID).interleaveOrder : null,
                    false);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public Model getRotatedModel() {
        if (definition == null)
            return null;
        Model animatedModel = getAnimatedModel();
        if (animatedModel == null)
            return null;
        super.height = animatedModel.modelBaseY;
        if (super.graphicId != -1 && super.currentAnimationId != -1) {
      if (Graphic.graphics == null || super.graphicId < 0 || super.graphicId >= Graphic.graphics.length) {
        super.graphicId = -1;
        return animatedModel;
      }
      Graphic spotAnim = Graphic.graphics[super.graphicId];
      if (spotAnim == null || spotAnim.animationSequence == null) {
        super.graphicId = -1;
        return animatedModel;
      }
            Model model_1 = spotAnim.getModel();
            if (model_1 != null && spotAnim.animationSequence.primaryFrameIds != null
                    && super.currentAnimationId >= 0
                    && super.currentAnimationId < spotAnim.animationSequence.primaryFrameIds.length) {
                int frame = spotAnim.animationSequence.primaryFrameIds[super.currentAnimationId];
                Model graphic = new Model(true, Frame.hasAlphaTransform(frame), false, model_1);
                graphic.offsetBy(0, -super.graphicHeight, 0);
                graphic.generateBones();
                graphic.interpolate(frame);
                graphic.groupedTriangleLabels = null;
                graphic.groupedVertexLabels = null;
                if (spotAnim.resizeXY != 128 || spotAnim.resizeZ != 128)
                    graphic.scale(spotAnim.resizeXY, spotAnim.resizeXY, spotAnim.resizeZ);
                graphic.light(64 + spotAnim.modelBrightness, 850 + spotAnim.modelShadow, -30, -50, -30, true);
                Model[] build = {animatedModel, graphic};
                animatedModel = new Model(build);
            }
        }
        if (definition.size == 1)
            animatedModel.singleTile = true;
        return animatedModel;
    }

    public boolean isVisible() {
        return definition != null;
    }

    Npc() {
    }

    public NpcDefinition definition;

    @Override
    public int getCombatLevel() {
        return 0;
    }

    @Nullable
    @Override
    public NPCComposition getTransformedComposition() {
        return null;
    }

    @Override
    public void onDefinitionChanged(NPCComposition composition) {

    }

    @Override
    public int getId() {
        return (int) (definition != null ? definition.npcId : -1);
    }

    @Nullable
    @Override
    public String getName() {
        final NpcDefinition definition = this.definition;
        return definition == null ? null : definition.name;
    }

    @Override
    public Actor getInteracting() {
        return null;
    }

    @Override
    public int getHealthRatio() {
        return 0;
    }

    @Override
    public int getHealthScale() {
        return 0;
    }

    @Override
    public WorldPoint getWorldLocation() {
        return WorldPoint.fromLocal(Client.instance,
                this.getPathX()[0] * Perspective.LOCAL_TILE_SIZE + Perspective.LOCAL_TILE_SIZE / 2,
                this.getPathY()[0] * Perspective.LOCAL_TILE_SIZE + Perspective.LOCAL_TILE_SIZE / 2,
                Client.instance.getPlane());
    }

    @Override
    public WorldArea getWorldArea() {
        return new WorldArea(getWorldLocation(), definition.size, definition.size);
    }

    @Override
    public LocalPoint getLocalLocation() {
        return new LocalPoint(this.x, this.y);
    }

    @Override
    public void setIdleRotateLeft(int animationID) {

    }

    @Override
    public void setIdleRotateRight(int animationID) {

    }

    @Override
    public void setWalkAnimation(int animationID) {

    }

    @Override
    public void setWalkRotateLeft(int animationID) {

    }

    @Override
    public void setWalkRotateRight(int animationID) {

    }

    @Override
    public void setWalkRotate180(int animationID) {

    }

    @Override
    public void setRunAnimation(int animationID) {

    }

    @Override
    public Polygon getCanvasTilePoly() {
        return Perspective.getCanvasTilePoly(Client.instance, getLocalLocation());
    }

    @Nullable
    @Override
    public Point getCanvasTextLocation(Graphics2D graphics, String text, int zOffset) {
        return Perspective.getCanvasTextLocation(Client.instance, graphics, getLocalLocation(), text, zOffset);
    }

    @Override
    public Point getCanvasImageLocation(BufferedImage image, int zOffset) {
        return Perspective.getCanvasImageLocation(Client.instance, getLocalLocation(), image, zOffset);
    }

    @Override
    public Point getCanvasSpriteLocation(SpritePixels sprite, int zOffset) {
        return null;
    }

    @Override
    public Point getMinimapLocation() {
        return null;
    }

    @Override
    public Shape getConvexHull() {
        return null;
    }

    @Override
    public boolean isDead() {
        return false;
    }

    @Override
    public boolean isMoving() {
        return false;
    }

    @Override
    public int getRSInteracting() {
        return 0;
    }

    @Override
    public String getOverheadText() {
        return null;
    }

    @Override
    public void setOverheadText(String overheadText) {

    }

    @Override
    public int getX() {
        return x;
    }

    @Override
    public int getY() {
        return y;
    }

    @Override
    public int[] getPathX() {
        return this.smallX;
    }

    @Override
    public int[] getPathY() {
        return this.smallY;
    }

    @Override
    public int getAnimation() {
        return 0;
    }

    @Override
    public void setAnimation(int animation) {

    }

    @Override
    public int getAnimationFrame() {
        return 0;
    }

    @Override
    public int getActionFrame() {
        return 0;
    }

    @Override
    public void setAnimationFrame(int frame) {

    }

    @Override
    public void setActionFrame(int frame) {

    }

    @Override
    public int getActionFrameCycle() {
        return 0;
    }

    @Override
    public int getGraphic() {
        return 0;
    }

    @Override
    public void setGraphic(int id) {

    }

    @Override
    public int getSpotAnimFrame() {
        return 0;
    }

    @Override
    public void setSpotAnimFrame(int id) {

    }

    @Override
    public int getSpotAnimationFrameCycle() {
        return 0;
    }

    @Override
    public int getIdlePoseAnimation() {
        return 0;
    }

    @Override
    public void setIdlePoseAnimation(int animation) {

    }

    @Override
    public int getPoseAnimation() {
        return 0;
    }

    @Override
    public void setPoseAnimation(int animation) {

    }

    @Override
    public int getPoseFrame() {
        return 0;
    }

    @Override
    public void setPoseFrame(int frame) {

    }

    @Override
    public int getPoseFrameCycle() {
        return 0;
    }

    @Override
    public int getLogicalHeight() {
        return 0;
    }

    @Override
    public int getOrientation() {
        return 0;
    }

    @Override
    public int getCurrentOrientation() {
        return 0;
    }

    @Override
    public RSIterableNodeDeque getHealthBars() {
        return null;
    }

    @Override
    public int[] getHitsplatValues() {
        return new int[0];
    }

    @Override
    public int[] getHitsplatTypes() {
        return new int[0];
    }

    @Override
    public int[] getHitsplatCycles() {
        return new int[0];
    }

    @Override
    public int getIdleRotateLeft() {
        return 0;
    }

    @Override
    public int getIdleRotateRight() {
        return 0;
    }

    @Override
    public int getWalkAnimation() {
        return 0;
    }

    @Override
    public int getWalkRotate180() {
        return 0;
    }

    @Override
    public int getWalkRotateLeft() {
        return 0;
    }

    @Override
    public int getWalkRotateRight() {
        return 0;
    }

    @Override
    public int getRunAnimation() {
        return 0;
    }

    @Override
    public void setDead(boolean dead) {

    }

    @Override
    public int getPathLength() {
        return 0;
    }

    @Override
    public int getOverheadCycle() {
        return 0;
    }

    @Override
    public void setOverheadCycle(int cycle) {

    }

    @Override
    public int getPoseAnimationFrame() {
        return 0;
    }

    @Override
    public void setPoseAnimationFrame(int frame) {

    }

    @Override
    public RSNPCComposition getComposition() {
        return null;
    }

    @Override
    public int getIndex() {
        return 0;
    }

    @Override
    public void setIndex(int id) {

    }
}
