/*
 Copyright (c) 2019-2023, Stephen Gold
 All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright
 notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright
 notice, this list of conditions and the following disclaimer in the
 documentation and/or other materials provided with the distribution.
 * Neither the name of the copyright holder nor the names of its contributors
 may be used to endorse or promote products derived from this software without
 specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package ryzomdemos;

import com.jme3.app.Application;
import com.jme3.app.state.AppStateManager;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import java.util.logging.Logger;
import jme3utilities.SimpleAppState;
import jme3utilities.math.MyMath;
import jme3utilities.math.MyVector3f;

/**
 * AppState to update an arrow's position and detect collisions.
 *
 * @author Stephen Gold sgold@sonic.net
 */
public class ArrowAppState extends SimpleAppState {
    // *************************************************************************
    // constants and loggers

    /**
     * flight time to target (in seconds): determines the game's difficulty
     */
    final private static float flightTime = 0.8f;
    /**
     * squared radius of the target cylinder (in world units)
     */
    final private static float targetCylinderR2 = 0.05f;
    /**
     * message logger for this class
     */
    final public static Logger logger
            = Logger.getLogger(ArrowAppState.class.getName());
    /**
     * asset path to the arrow model
     */
    final private static String arrowAssetPath
            = "Models/indicators/arrow/arrow.j3o";
    /**
     * uniform acceleration (wu/sec/sec in world coordinates)
     */
    final private static Vector3f gravity = new Vector3f(0f, -10f, 0f);
    // *************************************************************************
    // fields

    /**
     * reference to the application instance
     */
    private DodgerGame appInstance;
    /**
     * Geometry for visualization
     */
    private Geometry arrowGeometry;
    /**
     * velocity vector (wu/sec in world coordinates)
     */
    final private Vector3f velocity = new Vector3f();
    // *************************************************************************
    // constructors

    /**
     * Instantiate an uninitialized enabled state.
     */
    ArrowAppState() {
        super(true);
    }
    // *************************************************************************
    // new methods exposed

    /**
     * Reset the arrow, causing it to disappear.
     */
    void reset() {
        arrowGeometry.setLocalTranslation(new Vector3f(0f, -0.5f, 5f));
        velocity.zero();
        setVisible(false);
    }

    /**
     * Begin launching the arrow at random intervals.
     */
    void start() {
        assert MyVector3f.isZero(velocity);
        arrowGeometry.setLocalTranslation(new Vector3f(0f, -0.5f, -5f));
    }
    // *************************************************************************
    // SimpleAppState methods

    /**
     * Clean up this AppState during the first update after it gets detached.
     * Should be invoked only by a subclass or by the AppStateManager.
     */
    @Override
    public void cleanup() {
        super.cleanup();

        // Remove the geometry from the scene.
        arrowGeometry.removeFromParent();
    }

    /**
     * Initialize this AppState on the first update after it gets attached.
     *
     * @param sm application's state manager (not null)
     * @param app application which owns this state (not null)
     */
    @Override
    public void initialize(AppStateManager sm, Application app) {
        super.initialize(sm, app);

        appInstance = (DodgerGame) app;

        // Attach the geometry to the scene.
        Node node = (Node) assetManager.loadModel(arrowAssetPath);
        Node node2 = (Node) node.getChild(0);
        Node node3 = (Node) node2.getChild(0);
        arrowGeometry = (Geometry) node3.getChild(0);
        rootNode.attachChild(arrowGeometry);

        setVisible(false);
    }

    /**
     * Callback to update this AppState prior to rendering. (Invoked once per
     * frame while the state is attached and enabled.)
     *
     * @param tpf time interval between frames (in seconds, &ge;0)
     */
    @Override
    public void update(float tpf) {
        super.update(tpf);

        // Update the arrow's velocity and location.
        MyVector3f.accumulateScaled(velocity, gravity, tpf);
        Vector3f offset = velocity.mult(tpf);
        arrowGeometry.move(offset);

        // Rotate the arrow to point in the direction of motion.
        if (velocity.lengthSquared() > 0.01f) {
            Vector3f direction = velocity.normalize();

            Vector3f xDir = direction.clone();
            Vector3f yDir = new Vector3f();
            Vector3f zDir = new Vector3f();
            MyVector3f.generateBasis(xDir, yDir, zDir);
            Quaternion orientation = new Quaternion();
            orientation.fromAxes(xDir, yDir, zDir);
            arrowGeometry.setLocalRotation(orientation);
        }
        /*
         * Check for collision with the character.
         * The character is approximated by a cylinder centered on his pelvis.
         * If the arrow's tip is in that cylinder, the character dies.
         */
        Vector3f characterCenter = appInstance.characterLocation();
        Vector3f arrowTip = arrowGeometry.localToWorld(Vector3f.UNIT_X, null);
        Vector3f off = arrowTip.subtract(characterCenter);
        if (FastMath.abs(off.y) < 1f
                && MyMath.sumOfSquares(off.x, off.z) < targetCylinderR2) {
            reset();
            appInstance.die();
        }

        // Check whether the arrow has passed behind the camera.
        float cameraZ = cam.getLocation().z;
        float arrowZ = arrowGeometry.getLocalTranslation().z;
        if (arrowZ < cameraZ) {
            // There's a random delay before the arrow is re-launched.
            int n = RyzomUtil.generator.nextPoisson(tpf);
            if (n > 0) {
                velocity.zero();
                launch();
                appInstance.incrementScore();
            }
        }
    }
    // *************************************************************************
    // private methods

    /**
     * Launch (or re-launch) the arrow.
     */
    private void launch() {
        assert MyVector3f.isZero(velocity);

        float xInit = -3f + 6f * RyzomUtil.generator.nextFloat();
        float yInit = -0.5f;
        float zInit = 5f;
        arrowGeometry.setLocalTranslation(new Vector3f(xInit, yInit, zInit));

        // Aim for the exact center 50% of the time.
        float xTarget;
        if (RyzomUtil.generator.nextFloat() < 0.5f) {
            xTarget = 0f; // center
        } else {
            xTarget = -1f + 2f * RyzomUtil.generator.nextFloat();
        }
        float yTarget = 1f;
        float zTarget = 0f;
        velocity.x = (xTarget - xInit) / flightTime;
        velocity.y
                = (yTarget - yInit) / flightTime - gravity.y * flightTime / 2f;
        velocity.z = (zTarget - zInit) / flightTime;

        setVisible(true);
    }

    private void setVisible(boolean show) {
        Spatial.CullHint cullHint = show ? Spatial.CullHint.Dynamic
                : Spatial.CullHint.Always;
        arrowGeometry.setCullHint(cullHint);
    }
}
