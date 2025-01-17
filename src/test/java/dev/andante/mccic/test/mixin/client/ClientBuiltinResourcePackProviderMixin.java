package dev.andante.mccic.test.mixin.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.resource.ClientBuiltinResourcePackProvider;
import net.minecraft.resource.ResourcePackProfile;
import net.minecraft.resource.ResourcePackSource;
import net.minecraft.resource.metadata.PackResourceMetadata;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.io.File;
import java.util.concurrent.CompletableFuture;

@Environment(EnvType.CLIENT)
@Mixin(ClientBuiltinResourcePackProvider.class)
public class ClientBuiltinResourcePackProviderMixin {
    @Shadow private @Nullable ResourcePackProfile serverContainer;

    @Inject(
        method = "loadServerPack(Ljava/io/File;Lnet/minecraft/resource/ResourcePackSource;)Ljava/util/concurrent/CompletableFuture;",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/MinecraftClient;reloadResourcesConcurrently()Ljava/util/concurrent/CompletableFuture;",
            shift = At.Shift.BEFORE
        ),
        locals = LocalCapture.CAPTURE_FAILHARD
    )
    private void onLoadServerPack(File packZip, ResourcePackSource packSource, CallbackInfoReturnable<CompletableFuture<Void>> cir, PackResourceMetadata meta) {
        this.serverContainer = new ResourcePackProfile(this.serverContainer.getName(), false, this.serverContainer::createResourcePack, this.serverContainer.getDisplayName(), this.serverContainer.getDescription(), this.serverContainer.getCompatibility(), this.serverContainer.getInitialPosition(), false, this.serverContainer.getSource());
    }
}
