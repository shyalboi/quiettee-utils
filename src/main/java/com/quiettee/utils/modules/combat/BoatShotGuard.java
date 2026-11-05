package com.quiettee.utils.modules.combat;

import meteordevelopment.meteorclient.systems.friends.Friends;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.*;

public final class BoatShotGuard {
    private final List<Vec3> crystals = new ArrayList<>();
    private final Set<BlockPos> reservations = new HashSet<>();
    public void reset() { crystals.clear(); reservations.clear(); }
    public void reserve(BlockPos pos) { reservations.add(pos.immutable()); }
    public void release(BlockPos pos) { reservations.remove(pos); }

    public void refresh(ClientLevel world, Player self, AbstractBoat boat, Entity target) {
        refresh(world,self,boat,target,true);
    }
    public void refresh(ClientLevel world, Player self, AbstractBoat boat, Entity target, boolean protectCrystals) {
        crystals.clear();
        if (!protectCrystals) return;
        AABB scan = boat.getBoundingBox().inflate(45);
        if(target!=null) scan=scan.minmax(target.getBoundingBox().inflate(14));
        for (Entity entity : world.getEntities(self,scan,e -> e instanceof EndCrystal && !e.isRemoved())) crystals.add(entity.position());

        int scanned=0;
        for (Player enemy : world.players()) {
            if (enemy==self || enemy.isSpectator() || !enemy.isAlive() || !Friends.get().shouldAttack(enemy)
                || enemy.distanceToSqr(boat)>45*45 && (target==null || enemy.distanceToSqr(target)>14*14)) continue;
            if (++scanned>12) { crystals.add(enemy.position()); continue; }
            BlockPos center=enemy.blockPosition();
            for (BlockPos base : BlockPos.betweenClosed(center.offset(-6,-3,-6),center.offset(6,4,6))) {
                if (!world.getChunkSource().hasChunk(base.getX()>>4,base.getZ()>>4)) continue;
                var state=world.getBlockState(base);
                if (!(state.is(Blocks.OBSIDIAN)||state.is(Blocks.BEDROCK)) || !world.getBlockState(base.above()).isAir()) continue;
                Vec3 source=Vec3.atCenterOf(base).add(0,.5,0);

                if (enemy.getEyePosition().distanceToSqr(source)<=49) crystals.add(source);
            }
        }
    }

    public boolean clear(ClientLevel world, AbstractBoat boat, Vec3 step) {
        return clear(world,boat,step,true);
    }
    public boolean clear(ClientLevel world, AbstractBoat boat, Vec3 step, boolean protectCrystals) {
        return clearAt(world,boat,Vec3.ZERO,step,protectCrystals);
    }
    public boolean clearAt(ClientLevel world, AbstractBoat boat, Vec3 offset, Vec3 step, boolean protectCrystals) {
        if (!Double.isFinite(step.lengthSqr()) || !Double.isFinite(offset.lengthSqr())) return false;
        for (Entity body : boat.getSelfAndPassengers().toList()) {
            AABB current=body.getBoundingBox().move(offset).inflate(.35), path=current.expandTowards(step);
            if (protectCrystals) for (Vec3 source : crystals) if (!awayOrOutside(current,step,source,13)) return false;
            for (BlockPos pos : reservations) if (!webClear(current,step,new AABB(pos))) return false;
            for (BlockPos pos : BlockPos.betweenClosed(BlockPos.containing(path.minX,path.minY,path.minZ),BlockPos.containing(path.maxX,path.maxY,path.maxZ))) {
                var state=world.getBlockState(pos);
                if (state.is(Blocks.COBWEB) || state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE)
                    || state.is(Blocks.CACTUS) || state.is(Blocks.SWEET_BERRY_BUSH) || state.is(Blocks.POWDER_SNOW)) {
                    if (!webClear(current,step,new AABB(pos))) return false;
                }
            }
        }
        return true;
    }
    public static boolean webClear(AABB body, Vec3 step, AABB web) {
        if (!body.expandTowards(step).intersects(web)) return true;

        return body.intersects(web) && !body.move(step).intersects(web)
            && body.getCenter().subtract(web.getCenter()).dot(step)>0;
    }
    public static boolean awayOrOutside(AABB body, Vec3 step, Vec3 source, double radius) {
        if (distanceSquared(body.expandTowards(step),source)>radius*radius) return true;
        double before=distanceSquared(body,source), after=distanceSquared(body.move(step),source);
        Vec3 nearest=new Vec3(Math.clamp(source.x,body.minX,body.maxX),Math.clamp(source.y,body.minY,body.maxY),Math.clamp(source.z,body.minZ,body.maxZ));
        return before<=radius*radius && after>before+.001 && nearest.subtract(source).dot(step)>=0;
    }
    public static double distanceSquared(AABB box, Vec3 p) {
        double x=Math.max(Math.max(box.minX-p.x,p.x-box.maxX),0), y=Math.max(Math.max(box.minY-p.y,p.y-box.maxY),0), z=Math.max(Math.max(box.minZ-p.z,p.z-box.maxZ),0);
        return x*x+y*y+z*z;
    }
}
