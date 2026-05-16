package com.duntale.dungeongen.command;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

/**
 * Opens the standalone dungeon generation UI page.
 *
 * <p>Usage: {@code /dungen}</p>
 *
 * @since 1.0.3
 */
public class DungenCommand extends AbstractPlayerCommand {

    public DungenCommand() {
        super("dungen", "Open the dungeon generator UI");
    }

    @Override
    protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
                           @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef,
                           @Nonnull World world) {
        Player playerComponent = store.getComponent(ref, Player.getComponentType());
        if (playerComponent == null) {
            return;
        }

        playerComponent.getPageManager().openCustomPage(ref, store,
                new DungeonGeneratePage(playerRef, world));
    }
}
