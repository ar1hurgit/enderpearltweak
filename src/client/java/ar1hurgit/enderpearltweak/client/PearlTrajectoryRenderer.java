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

import java.util.function.Predicate;

public class PearlTrajectoryRenderer {

    public static void render(WorldRenderContext context) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;

        MatrixStack matrices = context.matrixStack();
        Immediate provider = mc.getBufferBuilders().getEntityVertexConsumers();
        Vec3d cameraPos = mc.gameRenderer.getCamera().getPos();

        Box worldBox = new Box(-1e6, -1e6, -1e6, 1e6, 1e6, 1e6);
        Predicate<EnderPearlEntity> filter = pearl -> true;

        for (EnderPearlEntity pearl : mc.world.getEntitiesByType(EntityType.ENDER_PEARL, worldBox, filter)) {
            Vec3d pos = pearl.getPos();
            Vec3d vel = pearl.getVelocity();

            matrices.push();
            VertexConsumer buffer = provider.getBuffer(RenderLayer.getLines());
            MatrixStack.Entry entry = matrices.peek();

            double dt = 1;
            Vec3d prev = pos;
            for (int i = 0; i < 1000; i++) {
                vel = vel.add(0, -0.03 * dt, 0);
                pos = pos.add(vel.multiply(dt));

                // Dessiner segment
                Vec3d start = prev.subtract(cameraPos);
                Vec3d end = pos.subtract(cameraPos);
                buffer.vertex(entry.getPositionMatrix(), (float) start.x, (float) start.y, (float) start.z)
                        .color(1f, 1f, 1f, 1f)
                        .normal(0f, 1f, 0f);
                buffer.vertex(entry.getPositionMatrix(), (float) end.x, (float) end.y, (float) end.z)
                        .color(1f, 1f, 1f, 1f)
                        .normal(0f, 1f, 0f);

                prev = pos;

                if (mc.world.getBlockState(BlockPos.ofFloored(pos)).isSolidBlock(mc.world, BlockPos.ofFloored(pos))) break;
            }

            matrices.pop();
        }

        provider.draw();
    }
}
