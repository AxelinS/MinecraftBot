package com.Bot.nkbot;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.entity.SpawnReason;
import net.minecraft.server.world.ServerWorld;

import static net.minecraft.server.command.CommandManager.literal;

public class BotMod implements ModInitializer {

    private ZombieEntity bot;

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(literal("summonbot").executes(context -> {
                ServerPlayerEntity player = context.getSource().getPlayer();
                World world = player.getWorld();

                bot = EntityType.ZOMBIE.create(
                    (ServerWorld) world,
                    (zombie) -> zombie.setCustomName(Text.literal("Botito")),
                    player.getBlockPos().up(),
                    SpawnReason.COMMAND,
                    true,
                    false
                );
                if (bot != null) {
                    bot.setCustomName(Text.literal("Botito"));
                    bot.setCustomNameVisible(true);
                    bot.setPosition(player.getX() + 2, player.getY(), player.getZ());
                    world.spawnEntity(bot);
                    context.getSource().sendFeedback(() -> Text.literal("Botito ha sido invocado"), false);
                }

                return 1;
            }));
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (bot != null && bot.isAlive()) {
                ServerPlayerEntity target = server.getPlayerManager().getPlayerList().stream().findFirst().orElse(null);
                if (target != null) {
                    Vec3d botPos = bot.getPos();
                    Vec3d playerPos = target.getPos();
                    Vec3d direction = playerPos.subtract(botPos).normalize().multiply(0.1); // velocidad

                    bot.setVelocity(direction);
                    bot.velocityModified = true;
                }
            }
        });
    }
}
