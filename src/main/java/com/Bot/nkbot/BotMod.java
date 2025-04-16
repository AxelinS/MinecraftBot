package com.Bot.nkbot;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.Blocks;
import net.minecraft.command.argument.EntityAnchorArgumentType;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.VexEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.passive.AllayEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.entity.SpawnReason;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

import com.mojang.brigadier.arguments.StringArgumentType;

public class BotMod implements ModInitializer {

    private ZombieEntity botTerrestre;
    private AllayEntity botAllay;
    private VexEntity botVex;
    private ServerPlayerEntity target;
    private String tipo;

    @Override
    public void onInitialize() {
        registrarComando();
        registrarEventos();
    }

    private void registrarComando() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(
                literal("summonbot")
                    .then(argument("target", EntityArgumentType.player())
                    .then(argument("tipo", StringArgumentType.word())
                    .executes(context -> {
                        target = EntityArgumentType.getPlayer(context, "target");
                        tipo = StringArgumentType.getString(context, "tipo");
                        
                        ServerPlayerEntity invocador = context.getSource().getPlayer();
                        World world = invocador.getWorld();
                        // Validar que el mundo sea ServerWorld
                        if (!(world instanceof ServerWorld serverWorld)) {
                            context.getSource().sendError(Text.literal("Este comando debe ejecutarse en un mundo de servidor."));
                            return 0;
                        }

                        switch (tipo){
                            case "allay":
                                // Invoca el bot volador
                                botAllay = EntityType.ALLAY.create(serverWorld, (allay) -> {
                                    allay.setCustomName(Text.literal("Cosa Voladora"));
                                    allay.setCustomNameVisible(true);
                                }, invocador.getBlockPos().up(), SpawnReason.COMMAND, true, false);
                                if (botAllay != null) {
                                    botAllay.setPosition(invocador.getX() + 5, invocador.getY(), invocador.getZ());
                                    serverWorld.spawnEntity(botAllay);
                                    context.getSource().sendFeedback(() -> Text.literal("Putada Voladora ha sido invocado"), false);
                                }
                            break;
                            case "vex":
                                // Invoca el bot asesino volador
                                botVex = EntityType.VEX.create(serverWorld, (vex) -> {
                                    vex.setCustomName(Text.literal("Vex durisimo"));
                                    vex.setCustomNameVisible(true);
                                    vex.setHealth(199);
                                }, invocador.getBlockPos().up(), SpawnReason.COMMAND, true, false);
                                if (botVex != null) {
                                    botVex.setPosition(invocador.getX() + 5, invocador.getY(), invocador.getZ());
                                    serverWorld.spawnEntity(botVex);
                                    context.getSource().sendFeedback(() -> Text.literal("Putada Voladora ha sido invocado"), false);
                                }
                            break;
                            case "zombie":
                                // Invoca el bot terrestre
                                botTerrestre = EntityType.ZOMBIE.create(serverWorld, (zombie) -> {
                                    zombie.setCustomName(Text.literal("Botito"));
                                    zombie.setCustomNameVisible(true);
                                    zombie.setHealth(199);
                                    zombie.setBaby(true);
                                }, invocador.getBlockPos().up(), SpawnReason.COMMAND, true, false);

                                if (botTerrestre != null) {
                                    botTerrestre.setPosition(invocador.getX() + 2, invocador.getY(), invocador.getZ());
                                    serverWorld.spawnEntity(botTerrestre);
                                    context.getSource().sendFeedback(() -> Text.literal("Botito ha sido invocado"), false);
                                }
                            break;
                        }
                        return 1;
                    }))
            ));
        });
    }

    private void registrarEventos() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (botTerrestre != null && botTerrestre.isAlive() && target != null) {
                manejarBotTerrestre(botTerrestre, target);
            }
            if (botAllay != null && botAllay.isAlive()) {
                ServerPlayerEntity jugadorObjetivo = (target != null) ? target : server.getPlayerManager().getPlayerList().stream().findFirst().orElse(null);
                if (jugadorObjetivo != null) {
                    manejarBotAllay(botAllay, jugadorObjetivo);
                }
            }
            if (botVex != null && botVex.isAlive()) {
                ServerPlayerEntity jugadorObjetivo = (target != null) ? target : server.getPlayerManager().getPlayerList().stream().findFirst().orElse(null);
                if (jugadorObjetivo != null) {
                    manejarBotVex(botVex, jugadorObjetivo);
                }
            }
        });
    }

    private void manejarBotTerrestre(ZombieEntity bot, ServerPlayerEntity objetivo) {
        ServerWorld mundo = (ServerWorld) bot.getWorld();

        Vec3d objetivo_pos = objetivo.getPos();

        // Se mueve hacia el target
        bot.getNavigation().startMovingTo(objetivo, 1.2);

        // Mira al target
        bot.lookAt(EntityAnchorArgumentType.EntityAnchor.EYES, objetivo_pos.add(0, 1.7, 0));

        // Si existe bloque frente su cabeza lo rompe
        BlockPos bloqueFrente = bot.getBlockPos().offset(bot.getHorizontalFacing());
        BlockPos bloqueFrenteCabeza = bloqueFrente.up();
        if (!mundo.getBlockState(bloqueFrenteCabeza).isAir()) {
            mundo.breakBlock(bloqueFrenteCabeza, true);
        }

        Vec3d bot_pos = bot.getPos();
        // Diferencia de altura entre el target y el bot si target esta mas arriba el bot salta
        double diferenciaAltura = objetivo_pos.getY() - bot_pos.getY();
        if (diferenciaAltura >= 2.1){
            bot.jump();
            BlockPos botBloqueAbajo = bot.getBlockPos().down();
            if(mundo.getBlockState(botBloqueAbajo).isAir()){
                mundo.setBlockState(botBloqueAbajo, Blocks.DIRT.getDefaultState());
            }
        }

        // if (!mundo.getBlockState(bloqueFrente).isAir()) {
        //     bot.jump();
        // }
    }

    private void manejarBotAllay(AllayEntity bot, ServerPlayerEntity objetivo) {
        Vec3d posBot = bot.getPos();
        Vec3d posObjetivo = objetivo.getPos().add(1.3, 0, 1.3);
        Vec3d direccion = posObjetivo.subtract(posBot).normalize().multiply(0.3);

        bot.setVelocity(direccion);
        bot.velocityModified = true;
    }
    
    private void manejarBotVex(VexEntity bot, ServerPlayerEntity objetivo) {
        Vec3d posBot = bot.getPos();
        Vec3d posObjetivo = objetivo.getPos();
        Vec3d direccion = posObjetivo.subtract(posBot).normalize().multiply(0.3);

        bot.setVelocity(direccion);
        bot.velocityModified = true;
    }
}
