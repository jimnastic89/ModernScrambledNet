package org.hermit.android.core;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.jimnastic.modernscramblednet.Timer;

public abstract class SurfaceRunner
        extends SurfaceView
        implements SurfaceHolder.Callback
{
    // Create a SurfaceRunner instance
    public SurfaceRunner(Context app)
    {
        super(app);
        init();
    }

    // Create a SurfaceRunner instance
    public SurfaceRunner(Context app, AttributeSet attrs)
    {
        super(app, attrs);
        init();
    }

    //Initialize this SurfaceRunner instance
    private void init()
    {
        animationDelay = 0;

        // Register for events on the surface
        surfaceHolder = getHolder();
        surfaceHolder.addCallback(this);
        setFocusable(true);
        setFocusableInTouchMode(true);

        // Workaround for edge fading
        // Sometimes after repeated orientation changes, one edge will fade; this fixes it
        setHorizontalFadingEdgeEnabled(false);
        setVerticalFadingEdgeEnabled(false);
    }

    ///This is called immediately after the surface is first created. Implementations of this should
    // start up whatever rendering code they desire
    @Override
    public void surfaceCreated(SurfaceHolder holder)
    {
        setEnable(0x01);
    }

    // This is called immediately after any structural changes (format or size) have been made to
    // the surface. This method is always called at least once, after surfaceCreated(SurfaceHolder)
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height)
    {
        // On Droid (at least) this can get called after a rotation, which shouldn't happen as we
        // should get shut down first. Ignore that, unless we're handling config changes dynamically
        if (isEnable())
        {
            return;
        }

        setSize(format, width, height);
        setEnable(0x02);
    }


    //This is called immediately before a surface is destroyed. After returning from this call, you
    // should no longer try to access this surface.  If you have a rendering thread that directly
    // accesses the surface, you must ensure that thread is no longer touching the Surface before
    // returning from this function
    @Override
    public void surfaceDestroyed(SurfaceHolder holder)
    {
        clearEnable(0x01);
    }

    //The application is starting. Applications must call this from their Activity.onStart() method
    public void onStart()
    {
        // Tell the subclass to start
        try
        {
            appStart();
        } catch (Exception e)
        {
            //
        }
    }

    //We're resuming the app. Applications must call this from their Activity.onResume() method
    public void onResume()
    {
        setEnable(0x04);
    }

    // Start the surface running. Applications must call this to set the surface going, and may use
    // this to implement their own level of start/stop control e.g. to implement a "pause" button
    public void surfaceStart()
    {
        setEnable(0x08);
    }

    // Stop the surface running. Applications may call this to stop the surface running. They may
    // use this to implement their own level of start/stop control, for example to implement a
    // "pause" button
    public void surfaceStop()
    {
        clearEnable(0x08);
    }

    //Pause the app. Applications must call this from their Activity.onPause() method
    public void onPause()
    {
        clearEnable(0x04);
    }

    // The application is closing down. Applications must call this from their Activity.onStop()
    // method
    public void onStop()
    {
        // Make sure we're paused
        onPause();

        // Tell the subclass
        try
        {
            appStop();
        } catch (Exception e)
        {
            //e
        }
    }

    // Handle changes in focus. When we lose focus, pause the game so a popup (like the menu)
    // doesn't cause havoc
    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus)
    {
        if (!hasWindowFocus)
            clearEnable(0x10);
        else
            setEnable(0x10);
    }

    // Query the given enable flag
    private boolean isEnable()
    {
        boolean val;
        synchronized (surfaceHolder)
        {
            val = (enableFlags & 0x02) == 0x02;
        }
        return val;
    }

    // Set the given enable flag, and see if we're good to go
    private void setEnable(int flag)
    {
        boolean enabled1;
        boolean enabled2;
        synchronized (surfaceHolder)
        {
            enabled1 = (enableFlags & ENABLE_ALL) == ENABLE_ALL;
            enableFlags |= flag;
            enabled2 = (enableFlags & ENABLE_ALL) == ENABLE_ALL;
        }

        // Are we all set?
        if (!enabled1 && enabled2)
            startRun();
    }

    // Clear the given enable flag, and see if we need to shut down
    private void clearEnable(int flag)
    {
        boolean enabled1;
        boolean enabled2;
        synchronized (surfaceHolder)
        {
            enabled1 = (enableFlags & ENABLE_ALL) == ENABLE_ALL;
            enableFlags &= ~flag;
            enabled2 = (enableFlags & ENABLE_ALL) == ENABLE_ALL;
        }

        // Do we need to stop?
        if (enabled1 && !enabled2)
            stopRun();
    }

    // Start the animation running. All the conditions we need to run are present (surface, size,
    // resumed)
    private void startRun()
    {
        synchronized (surfaceHolder)
        {
            // Tell the subclass we're running
            try
            {
                animStart();
            } catch (Exception e)
            {
                //
            }

            if (animTicker != null && animTicker.isAlive())
                animTicker.kill();
            animTicker = new ThreadTicker();
        }
    }

    // Stop the animation running. Our surface may have been destroyed, so stop all accesses to it.
    // If the caller is not the ticker thread, this method will only return when the ticker thread
    // has died
    private void stopRun()
    {
        // Kill the thread if it's running, and wait for it to die. This is important when the
        // surface is destroyed, as we can't touch the surface after we return. But if I am the
        // ticker thread, don't wait for myself to die
        Timer.Ticker ticker;
        synchronized (surfaceHolder)
        {
            ticker = animTicker;
        }
        if (ticker != null && ticker.isAlive())
        {
            if (onSurfaceThread())
                ticker.kill();
            else
                ticker.killAndWait();
        }
        synchronized (surfaceHolder)
        {
            animTicker = null;
        }

        // Tell the subclass we've stopped
        try
        {
            animStop();
        } catch (Exception e)
        {
            //
        }
    }

    // Set the size of the table
    private void setSize(int format, int width, int height)
    {
        synchronized (surfaceHolder)
        {
            canvasWidth = width;
            canvasHeight = height;

            // Create the pixmap for the background image
            if (format == PixelFormat.RGBA_8888)
                canvasConfig = Bitmap.Config.ARGB_8888;
            else
                canvasConfig = Bitmap.Config.RGB_565;

            try
            {
                appSize(canvasWidth, canvasHeight, canvasConfig);
            }
            catch (Exception e)
            {
                //
            }
        }
    }

    private void tick()
    {
        try
        {
            // Do the application's physics
            long now = System.currentTimeMillis();
            doUpdate(now);

            // And update the screen
            refreshScreen(now);
        }
        catch (Exception e)
        {
            //
        }
    }

    // Draw the game board to the screen in its current state, as a one-off. This can be used to
    // refresh the screen
    private void refreshScreen(long now)
    {
        Canvas canvas = null;
        try
        {
            canvas = surfaceHolder.lockCanvas(null);
            synchronized (surfaceHolder)
            {
                doDraw(canvas, now);
            }
        } finally
        {
            // Do this in a finally so that if an exception is thrown during the above, we don't
            // leave the Surface in an inconsistent state
            if (canvas != null)
                surfaceHolder.unlockCanvasAndPost(canvas);
        }
    }

    // The application is starting. Perform any initial set-up prior to starting the application.
    // We may not have a screen size yet, so this is not a good place to allocate resources which
    // depend on that
    protected abstract void appStart();

    // Set the screen size. This is guaranteed to be called before animStart(), but perhaps not
    // before appStart()
    protected abstract void appSize(int width, int height, Bitmap.Config config);

    // We are starting the animation loop. The screen size is known. doUpdate() and doDraw() may be
    // called from this point on
    protected abstract void animStart();

    // We are stopping the animation loop, for example to pause the app. doUpdate() and doDraw()
    // will not be called from this point on
    protected abstract void animStop();

    //The application is closing down. Clean up any resources
    protected abstract void appStop();

    /**
     * Update the state of the application for the current frame.
     *
     * <p>Applications must override this, and can use it to update for example the physics of a
     * game. This may be a no-op in some cases
     *
     * <p>doDraw() will always be called after this method is called; however, the converse is not
     * true, as we sometimes need to draw just to update the screen.  Hence this method is useful
     * for updates which are dependent on time rather than frames
     *
     * @param now Current time in ms.
     */
    protected abstract void doUpdate(long now);

    /**
     * Draw the current frame of the application
     *
     * <p>Applications must override this, and are expected to draw the entire screen into the
     * provided canvas
     *
     * <p>This method will always be called after a call to doUpdate(), and also when the screen
     * needs to be re-drawn
     *
     * @param canvas The Canvas to draw into
     * @param now    Current time in ms.  Will be the same as that passed to doUpdate(), if there
     *               was a preceeding call to doUpdate()
     */
    protected abstract void doDraw(Canvas canvas, long now);

    /**
     * Get a Bitmap which is the same size and format as the surface. This can be used to get an
     * off-screen rendering buffer, for example
     *
     * @return A Bitmap which is the same size and pixel format as the screen
     */
    public Bitmap getBitmap()
    {
        return Bitmap.createBitmap(canvasWidth, canvasHeight, canvasConfig);
    }

    // Determine whether the caller is on the surface's animation thread
    public boolean onSurfaceThread()
    {
        return Thread.currentThread() == animTicker;
    }


    //Thread-based ticker class
    private class ThreadTicker
            extends Thread
            implements Timer.Ticker
    {

        // Constructor - start at once
        private ThreadTicker()
        {
            super("Surface Runner");
            enable = true;
            start();
        }

        // Stop this thread. There will be no new calls to tick() after this
        @Override
        public void kill()
        {
            enable = false;
        }

        // Stop this thread and wait for it to die. When we return, it is guaranteed that tick()
        // will never be called again
        //
        // Caution: if this is called from within tick(), deadlock is guaranteed
        @Override
        public void killAndWait()
        {
            if (Thread.currentThread() == this)
                throw new IllegalStateException("ThreadTicker.killAndWait() called from ticker thread");

            enable = false;

            // Wait for the thread to finish. Ignore interrupts
            if (isAlive())
            {
                boolean retry = true;
                while (retry)
                {
                    try
                    {
                        join();
                        retry = false;
                    }
                    catch (InterruptedException e)
                    {
                        //
                    }
                }
            }
            else
            {
                //
            }
        }

        // Run method for this thread - simply call tick() a lot until enable is false
        @Override
        public void run()
        {
            while (enable)
            {
                tick();

                if (animationDelay != 0)
                try
                {
                    sleep(animationDelay);
                }
                catch (InterruptedException e)
                {
                    //
                }
            }
        }

        // Flag used to terminate this thread - when false, we die
        private boolean enable;
    }

    // Enable flags. In order to run, we need onSurfaceCreated() and onResume(), which can come in
    // either order. So we track which ones we have by these flags. When all are set, we're good to
    // go. Note that this is distinct from the game state machine, and its pause and resume
    // actions - the whole game is enabled by the combination of these flags set in enableFlags
    //private static final int ENABLE_SURFACE = 0x01;
    //private static final int ENABLE_SIZE = 0x02;
    //private static final int ENABLE_RESUMED = 0x04;
    //private static final int ENABLE_STARTED = 0x08;
    //private static final int ENABLE_FOCUSED = 0x10;
    private static final int ENABLE_ALL = 0x01 | 0x02 | 0x04 | 0x08 | 0x10;

    // The surface manager for the view.
    private SurfaceHolder surfaceHolder = null;

    // The time in ms to sleep each time round the main animation loop. If zero, we will not sleep,
    // but will run continuously
    public static long animationDelay = 0;

    // Enablement flags; see comment above
    private int enableFlags = 0;

    // Width, height and pixel format of the surface
    private int canvasWidth = 0;
    private int canvasHeight = 0;
    private Bitmap.Config canvasConfig = null;

    // The ticker thread which runs the animation.  null if not active
    private Timer.Ticker animTicker = null;
}