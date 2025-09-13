package ar1hurgit.enderpearltweak.client;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider.Immediate;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.entity.EntityType;
import net.minecraft.util.math.Box;

import java.util.*;

public class PearlTrajectoryRenderer {

    private static final Map<UUID, List<Vec3d>> trajectories = new HashMap<>();

    public static class SphereRenderer {
        private final Immediate provider;
        private final MatrixStack matrices;
        private final Vec3d cameraPos;
        private final float radius;
        private final int segments;

        public SphereRenderer(Immediate provider, MatrixStack matrices, Vec3d cameraPos, float radius, int segments) {
            this.provider = provider;
            this.matrices = matrices;
            this.cameraPos = cameraPos;
            this.radius = radius;
            this.segments = segments;
        }

        public void drawFilledSphere(Vec3d pos, float r, float g, float b, float a) {
            VertexConsumer buffer = provider.getBuffer(RenderLayer.getSolid());
            MatrixStack.Entry entry = matrices.peek();
            Vec3d center = pos.subtract(cameraPos);

            int latSegments = segments;
            int longSegments = segments;

            for (int i = 0; i < latSegments; i++) {
                double phi1 = Math.PI * i / latSegments;
                double phi2 = Math.PI * (i + 1) / latSegments;

                for (int j = 0; j < longSegments; j++) {
                    double theta1 = 2 * Math.PI * j / longSegments;
                    double theta2 = 2 * Math.PI * (j + 1) / longSegments;

                    Vec3d p1 = new Vec3d(
                            center.x + radius * Math.sin(phi1) * Math.cos(theta1),
                            center.y + radius * Math.cos(phi1),
                            center.z + radius * Math.sin(phi1) * Math.sin(theta1)
                    );
                    Vec3d p2 = new Vec3d(
                            center.x + radius * Math.sin(phi1) * Math.cos(theta2),
                            center.y + radius * Math.cos(phi1),
                            center.z + radius * Math.sin(phi1) * Math.sin(theta2)
                    );
                    Vec3d p3 = new Vec3d(
                            center.x + radius * Math.sin(phi2) * Math.cos(theta2),
                            center.y + radius * Math.cos(phi2),
                            center.z + radius * Math.sin(phi2) * Math.sin(theta2)
                    );
                    Vec3d p4 = new Vec3d(
                            center.x + radius * Math.sin(phi2) * Math.cos(theta1),
                            center.y + radius * Math.cos(phi2),
                            center.z + radius * Math.sin(phi2) * Math.sin(theta1)
                    );

                    Vec3d n1 = p1.subtract(center).normalize();
                    Vec3d n2 = p2.subtract(center).normalize();
                    Vec3d n3 = p3.subtract(center).normalize();
                    Vec3d n4 = p4.subtract(center).normalize();

                    // Triangle 1
                    buffer.vertex(entry.getPositionMatrix(), (float)p1.x, (float)p1.y, (float)p1.z)
                            .color(r,g,b,a).texture(0f,0f).overlay(0,0).light(0xF000F0)
                            .normal((float)n1.x, (float)n1.y, (float)n1.z);
                    buffer.vertex(entry.getPositionMatrix(), (float)p2.x, (float)p2.y, (float)p2.z)
                            .color(r,g,b,a).texture(0f,0f).overlay(0,0).light(0xF000F0)
                            .normal((float)n2.x, (float)n2.y, (float)n2.z);
                    buffer.vertex(entry.getPositionMatrix(), (float)p3.x, (float)p3.y, (float)p3.z)
                            .color(r,g,b,a).texture(0f,0f).overlay(0,0).light(0xF000F0)
                            .normal((float)n3.x, (float)n3.y, (float)n3.z);

                    // Triangle 2
                    buffer.vertex(entry.getPositionMatrix(), (float)p3.x, (float)p3.y, (float)p3.z)
                            .color(r,g,b,a).texture(0f,0f).overlay(0,0).light(0xF000F0)
                            .normal((float)n3.x, (float)n3.y, (float)n3.z);
                    buffer.vertex(entry.getPositionMatrix(), (float)p4.x, (float)p4.y, (float)p4.z)
                            .color(r,g,b,a).texture(0f,0f).overlay(0,0).light(0xF000F0)
                            .normal((float)n4.x, (float)n4.y, (float)n4.z);
                    buffer.vertex(entry.getPositionMatrix(), (float)p1.x, (float)p1.y, (float)p1.z)
                            .color(r,g,b,a).texture(0f,0f).overlay(0,0).light(0xF000F0)
                            .normal((float)n1.x, (float)n1.y, (float)n1.z);
                }
            }
        }

    }

    public static void render(WorldRenderContext context) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;

        MatrixStack matrices = context.matrixStack();
        Immediate provider = mc.getBufferBuilders().getEntityVertexConsumers();
        Vec3d cameraPos = mc.gameRenderer.getCamera().getPos();

        Box worldBox = new Box(-1e6, -1e6, -1e6, 1e6, 1e6, 1e6);

        SphereRenderer sphereRenderer = new SphereRenderer(provider, matrices, cameraPos, 0.1f, 8);

        Iterator<Map.Entry<UUID, List<Vec3d>>> it = trajectories.entrySet().iterator();


        while (it.hasNext()) {
            Map.Entry<UUID, List<Vec3d>> entry = it.next();
            boolean exists = false;
            for (var entity : mc.world.getEntities()) {
                if (entity.getUuid().equals(entry.getKey())) {
                    exists = true;
                    break;
                }
            }
            if (!exists) it.remove();
        }

        // Dessiner les trajectoires
        for (EnderPearlEntity pearl : mc.world.getEntitiesByType(EntityType.ENDER_PEARL, worldBox, e -> true)) {
            UUID id = pearl.getUuid();

            if (!trajectories.containsKey(id)) {
                List<Vec3d> points = new ArrayList<>();
                Vec3d pos = pearl.getPos();
                Vec3d vel = pearl.getVelocity();

                double pointInterval = 1.0;
                double distanceAccumulator = 0;
                Vec3d prev = pos;

                float gravity = 0.03f;
                float drag = 0.99f;

                for (int i = 0; i < 1000; i++) {
                    pos = pos.add(vel);
                    vel = vel.multiply(drag).add(0, -gravity, 0);

                    distanceAccumulator += prev.distanceTo(pos);
                    if (distanceAccumulator >= pointInterval) {
                        points.add(pos);
                        distanceAccumulator = 0;
                    }

                    prev = pos;
                    if (mc.world.getBlockState(BlockPos.ofFloored(pos)).isSolidBlock(mc.world, BlockPos.ofFloored(pos))) break;
                }

                trajectories.put(id, points);
            }

            matrices.push();
            for (Vec3d point : trajectories.get(id)) {
                sphereRenderer.drawFilledSphere(point, 1f, 1f, 1f, 1f);
            }
            matrices.pop();
        }

        provider.draw();
    }
}
