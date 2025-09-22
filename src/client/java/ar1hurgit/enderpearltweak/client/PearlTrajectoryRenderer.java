package ar1hurgit.enderpearltweak.client;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.*;

public class PearlTrajectoryRenderer {
    private static final Map<UUID, List<Vec3d>> trajectories = new HashMap<>();

    public static class SphereRenderer {
        private final VertexConsumerProvider.Immediate provider;
        private final MatrixStack matrices;
        private final Vec3d cameraPos;
        private final float radius;

        public SphereRenderer(VertexConsumerProvider.Immediate provider, MatrixStack matrices, Vec3d cameraPos, float radius) {
            this.provider = provider;
            this.matrices = matrices;
            this.cameraPos = cameraPos;
            this.radius = radius;
        }

        public void drawIcosahedron(Vec3d center, float r, float g, float b, float a) {
            VertexConsumer buffer = provider.getBuffer(RenderLayer.getEntityTranslucent(Identifier.of("enderpearltweak", "sphere_shader")));

            matrices.push();
            MatrixStack.Entry entry = matrices.peek();
            Vec3d c = center.subtract(cameraPos);

            double phi = (1 + Math.sqrt(5)) / 2.0;

            // Calcul du scale pour que le rayon circonscrit soit 'radius'
            double R = radius;
            double scale = R / Math.sqrt(phi * phi + 1);

            Vec3d[] verts = new Vec3d[] {
                    new Vec3d(-1,  phi, 0).multiply(scale).add(c),
                    new Vec3d( 1,  phi, 0).multiply(scale).add(c),
                    new Vec3d(-1, -phi, 0).multiply(scale).add(c),
                    new Vec3d( 1, -phi, 0).multiply(scale).add(c),
                    new Vec3d(0, -1,  phi).multiply(scale).add(c),
                    new Vec3d(0,  1,  phi).multiply(scale).add(c),
                    new Vec3d(0, -1, -phi).multiply(scale).add(c),
                    new Vec3d(0,  1, -phi).multiply(scale).add(c),
                    new Vec3d( phi, 0, -1).multiply(scale).add(c),
                    new Vec3d( phi, 0,  1).multiply(scale).add(c),
                    new Vec3d(-phi, 0, -1).multiply(scale).add(c),
                    new Vec3d(-phi, 0,  1).multiply(scale).add(c)
            };

            int[][] faces = new int[][] {
                    {0,11,5},{0,5,1},{0,1,7},{0,7,10},{0,10,11},
                    {1,5,9},{5,11,4},{11,10,2},{10,7,6},{7,1,8},
                    {3,9,4},{3,4,2},{3,2,6},{3,6,8},{3,8,9},
                    {4,9,5},{2,4,11},{6,2,10},{8,6,7},{9,8,1}
            };

            for (int[] f : faces) {
                addTriangle(buffer, entry, verts[f[0]], verts[f[1]], verts[f[2]], r, g, b, a);
            }

            matrices.pop();
        }


        private void addTriangle(VertexConsumer buffer, MatrixStack.Entry entry, Vec3d v1, Vec3d v2, Vec3d v3,
                                 float r, float g, float b, float a) {
            Vec3d normal = v2.subtract(v1).crossProduct(v3.subtract(v1)).normalize();
            buffer.vertex(entry.getPositionMatrix(), (float)v1.x, (float)v1.y, (float)v1.z)
                    .color(r, g, b, a).texture(0f,0f).overlay(OverlayTexture.DEFAULT_UV)
                    .light(0xF000F0).normal((float)normal.x,(float)normal.y,(float)normal.z);
            buffer.vertex(entry.getPositionMatrix(), (float)v2.x, (float)v2.y, (float)v2.z)
                    .color(r, g, b, a).texture(0f,0f).overlay(OverlayTexture.DEFAULT_UV)
                    .light(0xF000F0).normal((float)normal.x,(float)normal.y,(float)normal.z);
            buffer.vertex(entry.getPositionMatrix(), (float)v3.x, (float)v3.y, (float)v3.z)
                    .color(r, g, b, a).texture(0f,0f).overlay(OverlayTexture.DEFAULT_UV)
                    .light(0xF000F0).normal((float)normal.x,(float)normal.y,(float)normal.z);
        }
    }

    public static void render(WorldRenderContext context) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;

        MatrixStack matrices = context.matrixStack();
        VertexConsumerProvider.Immediate provider = mc.getBufferBuilders().getEntityVertexConsumers();
        Vec3d cameraPos = mc.gameRenderer.getCamera().getPos();
        Box worldBox = new Box(-1e6, -1e6, -1e6, 1e6, 1e6, 1e6);

        Iterator<Map.Entry<UUID, List<Vec3d>>> it = trajectories.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, List<Vec3d>> entry = it.next();
            boolean found = false;
            for (EnderPearlEntity pearl : mc.world.getEntitiesByType(EntityType.ENDER_PEARL, worldBox, e -> true)) {
                if (entry.getKey().equals(pearl.getUuid())) { found = true; break; }
            }
            if (!found) it.remove();
        }

        for (EnderPearlEntity pearl : mc.world.getEntitiesByType(EntityType.ENDER_PEARL, worldBox, e -> true)) {
            UUID id = pearl.getUuid();
            trajectories.computeIfAbsent(id, k -> {
                List<Vec3d> points = new ArrayList<>();
                Vec3d pos = pearl.getPos();
                Vec3d vel = pearl.getVelocity();
                double pointInterval = 0.5;
                double distanceAccumulator = 0;
                Vec3d prev = pos;
                float gravity = 0.03f;
                float drag = 0.99f;
                for (int i = 0; i < 1000; i++) {
                    pos = pos.add(vel);
                    vel = vel.multiply(drag).add(0, -gravity, 0);
                    distanceAccumulator += prev.distanceTo(pos);
                    if (distanceAccumulator >= pointInterval) {
                        points.add(new Vec3d(pos.x, pos.y, pos.z));
                        distanceAccumulator = 0;
                    }
                    prev = pos;
                    if (mc.world.getBlockState(BlockPos.ofFloored(pos)).isSolidBlock(mc.world, BlockPos.ofFloored(pos))) break;
                }
                return points;
            });

            VertexConsumer buffer = provider.getBuffer(RenderLayer.getLines());
            matrices.push();
            MatrixStack.Entry entry = matrices.peek();
            List<Vec3d> points = trajectories.get(id);
            for (int i = 1; i < points.size(); i++) {
                Vec3d p1 = points.get(i - 1).subtract(cameraPos);
                Vec3d p2 = points.get(i).subtract(cameraPos);
                buffer.vertex(entry.getPositionMatrix(), (float)p1.x, (float)p1.y, (float)p1.z)
                        .color(1f, 1f, 1f, 0.5f).normal(2,2,2);
                buffer.vertex(entry.getPositionMatrix(), (float)p2.x, (float)p2.y, (float)p2.z)
                        .color(1f, 1f, 1f, 0.5f).normal(2,2,2);
            }
            matrices.pop();
        }
        provider.draw();
    }
}
