package com.fab;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.GL10;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.QueryCallback;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.physics.box2d.joints.MouseJoint;
import com.badlogic.gdx.physics.box2d.joints.MouseJointDef;
import com.badlogic.gdx.utils.TimeUtils;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.utils.*;

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


	@Override
	public void render () {
		
		float acc = 1000f;
		float maxvel = 5f;
		float maxangacc = 10f;	
		Vector2 ballPos = ball.getPosition();
		for (Body player: players) {
			Vector2 playerPos = player.getPosition();
			Vector2 delta = ballPos.cpy().sub(playerPos);
			/*
			player.applyForceToCenter(delta.clamp(acc, acc), true);
			Vector2 vel = player.getLinearVelocity();
			if (vel.len() > maxvel) {
				player.setLinearVelocity(vel.nor().scl(maxvel));
			}
			*/
			float curAngle = player.getAngle();
			float desAngle = MathUtils.atan2(delta.y, delta.x);  //delta.angle();
			//player.setTransform(player.getPosition(), desAngle);
			float totalRotation = desAngle - curAngle;
			while ( totalRotation < -MathUtils.PI ) { totalRotation += MathUtils.PI2; }
			while ( totalRotation >  MathUtils.PI ) { totalRotation -= MathUtils.PI2; }
			float v = player.getAngularVelocity();
			float I = player.getInertia();
			float remaining = 0.5f * v * v * I * I / maxangacc;
			//player.applyTorque(totalRotation < 0 ? -10 : 10, true);
			if (Math.abs(totalRotation) > remaining) {
				if (v > 0) player.applyTorque(+maxangacc/I, true);
				else                   player.applyTorque(-maxangacc/I, true);
			} else {
				if (v > 0) player.applyTorque(-maxangacc/I, true);
				else                   player.applyTorque(+maxangacc/I, true);
			}
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
		float speedBall = 50.0f;
		{
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
			fd.friction = 0.5f;
			fd.restitution = 1.0f;

			for (int i = 0; i < 10; i++) {
				BodyDef bd = new BodyDef();
				bd.type = BodyType.DynamicBody;
				bd.position.set(MathUtils.random(-gw,+gw), MathUtils.random(-gh,+gh));
				bd.linearVelocity.set(MathUtils.random(speedPlayers), MathUtils.random(speedPlayers));
        bd.angle = MathUtils.PI2 * MathUtils.random(1.0f);
				bd.angularVelocity = MathUtils.random(1.0f);
				bd.linearDamping = 0.1f;
				
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
			fd.density = 0.2f;
			fd.friction = 1.0f;
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
