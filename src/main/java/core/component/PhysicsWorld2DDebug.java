package core.component;

import core.physics.PhysicsWorld2D;

public class PhysicsWorld2DDebug extends Component{
    public boolean debugCollisions = false;

    @Override
    public void update(){
        PhysicsWorld2D.DEBUG_COLLISIONS = debugCollisions;
    }
}
