package io.github.astelev.around_planet.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import io.github.astelev.around_planet.network.PlasmaPayload;
import io.github.astelev.around_planet.plasma.PlasmaMath;
import io.github.astelev.around_planet.space.SpaceVector;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Vector3d;

/** Optional Sable implementation, resolved only after an actual hot-ship sample arrives. */
final class PlasmaMeshRenderer {
    static void render(RenderLevelStageEvent event, PlasmaPayload payload) {
        var mc = Minecraft.getInstance();
        var container = SubLevelContainer.getContainer(mc.level); if (container == null) return;
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        PropulsionExhaustRenderer.shader.safeGetUniform("FlowTime").set((float)(((mc.level.getGameTime() + partial) % 24000) / 20));
        var camera = event.getCamera().getPosition(); var matrix = event.getPoseStack().last().pose();
        for (var sample : payload.samples()) {
            var ship = container.getSubLevel(sample.ship()); if (ship == null || ship.isRemoved()) continue;
            double heat = PlasmaMath.intensity(sample.velocity().length(), sample.density()); if (heat <= .001) continue;
            var bounds = ship.boundingBox();
            if (!event.getFrustum().isVisible(new net.minecraft.world.phys.AABB(bounds.minX() - 2, bounds.minY() - 2, bounds.minZ() - 2, bounds.maxX() + 2, bounds.maxY() + 2, bounds.maxZ() + 2))) continue;
            var mesh = PlasmaHullCache.get((ClientSubLevel)ship); if (mesh.hull().faces().isEmpty()) continue;
            var pose = ((ClientSubLevel)ship).renderPose(partial);
            var direction = new Vector3d(sample.velocity().x(), sample.velocity().y(), sample.velocity().z()).normalize();
            var localDirection = pose.orientation().transformInverse(new Vector3d(direction));
            var origin = mesh.origin(); var center = mesh.hull().center();
            var centerWorld = pose.transformPosition(new Vector3d(center.x() + origin.getX(), center.y() + origin.getY(), center.z() + origin.getZ()));
            var eye = new Vector3d(camera.x, camera.y, camera.z).sub(centerWorld).normalize();
            var consumer = DeferredExhaust.vertices();
            for (int shell = 0; shell < 3; shell++) for (var face : mesh.hull().faces()) {
                var n = face.normal(); var normal = pose.orientation().transform(new Vector3d(n.x(), n.y(), n.z()));
                double wind = normal.dot(direction), leading = Math.max(0, wind);
                if (wind < -.35) continue;
                double edge = 1 - Math.abs(normal.dot(eye));
                float alpha = (float)(heat * (.10 + .65 * leading * leading + .18 * edge * edge) * (shell == 0 ? .65 : shell == 1 ? .30 : .10));
                var points = face.vertices();
                for (int i = 1; i + 1 < points.size(); i++) {
                    var a = points.getFirst(); var b = points.get(i); var c = points.get(i + 1);
                    vertex(consumer, matrix, a, center, origin, pose, camera, localDirection, shell, leading, alpha);
                    vertex(consumer, matrix, b, center, origin, pose, camera, localDirection, shell, leading, alpha);
                    vertex(consumer, matrix, c, center, origin, pose, camera, localDirection, shell, leading, alpha);
                    vertex(consumer, matrix, c, center, origin, pose, camera, localDirection, shell, leading, alpha);
                    PlasmaRenderer.triangles++;
                }
            }
            PlasmaRenderer.draws++;
        }
    }
    private static void vertex(VertexConsumer out, Matrix4f matrix, SpaceVector p, SpaceVector center, net.minecraft.core.BlockPos origin,
            dev.ryanhcode.sable.companion.math.Pose3dc pose, net.minecraft.world.phys.Vec3 camera, Vector3d airflow, int shell, double leading, float alpha) {
        double scale = 1 + shell * .025;
        double x = center.x() + (p.x() - center.x()) * scale, y = center.y() + (p.y() - center.y()) * scale, z = center.z() + (p.z() - center.z()) * scale;
        var world = pose.transformPosition(new Vector3d(x + origin.getX(), y + origin.getY(), z + origin.getZ()));
        float u = (float)(4 + Math.atan2(p.z() - center.z(), p.x() - center.x()) / (2 * Math.PI) + .5);
        float distance = (float)(-p.x() * airflow.x - p.y() * airflow.y - p.z() * airflow.z);
        out.addVertex(matrix, (float)(world.x - camera.x), (float)(world.y - camera.y), (float)(world.z - camera.z))
                .setColor(255, (int)(85 + 150 * leading), (int)(20 + 150 * leading), (int)(255 * alpha)).setUv(u, distance);
    }
    private PlasmaMeshRenderer() {}
}
