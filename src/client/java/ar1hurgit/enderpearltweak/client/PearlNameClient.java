package ar1hurgit.enderpearltweak.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.FlyingItemEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public class PearlNameClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(
                net.minecraft.entity.EntityType.ENDER_PEARL,
                (EntityRendererFactory.Context ctx) -> new EnderPearlWithNameRenderer(ctx)
        );
    }

    public static class EnderPearlWithNameRenderer extends FlyingItemEntityRenderer<EnderPearlEntity> {
        public EnderPearlWithNameRenderer(EntityRendererFactory.Context context) {
            super(context);
        }

        @Override
        public void render(EnderPearlEntity pearl, float yaw, float tickDelta,
                           MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
            super.render(pearl, yaw, tickDelta, matrices, vertexConsumers, light);

            String name = pearl.getOwner() != null ? pearl.getOwner().getName().getString() : "null";

            double distSq = this.dispatcher.getSquaredDistanceToCamera(pearl);
            if (distSq > 8192.0D) return; // plus que 128 blocs

            matrices.push();
            matrices.translate(0.0D, 0.1D, 0.0D);
            this.renderLabelIfPresent(pearl, Text.literal(name), matrices, vertexConsumers, light, tickDelta);
            matrices.pop();
        }
    }
}
