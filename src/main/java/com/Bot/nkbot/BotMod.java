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
import net.minecraft.util.math.BlockPos;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class BotMod implements ModInitializer {

    private ZombieEntity botTerrestre;
    private AllayEntity botVolador;
    private ServerPlayerEntity target;

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
                    .executes(context -> {
                        target = EntityArgumentType.getPlayer(context, "target");

                        ServerPlayerEntity invocador = context.getSource().getPlayer();
                        World world = invocador.getWorld();
                        // Validar que el mundo sea ServerWorld
                        if (!(world instanceof ServerWorld serverWorld)) {
                            context.getSource().sendError(Text.literal("Este comando debe ejecutarse en un mundo de servidor."));
                            return 0;
                        }

                        // Invocar el bot volador
                        botVolador = EntityType.ALLAY.create(serverWorld, (allay) -> {
                            allay.setCustomName(Text.literal("Putada Voladora"));
                            allay.setCustomNameVisible(true);
                        }, invocador.getBlockPos().up(), SpawnReason.COMMAND, true, false);

                        // Invocar el bot terrestre
                        botTerrestre = EntityType.ZOMBIE.create(serverWorld, (zombie) -> {
                            zombie.setCustomName(Text.literal("Botito"));
                            zombie.setCustomNameVisible(true);
                        }, invocador.getBlockPos().up(), SpawnReason.COMMAND, true, false);

                        if (botTerrestre != null) {
                            botTerrestre.setPosition(invocador.getX() + 2, invocador.getY(), invocador.getZ());
                            serverWorld.spawnEntity(botTerrestre);
                            context.getSource().sendFeedback(() -> Text.literal("Botito ha sido invocado"), false);
                        }
                        if (botVolador != null) {
                            botVolador.setPosition(invocador.getX() + 5, invocador.getY(), invocador.getZ());
                            serverWorld.spawnEntity(botVolador);
                            context.getSource().sendFeedback(() -> Text.literal("Putada Voladora ha sido invocado"), false);
                        }

                        return 1;
                    }))
            );
        });
    }

    private void registrarEventos() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (botTerrestre != null && botTerrestre.isAlive() && target != null) {
                manejarBotTerrestre(botTerrestre, target);
            }
            if (botVolador != null && botVolador.isAlive()) {
                // Aquí, si se desea que ambos bots tengan el mismo target, usa "target",
                // de lo contrario se puede obtener otro jugador.
                ServerPlayerEntity jugadorObjetivo = (target != null) ? target : server.getPlayerManager().getPlayerList().stream().findFirst().orElse(null);
                if (jugadorObjetivo != null) {
                    manejarBotVolador(botVolador, jugadorObjetivo);
                }
            }
        });
    }

    private void manejarBotTerrestre(ZombieEntity bot, ServerPlayerEntity objetivo) {
        ServerWorld mundo = (ServerWorld) bot.getWorld();

        // Usar el sistema de navegación del bot para moverse hacia el objetivo
        bot.getNavigation().startMovingTo(objetivo, 1.2);

        // Ajustar la orientación para mirar al jugador
        bot.lookAt(EntityAnchorArgumentType.EntityAnchor.EYES, objetivo.getPos().add(0, 1.7, 0));

        // Calcular posición del bloque frente al bot y su cabeza
        BlockPos bloqueFrente = bot.getBlockPos().offset(bot.getHorizontalFacing());
        BlockPos bloqueFrenteCabeza = bloqueFrente.up();

        // Si existe bloque en la posición de la cabeza, romperlo
        if (!mundo.getBlockState(bloqueFrenteCabeza).isAir()) {
            mundo.breakBlock(bloqueFrenteCabeza, true);
        }

        // Si se desea, se puede añadir lógica para saltar sobre obstáculos
        // if (!mundo.getBlockState(bloqueFrente).isAir()) {
        //     bot.jump();
        // }
    }

    private void manejarBotVolador(AllayEntity bot, ServerPlayerEntity objetivo) {
        // Calcular la dirección ajustando la posición objetivo para que el movimiento sea más suave o centralizado.
        Vec3d posBot = bot.getPos();
        Vec3d posObjetivo = objetivo.getPos().add(1.3, 0, 1.3);
        Vec3d direccion = posObjetivo.subtract(posBot).normalize().multiply(0.3); // velocidad configurable

        bot.setVelocity(direccion);
        bot.velocityModified = true;
    }
}
