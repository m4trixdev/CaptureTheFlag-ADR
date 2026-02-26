package br.com.m4trixdev.model;

import org.bukkit.Location;

public class EventArea {

    private Location pos1;
    private Location pos2;

    public void setPos1(Location pos1) {
        this.pos1 = pos1 != null ? pos1.clone() : null;
    }

    public void setPos2(Location pos2) {
        this.pos2 = pos2 != null ? pos2.clone() : null;
    }

    public Location getPos1() { return pos1; }
    public Location getPos2() { return pos2; }

    public boolean isComplete() {
        return pos1 != null && pos2 != null;
    }

    public boolean isInside(Location loc) {
        if (!isComplete()) return false;
        if (loc.getWorld() == null || !loc.getWorld().equals(pos1.getWorld())) return false;

        double minX = Math.min(pos1.getX(), pos2.getX());
        double maxX = Math.max(pos1.getX(), pos2.getX());
        double minY = Math.min(pos1.getY(), pos2.getY());
        double maxY = Math.max(pos1.getY(), pos2.getY());
        double minZ = Math.min(pos1.getZ(), pos2.getZ());
        double maxZ = Math.max(pos1.getZ(), pos2.getZ());

        return loc.getX() >= minX && loc.getX() <= maxX
                && loc.getY() >= minY && loc.getY() <= maxY
                && loc.getZ() >= minZ && loc.getZ() <= maxZ;
    }
}
