package com.fab;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL10;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
import com.badlogic.gdx.physics.box2d.CircleShape;
import com.badlogic.gdx.physics.box2d.EdgeShape;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.TimeUtils;

/** Base class for all Box2D Testbed tests, all subclasses must implement the createWorld() method.
 * 
 * @author badlogicgames@gmail.com */
public class Game implements ApplicationListener {
 
	protected OrthographicCamera camera;
	protected Box2DDebugRenderer renderer;

	SpriteBatch batch;
	BitmapFont font;

	private World world;
	private Body ground;
	private Body ball;
	private Array<Body> players = new Array<Body>();
	
  String debugMsg = "";

	private static final float DEGTORAD = MathUtils.PI2 / 360;
  private static final float PI  = MathUtils.PI;
  private static final float PI2 = MathUtils.PI2;


  private void applyTorque(Body body, Vector2 direction, float maxangacc) {
    float curAngle = body.getAngle();
    float desAngle = direction.angle() / 360 * PI2; // MathUtils.atan2(delta.y, delta.x);
    /*
    Note: There are two ways to get the angle of a Vector2:
    - Vector2.angle() returns the angle in degrees 0-360 CCW => libgdx does not follow box2d's convention of rads only
    - MathUtils.atan2(delta.y, delta.x) which is in rad
    However:
    - there is a slight numerical discrepancy between both approaches
    - for both arguments negativ, the sign of the results seems to be reversed
      (angle is always positive, while atan2 seems to be between -PI, +PI. 
     */
    //player.setTransform(player.getPosition(), desAngle);
    float totalRotation = desAngle - curAngle;
    while ( totalRotation >  MathUtils.PI ) { totalRotation -= MathUtils.PI2; }
    while ( totalRotation < -MathUtils.PI ) { totalRotation += MathUtils.PI2; }
    
    
    float v = body.getAngularVelocity();
    float I = body.getInertia();
    float remaining = 0.5f * v * v * I * I / maxangacc;

    //Gdx.app.log("", String.format("dx = %8.3f    dy = %8.3f    curAngle = %8.3f    desAngle = %8.3f    origRotation = %8.3f    corrRotation = %8.3f    v = %8.3f    rem = %8.3f", 
    //    delta.x, delta.y, curAngle, desAngle, desAngle-curAngle, totalRotation, v, remaining));
    
    if (Math.abs(totalRotation) > remaining && Math.signum(v)==Math.signum(totalRotation)) {
      if (v > 0) body.applyTorque(+maxangacc/I, true);
      else       body.applyTorque(-maxangacc/I, true);
      //Gdx.app.log("", "accelerate");
    } else {
      if (v > 0) body.applyTorque(-maxangacc/I, true);
      else       body.applyTorque(+maxangacc/I, true);
      //Gdx.app.log("", "brake");
    }    
  }
  
  private void applyForce(Body body, Body ball, float maxacc) {
   
		Vector2 a = ball.getPosition().cpy();
		Vector2 n = ball.getLinearVelocity().cpy().nor();
		Vector2 p = body.getPosition();
		Vector2 ap = a.sub(p);
		Vector2 apn = n.scl(ap.dot(n));
		Vector2 nearest = ap.sub(apn);
		body.applyForceToCenter(nearest.clamp(maxacc, maxacc), true);
    /*
    Vector2 vel = body.getLinearVelocity();
    if (vel.len() > maxvel) {
      body.setLinearVelocity(vel.nor().scl(maxvel));
    }
    */
  }  
  
	@Override
	public void render () {
		
		float maxacc = 1000f;
		float maxvel = 5f;
		float maxangacc = 20f;	
		Vector2 ballPosCur = ball.getPosition().cpy();
		Vector2 ballPosNxt = ball.getPosition().cpy().add(ball.getLinearVelocity().cpy().scl(1f/60f));
		Vector2 ballPosSec = ball.getPosition().cpy().add(ball.getLinearVelocity().cpy().scl(1f));
		for (Body player: players) {
			Vector2 playerPos = player.getPosition();
			Vector2 delta = ballPosCur.cpy().sub(playerPos);

			applyTorque(player, delta, maxangacc);
      //applyForce(player, ball, maxacc);
		}
		
		// update the world with a fixed time step 
		long startTime = TimeUtils.nanoTime();
		world.step(Gdx.app.getGraphics().getDeltaTime(), 3, 3);
		float updateTime = (TimeUtils.nanoTime() - startTime) / 1000000000.0f;

		startTime = TimeUtils.nanoTime();
		// clear the screen and setup the projection matrix
		Gdx.gl.glClear(GL10.GL_COLOR_BUFFER_BIT);
		camera.update();

		// render the world using the debug renderer
		renderer.render(world, camera.combined);
		float renderTime = (TimeUtils.nanoTime() - startTime) / 1000000000.0f;

		batch.begin();
		font.draw(batch, "fps:" + Gdx.graphics.getFramesPerSecond() + ", update: " + updateTime + ", render: " + renderTime + " " + debugMsg, 0, 20);
		batch.end();
	}

	@Override
	public void create () {

		// create the debug renderer
		renderer = new Box2DDebugRenderer();

		// create the world
		world = new World(new Vector2(0, 0), true);
		createWorld(world);

		batch = new SpriteBatch();
		font = new BitmapFont(Gdx.files.internal("data/arial-15.fnt"), false);
	}
	
  void createWorld (World world) {
		
		float gw = 45;
		float gh = 35;
		float radiusPlayers = 1.0f;
		float radiusBall = 0.3f;
		float speedPlayers = 1.0f;
		float speedBall = 10.0f;
		{
		  // create walls
			BodyDef bd = new BodyDef();
			bd.position.set(0, 0);
			ground = world.createBody(bd);

			EdgeShape shape = new EdgeShape();

			FixtureDef fd = new FixtureDef();
			fd.shape = shape;
			fd.density = 0;
			fd.friction = 0.5f;
			fd.restitution = 0.98f;
			
			shape.set(new Vector2(-gw, -gh), new Vector2(-gw,  gh));
			ground.createFixture(fd);
      shape.set(new Vector2( gw, -gh), new Vector2( gw,  gh));
			ground.createFixture(fd);
      shape.set(new Vector2(-gw,  gh), new Vector2( gw,  gh));
			ground.createFixture(fd);
      shape.set(new Vector2(-gw, -gh), new Vector2( gw, -gh));
			ground.createFixture(fd);

			shape.dispose();
		}
		
		{
			// create players
			CircleShape shape = new CircleShape();
			shape.setRadius(radiusPlayers);

			FixtureDef fd = new FixtureDef();
			fd.shape = shape;
			fd.density = 1.0f;
			fd.friction = 0.0f;
			fd.restitution = 1.0f;

			for (int i = 0; i < 10; i++) {
				BodyDef bd = new BodyDef();
				bd.type = BodyType.DynamicBody;
				bd.position.set(MathUtils.random(-gw,+gw), MathUtils.random(-gh,+gh));
				bd.linearVelocity.set(MathUtils.random(speedPlayers), MathUtils.random(speedPlayers));
        bd.angle = MathUtils.PI2 * MathUtils.random(1.0f);
				bd.angularVelocity = 0; // MathUtils.random(1.0f);
				bd.linearDamping = 35.0f;
				
				Body body = world.createBody(bd);
				body.createFixture(fd);
				players.add(body);
			}
			shape.dispose();
	  }
			
		{
			// create ball
			BodyDef bd = new BodyDef();
			bd.type = BodyType.DynamicBody;
			bd.position.set(MathUtils.random(-gw,+gw), MathUtils.random(-gh,+gh));
			bd.linearVelocity.set(MathUtils.random(speedBall), MathUtils.random(speedBall));
			bd.angle = MathUtils.PI2 * MathUtils.random(1.0f);
			bd.angularVelocity = MathUtils.random(1.0f);
			
			CircleShape shape = new CircleShape();
			shape.setRadius(radiusBall);

			FixtureDef fd = new FixtureDef();
			fd.shape = shape;
			fd.density = 0.1f;
			fd.friction = 10.0f;
			fd.restitution = 1.0f;

			ball = world.createBody(bd);
			ball.createFixture(fd);
			shape.dispose();
	  }
	}
	
	@Override
	public void dispose () {
		renderer.dispose();
		world.dispose();

		renderer = null;
		world = null;
	}

	public void pause () {

	}

	public void resume () {

	}

	public void resize (int width, int height) {
		float w = Gdx.graphics.getWidth();
		float h = Gdx.graphics.getHeight();
		float aspect = w / h;
		if (aspect > 1) {
	  	camera = new OrthographicCamera(100*aspect, 100);
	  } else {
	  	camera = new OrthographicCamera(100, 100/aspect);
	  }
		//camera.position.set(0, 0, 0);
		//camera = new OrthographicCamera(100, 100);
		debugMsg = "" + aspect;
	}
}
