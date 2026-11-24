package dev.aether.render;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import java.util.List;

public final class EspRenderer {
    public record Item(Box box, int rgb, int outlineA, int fillA) {}

    // ---- Custom pipelines/layers so we can toggle depth testing (through-walls) ----
    private static final RenderPipeline LINES_XRAY = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.RENDERTYPE_LINES_SNIPPET)
                    .withLocation("pipeline/aether_lines_xray")
                    .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                    .withDepthWrite(false)
                    .build());

    private static RenderPipeline fillPipe(String name, DepthTestFunction f) {
        return RenderPipelines.register(RenderPipeline.builder(RenderPipelines.POSITION_COLOR_SNIPPET)
                .withLocation("pipeline/" + name)
                .withVertexFormat(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.QUADS)
                .withBlend(BlendFunction.TRANSLUCENT)
                .withCull(false)
                .withDepthTestFunction(f)
                .withDepthWrite(false)
                .build());
    }
    private static final RenderPipeline FILL_XRAY  = fillPipe("aether_fill_xray", DepthTestFunction.NO_DEPTH_TEST);
    private static final RenderPipeline FILL_DEPTH = fillPipe("aether_fill_depth", DepthTestFunction.LEQUAL_DEPTH_TEST);

    private static final RenderLayer L_LINES_XRAY = RenderLayer.of("aether_lines_xray", RenderSetup.builder(LINES_XRAY).build());
    private static final RenderLayer L_FILL_XRAY  = RenderLayer.of("aether_fill_xray", RenderSetup.builder(FILL_XRAY).translucent().build());
    private static final RenderLayer L_FILL_DEPTH = RenderLayer.of("aether_fill_depth", RenderSetup.builder(FILL_DEPTH).translucent().build());

    private static final int[][] FACES = {{0,1,2,3},{4,7,6,5},{0,4,5,1},{1,5,6,2},{2,6,7,3},{3,7,4,0}};

    public static void fills(MatrixStack m, VertexConsumerProvider buf, List<Item> items, boolean xray) {
        VertexConsumer c = buf.getBuffer(xray ? L_FILL_XRAY : L_FILL_DEPTH);
        Matrix4f mat = m.peek().getPositionMatrix();
        for (Item it : items) {
            if (it.fillA() <= 0) continue;
            int argb = (Math.min(255, it.fillA()) << 24) | it.rgb();
            Box b = it.box();
            float x0 = (float) b.minX, y0 = (float) b.minY, z0 = (float) b.minZ;
            float x1 = (float) b.maxX, y1 = (float) b.maxY, z1 = (float) b.maxZ;
            float[][] v = {{x0,y0,z0},{x1,y0,z0},{x1,y0,z1},{x0,y0,z1},{x0,y1,z0},{x1,y1,z0},{x1,y1,z1},{x0,y1,z1}};
            for (int[] f : FACES) for (int i : f)
                c.vertex(mat, v[i][0], v[i][1], v[i][2]).color(argb);
        }
    }

    public static void outlines(MatrixStack m, VertexConsumerProvider buf, List<Item> items, boolean xray, float width) {
        VertexConsumer c = buf.getBuffer(xray ? L_LINES_XRAY : RenderLayers.lines());
        Matrix4f mat = m.peek().getPositionMatrix();
        for (Item it : items) {
            if (it.outlineA() <= 0) continue;
            int argb = (Math.min(255, it.outlineA()) << 24) | it.rgb();
            boxEdges(c, mat, it.box(), argb, width);
        }
    }

    public static void tracers(MatrixStack m, VertexConsumerProvider buf, Vec3d start, List<Item> items, boolean xray, float width) {
        VertexConsumer c = buf.getBuffer(xray ? L_LINES_XRAY : RenderLayers.lines());
        Matrix4f mat = m.peek().getPositionMatrix();
        for (Item it : items) {
            int a = Math.max(Math.max(it.outlineA(), it.fillA()), 90);
            int argb = (Math.min(255, a) << 24) | it.rgb();
            Vec3d e = it.box().getCenter();
            Vec3d d = e.subtract(start);
            double len = Math.sqrt(d.lengthSquared());
            if (len < 1e-4) continue;
            float nx = (float) (d.x / len), ny = (float) (d.y / len), nz = (float) (d.z / len);
            c.vertex(mat, (float) start.x, (float) start.y, (float) start.z).color(argb).normal(nx, ny, nz).lineWidth(width);
            c.vertex(mat, (float) e.x, (float) e.y, (float) e.z).color(argb).normal(nx, ny, nz).lineWidth(width);
        }
    }

    private static void edge(VertexConsumer c, Matrix4f mat, float x0, float y0, float z0, float x1, float y1, float z1, int argb, float w) {
        float dx = x1 - x0, dy = y1 - y0, dz = z1 - z0;
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 1e-6f) return;
        dx /= len; dy /= len; dz /= len;
        c.vertex(mat, x0, y0, z0).color(argb).normal(dx, dy, dz).lineWidth(w);
        c.vertex(mat, x1, y1, z1).color(argb).normal(dx, dy, dz).lineWidth(w);
    }

    /** 12 box edges written straight to the buffer (no per-box shape allocation). */
    private static void boxEdges(VertexConsumer c, Matrix4f mat, Box b, int argb, float w) {
        float x0 = (float) b.minX, y0 = (float) b.minY, z0 = (float) b.minZ, x1 = (float) b.maxX, y1 = (float) b.maxY, z1 = (float) b.maxZ;
        edge(c, mat, x0, y0, z0, x1, y0, z0, argb, w); edge(c, mat, x0, y0, z1, x1, y0, z1, argb, w);
        edge(c, mat, x0, y1, z0, x1, y1, z0, argb, w); edge(c, mat, x0, y1, z1, x1, y1, z1, argb, w);
        edge(c, mat, x0, y0, z0, x0, y1, z0, argb, w); edge(c, mat, x1, y0, z0, x1, y1, z0, argb, w);
        edge(c, mat, x0, y0, z1, x0, y1, z1, argb, w); edge(c, mat, x1, y0, z1, x1, y1, z1, argb, w);
        edge(c, mat, x0, y0, z0, x0, y0, z1, argb, w); edge(c, mat, x1, y0, z0, x1, y0, z1, argb, w);
        edge(c, mat, x0, y1, z0, x0, y1, z1, argb, w); edge(c, mat, x1, y1, z0, x1, y1, z1, argb, w);
    }

    /** Connected line segments (points are camera-relative). */
    public static void polyline(MatrixStack m, VertexConsumerProvider buf, List<Vec3d> pts, int argb, float width, boolean xray) {
        if (pts.size() < 2) return;
        VertexConsumer c = buf.getBuffer(xray ? L_LINES_XRAY : RenderLayers.lines());
        Matrix4f mat = m.peek().getPositionMatrix();
        for (int i = 0; i + 1 < pts.size(); i++) {
            Vec3d a = pts.get(i), b = pts.get(i + 1);
            edge(c, mat, (float) a.x, (float) a.y, (float) a.z, (float) b.x, (float) b.y, (float) b.z, argb, width);
        }
    }
}
