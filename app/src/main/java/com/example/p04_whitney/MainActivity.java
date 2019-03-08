package com.example.p04_whitney;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }
}

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
//import android.media.AudioManager;
//import android.media.SoundPool;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import java.io.IOException;
import java.util.Random;

public class SnakeView extends SurfaceView implements Runnable
{
    private Thread gameThread = null;
    private SurfaceHolder ourHolder;
    private volatile boolean playing;
    private boolean paused = true;
    private Canvas canvas;
    private Paint paint;
    private Context context;
    //Sound
    //private SoundPool soundPool;
    //private int mouseSound = -1;
    //private int deathSound = -1;

    //manifest
    //android:theme="@android:style/Theme.NoTitleBar.Fullscreen"
    //android:screenOrientation="landscape">

    private int[] snakeX, snakeY;
    private int snakeLength, score;
    private int mouseX, mouseY;
    private int height, width;
    public enum Direction {UP, LEFT, DOWN, RIGHT}
    private Direction direction = Direction.RIGHT;      // snake faces right to start by default
    private long nextFrameTime;
    private final long FPS = 10;            // frames per second
    private final long MSPS = 1000;         // milliseconds per second
    private int blockSize;
    private final int NUM_BLOCKS_WIDE = 40;
    private int numBlocksHigh;              // determined dynamically

    public SnakeView(Context c, Point p)
    {
        super(c);
        ourHolder = getHolder();
        paint = new Paint();
        context = c;
        snakeX = new int[200];
        snakeY = new int[200];
        height = p.y;
        width = p.x;
        blockSize = width/NUM_BLOCKS_WIDE;
        numBlocksHigh = height/blockSize;
        //  loadSound();
        startGame();
    }

    @Override
    public void run()
    {
        while(playing)
        {
            if(checkForUpdate())
            {
                update();
                draw();
            }
        }
    }

    public void update()
    {
        if(snakeX[0] == mouseX && snakeY[0] == mouseY)
        {
            eatMouse();
        }
        moveSnake();
        if(detectDeath())
        {
            //soundPool.play(deathSound, 1, 1, 0, 0, 1);
            startGame();
        }
    }

    public void draw()
    {
        if(ourHolder.getSurface().isValid())
        {
            canvas = ourHolder.lockCanvas();
            canvas.drawColor(Color.argb(255, 0, 0, 205));
            paint.setColor(Color.argb(255, 255, 255, 255));
            paint.setTextSize(50);
            canvas.drawText("Score:" + score, 10, 50, paint);
            // draw snake
            for(int i = 0; i < snakeLength; i++)
            {
                float sX = snakeX[i] * blockSize;
                float sY = snakeY[i] * blockSize;
                canvas.drawRect(sX, sY, sX + blockSize, sY + blockSize, paint);
                //canvas.drawRect(snakeX[i] * blockSize, snakeY[i] * blockSize, (snakeX[i] * blockSize) + blockSize, (snakeY[i] * blockSize) + blockSize, paint);
            }
            // draw the mouse
            float mX = mouseX * blockSize;
            float mY = mouseY * blockSize;
            canvas.drawRect(mX, mY, mX + blockSize, mY+ blockSize, paint);
            //canvas.drawRect(mouseX * blockSize, mouseY * blockSize, (mouseX * blockSize) + blockSize, (mouseY * blockSize) + blockSize, paint);
            ourHolder.unlockCanvasAndPost(canvas);
        }
    }

    public void pause()
    {
        playing = false;
        try
        {
            gameThread.join();
        }
        catch (InterruptedException e)
        {
            Log.e("Error: ", "joining thread" );
        }
    }

    public void resume()
    {
        playing = true;
        gameThread = new Thread(this);
        gameThread.start();
    }

    public void startGame()
    {
        snakeLength = 1;                        // start with just a snake made of a single block
        snakeX[0] = NUM_BLOCKS_WIDE / 2;        // start with the snake in the middle of the screen
        snakeY[0] = numBlocksHigh / 2;
        score = 0;
        spawnMouse();                           // spawn a mouse in
        nextFrameTime = System.currentTimeMillis();     // nextFrameTime = now, so update is constantly triggered
    }

    public boolean checkForUpdate()
    {
        if(nextFrameTime <= System.currentTimeMillis())     // tenth of a second has passed
        {
            nextFrameTime = System.currentTimeMillis() + MSPS/FPS;  // update nextFrameTime
            return true;
        }
        return false;
    }

    /*public void loadSound()
    {
        soundPool = new SoundPool(10, AudioManager.STREAM_MUSIC, 0);
        try
        {
            AssetManager assetManager = context.getAssets();
            AssetFileDescriptor descriptor;
            descriptor = assetManager.openFd("mouse_sound.ogg");
            mouseSound = soundPool.load(descriptor, 0);
            descriptor = assetManager.openFd("death_sound.ogg");
            deathSound = soundPool.load(descriptor, 0);
        }
        catch(IOException e)
        {
            Log.e("Error: ", "opening audio file" );
        }
    }*/

    public void spawnMouse()
    {
        Random random = new Random();
        mouseX = random.nextInt(NUM_BLOCKS_WIDE - 1) + 1;
        mouseY = random.nextInt(numBlocksHigh - 1) + 1;
    }

    private void eatMouse()
    {
        snakeLength++;
        spawnMouse();
        score++;
        //soundPool.play(mouseSound, 1, 1, 0, 0, 1);
    }

    private void moveSnake()
    {
        for(int i = snakeLength; i > 0; i--)
        {
            snakeX[i] = snakeX[i-1];                // update snake tail to head by moving each block
            snakeY[i] = snakeY[i-1];                // to the position of the block in front of it
        }
        switch(direction)                           // then update the head based on which direction is selected
        {
            case UP:
                snakeY[0]--;
                break;
            case LEFT:
                snakeX[0]--;
                break;
            case DOWN:
                snakeY[0]++;
                break;
            case RIGHT:
                snakeX[0]++;
                break;
        }
    }

    private boolean detectDeath()
    {
        boolean dead = false;
        // check to see if snake hit a wall
        if(snakeX[0] == -1 ) dead = true;
        if(snakeX[0] >= NUM_BLOCKS_WIDE) dead = true;
        if(snakeY[0] == -1) dead = true;
        if(snakeY[0] >= numBlocksHigh) dead = true;
        // check to see if snake hit itself
        for(int i = snakeLength-1; i > 0; i--)
        {
            if((i > 4) && (snakeX[0] == snakeX[i]) && (snakeY[0] == snakeY[i]))
            {
                dead = true;
            }
        }
        return dead;
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent)
    {
        switch(motionEvent.getAction() & MotionEvent.ACTION_MASK)
        {
            case MotionEvent.ACTION_UP:
                if (motionEvent.getX() >= width / 2)
                {
                    switch(direction)
                    {
                        case UP:
                            direction = Direction.RIGHT;
                            break;
                        case RIGHT:
                            direction = Direction.DOWN;
                            break;
                        case DOWN:
                            direction = Direction.LEFT;
                            break;
                        case LEFT:
                            direction = Direction.UP;
                            break;
                    }
                }
                else
                {
                    switch(direction)
                    {
                        case UP:
                            direction = Direction.LEFT;
                            break;
                        case LEFT:
                            direction = Direction.DOWN;
                            break;
                        case DOWN:
                            direction = Direction.RIGHT;
                            break;
                        case RIGHT:
                            direction = Direction.UP;
                            break;
                    }
                }
        }
        return true;
    }
}
