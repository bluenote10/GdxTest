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

/** Base class for all Box2D Testbed tests, all subclasses must implement the createWorld() method.
 * 
 * @author badlogicgames@gmail.com */
public class Game implements ApplicationListener {
 
	protected OrthographicCamera camera;
	protected Box2DDebugRenderer renderer;

	SpriteBatch batch;
	BitmapFont font;

	private World world;
  String debugMsg = "";


	@Override
	public void render () {
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
		world = new World(new Vector2(0, -10), true);
		createWorld(world);

		batch = new SpriteBatch();
		font = new BitmapFont(Gdx.files.internal("data/arial-15.fnt"), false);
	}
	
  void createWorld (World world) {
		world.setGravity(new Vector2(0, 0));
		Body ground;
		
		float gw = 35;
		float gh = 45;
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
			CircleShape shape = new CircleShape();
			shape.setRadius(0.3f);

			FixtureDef fd = new FixtureDef();
			fd.shape = shape;
			fd.density = 1.0f;
			fd.friction = 0.5f;
			fd.restitution = 1.0f;

			for (int i = 0; i < 100; i++) {
				BodyDef bd = new BodyDef();
				bd.type = BodyType.DynamicBody;
				bd.position.set(MathUtils.random(-gw,+gw), MathUtils.random(-gh,+gh));
				bd.linearVelocity.set(MathUtils.random(10.0f), MathUtils.random(10.0f));
        bd.angle = MathUtils.PI2 * MathUtils.random(1.0f);
				bd.angularVelocity = MathUtils.random(1.0f);
				
				Body body = world.createBody(bd);
				body.createFixture(fd);
			}
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
	  	camera = new OrthographicCamera(100, 100*aspect);
	  }
		//camera.position.set(0, 0, 0);
		//camera = new OrthographicCamera(100, 100);
		debugMsg = "" + aspect;
	}
}
