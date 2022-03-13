package demo.tests;

import demo.window.TestBedWindow;
import library.dynamics.World;
import library.math.Vec2;
import library.rays.ShadowCasting;

import java.awt.*;

public class LineOfSight {
    public static final String[] text = {"Line of sight:", "Mouse: Move mouse to change position of raycast"};
    public static boolean active = false;
    public static ShadowCasting b;

    public static void load(TestBedWindow testBedWindow) {
        testBedWindow.setWorld(new World(new Vec2(0, -9.81)));
        testBedWindow.setCamera(new Vec2(-120, 20), 3.3);
        active = true;

        testBedWindow.generateBoxOfObjects();

        b = new ShadowCasting(new Vec2(-1000, 0), 11000);
        testBedWindow.add(b);
    }

    public static void drawInfo(Graphics2D g, int x, int y) {
        g.drawString("No of rays: " + b.getNoOfRays(), x, y);
    }
}