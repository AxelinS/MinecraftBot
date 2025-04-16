package com.Bot.nkbot;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.command.argument.EntityAnchorArgumentType;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.passive.AllayEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.entity.SpawnReason;
import net.minecraft.server.world.ServerWorld;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class BotMod implements ModInitializer {

    private ZombieEntity bot;
    private AllayEntity bot_fly;
    private ServerPlayerEntity target;

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(
                literal("summonbot")
                .then(argument("target", EntityArgumentType.player())
                .executes(context -> {
                    target = EntityArgumentType.getPlayer(context, "target");

                    ServerPlayerEntity player = context.getSource().getPlayer();
                    World world = player.getWorld();

                    bot_fly = EntityType.ALLAY.create(
                        (ServerWorld) world,
                        (allay) -> allay.setCustomName(Text.literal("putada voladora")),
                        player.getBlockPos().up(),
                        SpawnReason.COMMAND,
                        true,
                        false
                    );

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
                    if (bot_fly != null) {
                        bot_fly.setCustomName(Text.literal("putada voladora"));
                        bot_fly.setCustomNameVisible(true);
                        bot_fly.setPosition(player.getX() + 5, player.getY(), player.getZ());
                        world.spawnEntity(bot_fly);
                        context.getSource().sendFeedback(() -> Text.literal("putada voladora ha sido invocado"), false);
                    }

                    return 1;
                }))
            );
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (bot != null && bot.isAlive()) {
                if (target != null) {
                    bot.lookAt(EntityAnchorArgumentType.EntityAnchor.EYES, target.getPos());
                    
                    Vec3d botPos = bot.getPos();
                    Vec3d playerPos = target.getPos();
                    Vec3d direction = playerPos.subtract(botPos).normalize().multiply(0.22); // velocidad

                    bot.setVelocity(direction);
                    bot.velocityModified = true;
                }
            }
            if (bot_fly != null && bot_fly.isAlive()) {
                ServerPlayerEntity target = server.getPlayerManager().getPlayerList().stream().findFirst().orElse(null);
                if (target != null) {
                    Vec3d botPos = bot_fly.getPos();
                    Vec3d playerPos = target.getPos();
                    playerPos = playerPos.add(1.3, 0, 1.3);
                    Vec3d direction = playerPos.subtract(botPos).normalize().multiply(0.3); // velocidad

                    bot_fly.setVelocity(direction);
                    bot_fly.velocityModified = true;
                }
            }
        });
    }
}
