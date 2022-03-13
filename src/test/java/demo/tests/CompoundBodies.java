package demo.tests;

import demo.window.TestBedWindow;
import library.dynamics.World;
import library.math.Vec2;

public class CompoundBodies {
    public static final String[] text = {"Compound Bodies:"};

    public static void load(TestBedWindow testBedWindow) {
        testBedWindow.setWorld(new World(new Vec2(0, -9.81)));
        World temp = testBedWindow.getWorld();
    }
}