package ar1hurgit.enderpearltweak.client.utils;


import ar1hurgit.enderpearltweak.client.PearlNameClient;
import ar1hurgit.enderpearltweak.client.utils.QuickImports;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.RaycastContext;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class RotationUtils implements QuickImports {

    public static float[] calculate(final Vec3d from, final Vec3d to) {
        final Vec3d diff = to.subtract(from);
        final double distance = Math.hypot(diff.getX(), diff.getZ());

        final float yaw = MathHelper.wrapDegrees((float) (Math.toDegrees(Math.atan2(diff.getZ(), diff.getX()))) - 90.0F);
        final float pitch = MathHelper.wrapDegrees((float) (-Math.toDegrees(Math.atan2(diff.getY(), distance))));

        return new float[]{yaw, pitch};
    }


    public static float[] calculate(final Vec3d to) {
        return calculate(mc.player.getPos().add(0, mc.player.getEyeHeight(mc.player.getPose()), 0), to);
    }

    public static float[] calculate(final Vec3d position, final Direction direction) {
        double x = position.getX() + 0.5D;
        double y = position.getY() + 0.5D;
        double z = position.getZ() + 0.5D;

        x += (double) direction.getVector().getX() * 0.5D;
        y += (double) direction.getVector().getY() * 0.5D;
        z += (double) direction.getVector().getZ() * 0.5D;
        return calculate(new Vec3d(x, y, z));
    }
    public static float[] getRotationToBlock(BlockPos blockPos, Direction direction) {
        PlayerEntity player = mc.player;
        Vec3d playerPos = player.getEyePos();
        Vec3d pos = null;
        double distance = Double.MAX_VALUE;
        for (float xOffset = 0.5f; xOffset >= -0.5; xOffset -= 0.01f) {
            for (float yOffset = 0.5f; yOffset >= 0; yOffset -= 0.1f) {
                for (float zOffset = 0.5f; zOffset >= -0.5; zOffset -= 0.01f) {
                    Vec3d target = blockPos.toCenterPos()
                            .add(
                                    (mc.player.getX() - blockPos.toCenterPos().getX()) + xOffset,
                                    yOffset,
                                    (mc.player.getZ() - blockPos.toCenterPos().getZ()) + zOffset);

                    if (mc.world.getBlockState(BlockPos.ofFloored(target)).isAir()) continue;
                    if (playerPos.distanceTo(target) < distance) {
                        distance = playerPos.distanceTo(target);
                        pos = target;
                    }
                }
            }
        }
        if (pos == null) {
            return new float[]{mc.player.getYaw(), mc.player.getPitch()};
        }
        Vec3d delta = pos.subtract(playerPos);

        double distanceXZ = Math.sqrt(delta.x * delta.x + delta.z * delta.z);

        float yaw = (float) MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90.0F);
        float pitch = (float) MathHelper.wrapDegrees(-Math.toDegrees(Math.atan2(delta.y, distanceXZ)));

        return new float[]{yaw, pitch};
    }

    public static float[] applyGCD(final float[] rotations, final float[] lastRotations) {
        final float f = (float) (mc.options.getMouseSensitivity().getValue() * 0.6F + 0.2F);
        final float gcd = f * f * f * 1.2F;

        final float deltaYaw = rotations[0] - lastRotations[0];
        final float deltaPitch = rotations[1] - lastRotations[1];

        return new float[]{
                lastRotations[0] + (deltaYaw - (deltaYaw % gcd)),
                lastRotations[1] + (deltaPitch - (deltaPitch % gcd))
        };
    }
    public static Direction getClosestSide(BlockPos blockPos, Vec3d targetVec) {
        Vec3d blockCenterPos = blockPos.toCenterPos();
        double deltaX = targetVec.x - blockCenterPos.x;
        double deltaY = targetVec.y - blockCenterPos.y;
        double deltaZ = targetVec.z - blockCenterPos.z;

        double absDeltaX = Math.abs(deltaX);
        double absDeltaY = Math.abs(deltaY);
        double absDeltaZ = Math.abs(deltaZ);

        if (absDeltaX > absDeltaY && absDeltaX > absDeltaZ) {
            return deltaX > 0 ? Direction.EAST : Direction.WEST;
        } else if (absDeltaY > absDeltaX && absDeltaY > absDeltaZ) {
            return deltaY > 0 ? Direction.UP : Direction.DOWN;
        } else {
            return deltaZ > 0 ? Direction.SOUTH : Direction.NORTH;
        }
    }
    public static float[] getFixedRotations(float[] prev, float[] current) {
        return applyGCD(getCappedRotations(prev, current), prev);
    }
    public static Vec3d getRotationVec(float yaw, float pitch) {
        float f = pitch * (float) (Math.PI / 180.0);
        float g = -yaw * (float) (Math.PI / 180.0);
        float h = MathHelper.cos(g);
        float i = MathHelper.sin(g);
        float j = MathHelper.cos(f);
        float k = MathHelper.sin(f);
        return new Vec3d((double)(i * j), (double)(-k), (double)(h * j));
    }
    public static BlockHitResult rayTrace(float yaw, float pitch, float reach) {
        Vec3d start = mc.cameraEntity.getCameraPosVec(mc.getRenderTickCounter().getTickDelta(false));
        Vec3d rotationVec = getRotationVec(yaw, pitch);
        Vec3d end = start.add(rotationVec.multiply(reach));

        return mc.world.raycast(new RaycastContext(start, end, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.SOURCE_ONLY, mc.cameraEntity));
    }
    public static BlockHitResult rayTrace(float yaw, float pitch, float reach, float tickDelta) {
        Vec3d start = mc.cameraEntity.getCameraPosVec(tickDelta);
        Vec3d rotationVec = getRotationVec(yaw, pitch);
        Vec3d end = start.add(rotationVec.multiply(reach));

        return mc.world.raycast(new RaycastContext(
                start,
                end,
                RaycastContext.ShapeType.OUTLINE,
                RaycastContext.FluidHandling.NONE,
                mc.cameraEntity
        ));
    }

    public static BlockHitResult rayTrace(Vec3d end) {
        Vec3d start = mc.cameraEntity.getCameraPosVec(mc.getRenderTickCounter().getTickDelta(false));
        return mc.world.raycast(new RaycastContext(start, end, RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, mc.cameraEntity));
    }
    public static HitResult rayTraceWithCobwebs(Vec3d endPos) {
        if (mc.player == null || mc.world == null) return null;

        Vec3d startPos = mc.player.getCameraPosVec(1.0F);

        return mc.world.raycast(new RaycastContext(
                startPos, endPos,
                RaycastContext.ShapeType.OUTLINE,
                RaycastContext.FluidHandling.NONE,
                mc.player
        ));
    }


    public static Optional<Vec3d> getPossiblePoints(LivingEntity entity, float shrink) {
        if (entity == null || mc.player == null || mc.world == null) return Optional.empty();

        Set<Vec3d> possiblePoints = new HashSet<>();
        Box bBox = entity.getBoundingBox();
        Vec3d playerPosition = mc.player.getPos().add(0, mc.player.getStandingEyeHeight(), 0);
        float offset = (float) (bBox.getLengthZ() / 2f * shrink);

        for (float x = -offset; x <= offset; x += (float) (bBox.getLengthX() / 25))
            for (float y = 0; y <= bBox.getLengthY(); y += (float) (bBox.getLengthY()) / 25)
                for (float z = -offset; z <= offset; z += (float) bBox.getLengthZ() / 25) {
                    Vec3d point = new Vec3d(bBox.getCenter().x - x, bBox.maxY - y - 0.1f, bBox.getCenter().z - z);
                    if (rayTrace(point).getPos() == point)
                        possiblePoints.add(point);
                }


        return possiblePoints.stream()
                .min(Comparator.comparingDouble(point -> point.squaredDistanceTo(playerPosition)));
    }


    public static float[] getCappedRotations(float[] prev, float[] current) {
        float yawDiff = getDelta(current[0], prev[0]);
        float cappedPYaw = prev[0] + yawDiff;

        float pitchDiff = getDelta(current[1], prev[1]);
        float cappedPitch = prev[1] + pitchDiff;
        return new float[]{cappedPYaw, cappedPitch};
    }

    public static float getDelta(float first, float second) {
        return MathHelper.wrapDegrees(first - second);
    }
}
