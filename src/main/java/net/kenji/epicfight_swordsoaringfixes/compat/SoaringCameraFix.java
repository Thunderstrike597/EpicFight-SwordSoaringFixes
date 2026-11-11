package net.kenji.epicfight_swordsoaringfixes.compat;

import net.kenji.epicfight_swordsoaringfixes.mixins.AccessorCamera;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.Vec3;
import org.jline.utils.Log;
import yesman.epicfight.api.client.event.EpicFightClientHooks;
import yesman.epicfight.api.client.event.types.BuildCameraTransform;
import yesman.epicfight.client.ClientEngine;
import yesman.epicfight.client.world.capabilites.entitypatch.player.LocalPlayerPatch;

public final class SoaringCameraFix {

    private static boolean latched = false; // true from flight start until the camera has fully recovered

    public static void init() {
        EpicFightClientHooks.Camera.BUILD_TRANSFORM_POST.registerEvent(SoaringCameraFix::onCameraPost, 10);
    }

    private static void onCameraPost(BuildCameraTransform.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        Camera camera = event.getCamera();

        if (player == null) { latched = false; return; }
        if (camera.getEntity() != player) return;

        LocalPlayerPatch patch = ClientEngine.getInstance().getPlayerPatch();
        boolean flying = patch != null && SwordSoaringCompat.isFlying(patch);
        if (flying) latched = true;
        if (!latched) return;
        AccessorCamera accessorCamera = ((AccessorCamera)camera);

        // the pose can stay fall-flying for a tick or so after the flag clears
        boolean lowPose = player.isFallFlying() || player.getPose() == Pose.FALL_FLYING;

        double standing = player.getEyeHeight(Pose.STANDING);
        double target = (flying || lowPose) ? standing : player.getEyeHeight();
        double cameraEye = Mth.lerp(event.getPartialTick(), accessorCamera.accessorEyeHeightOld(), accessorCamera.accessorEyeHeight());
        double lift = target - cameraEye;

        if (!flying && !lowPose && Math.abs(lift) < 0.02D) {
            latched = false; // camera has caught up with the real pose
            return;
        }

        Vec3 pos = camera.getPosition();
        accessorCamera.invokeSetPos(pos.x, pos.y + lift, pos.z);
    }
}