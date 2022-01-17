/*
 * org.hermit.android.core: useful Android foundation classes.
 *
 * These classes are designed to help build various types of application.
 *
 * <br>Copyright 2009-2010 Ian Cameron Smith
 *
 * <p>This program is free software; you can redistribute it and/or modify it under the terms of the
 * GNU General Public License version 2 as published by the Free Software Foundation (see COPYING)
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details
 */

/**
 * Common base for applications with an animated view. This class can be used in games etc. It
 * handles all the setup states of a SurfaceView, and provides a Thread which the app can use to
 * manage the animation
 *
 * <p>When using this class in an app, the app context <b>must</b> call these methods (usually from
 * its corresponding Activity methods):
 *
 * <ul>
 * <li>{@link #onStart()}
 * <li>{@link #onResume()}
 * <li>{@link #onPause()}
 * <li>{@link #onStop()}
 * </ul>
 *
 * <p>The surface is enabled once it is created and sized, and {@link #onStart()} and
 * {@link #onResume()} have been called. You then start and stop it by calling
 * {@link #surfaceStart()} and {@link #surfaceStop()}
 */

package org.hermit.android.core;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public abstract class SurfaceRunner
        extends SurfaceView
        implements SurfaceHolder.Callback
{
    /**
     * Surface runner option: handle configuration changes dynamically. If set, configuration
     * changes such as screen orientation changes will be passed up to the app; otherwise, it is
     * assumed that we will re-start for these
     */
    public static final int SURFACE_DYNAMIC = 0x0001;

    /**
     * Surface runner option: use a Looper to drive animations. This allows asynchronous updates to
     * be posted by the app
     */
    public static final int LOOPED_TICKER = 0x0002;

    /**
     * Create a SurfaceRunner instance
     *
     * @param app The application context we're running in
     */
    public SurfaceRunner(Context app)
    {
        super(app);
        init();
    }

    /**
     * Create a SurfaceRunner instance
     *
     * @param app   The application context we're running in
     * @param attrs Layout attributes for this SurfaceRunner
     */
    public SurfaceRunner(Context app, AttributeSet attrs)
    {
        super(app, attrs);
        init();
    }


    //Initialize this SurfaceRunner instance
    private void init()
    {
        surfaceOptions = 0;
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

    /**
     * Check whether the given option flag is set on this surface
     *
     * @param option The option flag to test; one of SURFACE_XXX
     * @return true if the option is set
     */
    public boolean optionSet(int option)
    {
        return (surfaceOptions & option) != 0;
    }

    /**
     * Set the delay in ms in each iteration of the main loop.
     *
     * @param delay The time in ms to sleep each time round the main animation loop.  If zero, we
     *              will not sleep, but will run continuously
     */
    public void setDelay(long delay)
    {
        Log.i(TAG, "setDelay " + delay);
        animationDelay = delay;
    }

    /**
     * This is called immediately after the surface is first created. Implementations of this should
     * start up whatever rendering code they desire.
     * <p>
     * Note that only one thread can ever draw into a Surface, so you should not draw into the
     * Surface here if your normal rendering will be in another thread.
     *
     * @param    holder        The SurfaceHolder whose surface is being created.
     */
    @Override
    public void surfaceCreated(SurfaceHolder holder)
    {
        setEnable(ENABLE_SURFACE, "surfaceCreated");
    }

    /**
     * This is called immediately after any structural changes (format or size) have been made to
     * the surface.  This method is always called at least once, after surfaceCreated(SurfaceHolder)
     *
     * @param    holder        The SurfaceHolder whose surface has changed.
     * @param    format        The new PixelFormat of the surface.
     * @param    width        The new width of the surface.
     * @param    height        The new height of the surface.
     */
    @Override
    public void surfaceChanged(SurfaceHolder holder,
                               int format, int width, int height)
    {
        // On Droid (at least) this can get called after a rotation, which shouldn't happen as we
        // should get shut down first. Ignore that, unless we're handling config changes dynamically
        if (!optionSet(SURFACE_DYNAMIC) && isEnable(ENABLE_SIZE))
        {
            Log.e(TAG, "ignored surfaceChanged " + width + "x" + height);
            return;
        }

        setSize(format, width, height);
        setEnable(ENABLE_SIZE, "set size " + width + "x" + height);
    }


    /**
     * This is called immediately before a surface is destroyed. After returning from this call, you
     * should no longer try to access this surface.  If you have a rendering thread that directly
     * accesses the surface, you must ensure that thread is no longer touching the Surface before
     * returning from this function
     *
     * @param    holder        The SurfaceHolder whose surface is being destroyed
     */
    @Override
    public void surfaceDestroyed(SurfaceHolder holder)
    {
        clearEnable(ENABLE_SURFACE, "surfaceDestroyed");
    }


    //The application is starting. Applications must call this from their Activity.onStart() method
    public void onStart()
    {
        Log.i(TAG, "onStart");

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
        setEnable(ENABLE_RESUMED, "onResume");
    }


    /**
     * Start the surface running. Applications must call this to set the surface going. They may use
     * this to implement their own level of start/stop control, for example to implement a "pause"
     * button
     */
    public void surfaceStart()
    {
        setEnable(ENABLE_STARTED, "surfaceStart");
    }


    /**
     * Stop the surface running. Applications may call this to stop the surface running. They may
     * use this to implement their own level of start/stop control, for example to implement a
     * "pause" button.
     */
    public void surfaceStop()
    {
        clearEnable(ENABLE_STARTED, "surfaceStop");
    }


    //Pause the app.  Applications must call this from their Activity.onPause() method
    public void onPause()
    {
        clearEnable(ENABLE_RESUMED, "onPause");
    }


    /**
     * The application is closing down. Applications must call this from their Activity.onStop()
     * method
     */
    public void onStop()
    {
        Log.i(TAG, "onStop()");

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


    /**
     * Handle changes in focus.  When we lose focus, pause the game so a popup (like the menu)
     * doesn't cause havoc
     *
     * @param    hasWindowFocus        True if we have focus
     */
    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus)
    {
        if (!hasWindowFocus)
            clearEnable(ENABLE_FOCUSED, "onWindowFocusChanged");
        else
            setEnable(ENABLE_FOCUSED, "onWindowFocusChanged");
    }


    /**
     * Query the given enable flag
     *
     * @param flag The flag to check
     * @return The flag value
     */
    private boolean isEnable(int flag)
    {
        boolean val;
        synchronized (surfaceHolder)
        {
            val = (enableFlags & flag) == flag;
        }
        return val;
    }


    /**
     * Set the given enable flag, and see if we're good to go
     *
     * @param flag The flag to set
     * @param why  Short tag explaining why, for debugging
     */
    private void setEnable(int flag, String why)
    {
        boolean enabled1;
        boolean enabled2;
        synchronized (surfaceHolder)
        {
            enabled1 = (enableFlags & ENABLE_ALL) == ENABLE_ALL;
            enableFlags |= flag;
            enabled2 = (enableFlags & ENABLE_ALL) == ENABLE_ALL;

            Log.i(TAG, "EN + " + why + " -> " + enableString());
        }

        // Are we all set?
        if (!enabled1 && enabled2)
            startRun();
    }


    /**
     * Clear the given enable flag, and see if we need to shut down
     *
     * @param flag The flag to clear
     * @param why  Short tag explaining why, for debugging
     */
    private void clearEnable(int flag, String why)
    {
        boolean enabled1;
        boolean enabled2;
        synchronized (surfaceHolder)
        {
            enabled1 = (enableFlags & ENABLE_ALL) == ENABLE_ALL;
            enableFlags &= ~flag;
            enabled2 = (enableFlags & ENABLE_ALL) == ENABLE_ALL;

            Log.i(TAG, "EN - " + why + " -> " + enableString());
        }

        // Do we need to stop?
        if (enabled1 && !enabled2)
            stopRun();
    }


    /**
     * Get the current enable state as a string for debugging
     *
     * @return The current enable state as a string
     */
    private String enableString()
    {
        char[] buf = new char[5];
        buf[0] = (enableFlags & ENABLE_SURFACE) != 0 ? 'S' : '-';
        buf[1] = (enableFlags & ENABLE_SIZE) != 0 ? 'Z' : '-';
        buf[2] = (enableFlags & ENABLE_RESUMED) != 0 ? 'R' : '-';
        buf[3] = (enableFlags & ENABLE_STARTED) != 0 ? 'A' : '-';
        buf[4] = (enableFlags & ENABLE_FOCUSED) != 0 ? 'F' : '-';

        return String.valueOf(buf);
    }


    /**
     * Start the animation running. All the conditions we need to run are present (surface, size,
     * resumed)
     */
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
            Log.i(TAG, "set running: start ticker");
            animTicker = !optionSet(LOOPED_TICKER) ?
                    new ThreadTicker() : new LoopTicker();
        }
    }


    /**
     * Stop the animation running. Our surface may have been destroyed, so stop all accesses to it.
     * If the caller is not the ticker thread, this method will only return when the ticker thread
     * has died
     */
    private void stopRun()
    {
        /**
         * Kill the thread if it's running, and wait for it to die. This is important when the
         * surface is destroyed, as we can't touch the surface after we return. But if I am the
         * ticker thread, don't wait for myself to die
         */
        Ticker ticker;
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


    /**
     * Set the size of the table
     *
     * @param format The new PixelFormat of the surface
     * @param width  The new width of the surface
     * @param height The new height of the surface
     */
    private void setSize(int format, int width, int height)
    {
        synchronized (surfaceHolder)
        {
            canvasWidth = width;
            canvasHeight = height;

            // Create the pixmap for the background image
            switch (format)
            {
                case PixelFormat.RGBA_8888:
                    canvasConfig = Bitmap.Config.ARGB_8888;
                    break;
                default:
                    canvasConfig = Bitmap.Config.RGB_565;
                    break;
            }

            try
            {
                appSize(canvasWidth, canvasHeight, canvasConfig);
            } catch (Exception e)
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

            // And update the screen.
            refreshScreen(now);
        } catch (Exception e)
        {
            //
        }
    }


    /**
     * Draw the game board to the screen in its current state, as a one-off. This can be used to
     * refresh the screen
     */
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

    /**
     * The application is starting. Perform any initial set-up prior to starting the application.
     * We may not have a screen size yet, so this is not a good place to allocate resources which
     * depend on that
     */
    protected abstract void appStart();


    /**
     * Set the screen size. This is guaranteed to be called before animStart(), but perhaps not
     * before appStart()
     *
     * @param width  The new width of the surface
     * @param height The new height of the surface
     * @param config The pixel format of the surface
     */
    protected abstract void appSize(int width, int height, Bitmap.Config config);


    /**
     * We are starting the animation loop. The screen size is known.
     *
     * <p>doUpdate() and doDraw() may be called from this point on.
     */
    protected abstract void animStart();


    /**
     * We are stopping the animation loop, for example to pause the app.
     *
     * <p>doUpdate() and doDraw() will not be called from this point on.
     */
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

    /**
     * Determine whether the caller is on the surface's animation thread
     *
     * @return The resource value
     */
    public boolean onSurfaceThread()
    {
        return Thread.currentThread() == animTicker;
    }

    /**
     * Base interface for the ticker we use to control the animation
     */
    private interface Ticker
    {
        // Stop this thread. There will be no new calls to tick() after this
        void kill();

        /**
         * Stop this thread and wait for it to die.  When we return, it is guaranteed that tick()
         * will never be called again
         *
         * Caution: if this is called from within tick(), deadlock is guaranteed
         */
        void killAndWait();

        // Determine whether this ticker is still going
        boolean isAlive();
    }


    //Thread-based ticker class. This may be faster than LoopTicker
    private class ThreadTicker
            extends Thread
            implements Ticker
    {

        // Constructor - start at once
        private ThreadTicker()
        {
            super("Surface Runner");
            Log.v(TAG, "ThreadTicker: start");
            enable = true;
            start();
        }

        // Stop this thread. There will be no new calls to tick() after this
        @Override
        public void kill()
        {
            Log.v(TAG, "ThreadTicker: kill");

            enable = false;
        }

        // Stop this thread and wait for it to die.  When we return, it is
        // guaranteed that tick() will never be called again.
        //
        // Caution: if this is called from within tick(), deadlock is
        // guaranteed.
        @Override
        public void killAndWait()
        {
            Log.v(TAG, "ThreadTicker: killAndWait");

            if (Thread.currentThread() == this)
                throw new IllegalStateException("ThreadTicker.killAndWait()" +
                        " called from ticker thread");

            enable = false;

            // Wait for the thread to finish.  Ignore interrupts.
            if (isAlive())
            {
                boolean retry = true;
                while (retry)
                {
                    try
                    {
                        join();
                        retry = false;
                    } catch (InterruptedException e)
                    {
                    }
                }
                Log.v(TAG, "ThreadTicker: killed");
            } else
            {
                Log.v(TAG, "Ticker: was dead");
            }
        }

        // Run method for this thread -- simply call tick() a lot until
        // enable is false.
        @Override
        public void run()
        {
            while (enable)
            {
                tick();

                if (animationDelay != 0) try
                {
                    sleep(animationDelay);
                } catch (InterruptedException e)
                {
                }
            }
        }

        // Flag used to terminate this thread -- when false, we die.
        private boolean enable;
    }


    /**
     * Looper-based ticker class. This has the advantage that asynchronous updates can be scheduled
     * by passing it a message
     */
    private class LoopTicker
            extends Thread
            implements Ticker
    {
        // Constructor - start at once
        private LoopTicker()
        {
            super("Surface Runner");
            Log.v(TAG, "Ticker: start");
            start();
        }

        // Stop this thread. There will be no new calls to tick() after this
        @Override
        public void kill()
        {
            Log.v(TAG, "LoopTicker: kill");

            synchronized (this)
            {
                if (msgHandler == null)
                    return;

                // Remove any delayed ticks.
                msgHandler.removeMessages(MSG_TICK);

                // Do an abort right now.
                msgHandler.sendEmptyMessage(MSG_ABORT);
            }
        }

        // Stop this thread and wait for it to die.  When we return, it is
        // guaranteed that tick() will never be called again.
        //
        // Caution: if this is called from within tick(), deadlock is
        // guaranteed.
        @Override
        public void killAndWait()
        {
            Log.v(TAG, "LoopTicker: killAndWait");

            if (Thread.currentThread() == this)
                throw new IllegalStateException("LoopTicker.killAndWait()" +
                        " called from ticker thread");

            synchronized (this)
            {
                if (msgHandler == null)
                    return;

                // Remove any delayed ticks.
                msgHandler.removeMessages(MSG_TICK);

                // Do an abort right now.
                msgHandler.sendEmptyMessage(MSG_ABORT);
            }

            // Wait for the thread to finish.  Ignore interrupts.
            if (isAlive())
            {
                boolean retry = true;
                while (retry)
                {
                    try
                    {
                        join();
                        retry = false;
                    } catch (InterruptedException e)
                    {
                    }
                }
                Log.v(TAG, "LoopTicker: killed");
            } else
            {
                Log.v(TAG, "LoopTicker: was dead");
            }
        }

        @Override
        public void run()
        {
            Looper.prepare();

            msgHandler = new Handler(Looper.getMainLooper())
            {
                @Override
                public void handleMessage(Message msg)
                {
                    switch (msg.what)
                    {
                        case MSG_TICK:
                            tick();
                            if (!msgHandler.hasMessages(MSG_TICK))
                                msgHandler.sendEmptyMessageDelayed(MSG_TICK,
                                        animationDelay);
                            break;
                        case MSG_ABORT:
                            Looper.myLooper().quit();
                            break;
                    }
                }
            };

            // Schedule the first tick.
            msgHandler.sendEmptyMessageDelayed(MSG_TICK, animationDelay);

            // Go into the processing loop.
            Looper.loop();
        }

        // Message codes.
        private static final int MSG_TICK = 6;
        private static final int MSG_ABORT = 9;

        // Our message handler.
        private Handler msgHandler = null;
    }


    // ******************************************************************** //
    // Class Data.
    // ******************************************************************** //

    // Debugging tag.
    private static final String TAG = "SurfaceRunner";

    // Enable flags.  In order to run, we need onSurfaceCreated() and
    // onResume(), which can come in either order.  So we track which ones
    // we have by these flags.  When all are set, we're good to go.  Note
    // that this is distinct from the game state machine, and its pause
    // and resume actions -- the whole game is enabled by the combination
    // of these flags set in enableFlags.
    private static final int ENABLE_SURFACE = 0x01;
    private static final int ENABLE_SIZE = 0x02;
    private static final int ENABLE_RESUMED = 0x04;
    private static final int ENABLE_STARTED = 0x08;
    private static final int ENABLE_FOCUSED = 0x10;
    private static final int ENABLE_ALL =
            ENABLE_SURFACE | ENABLE_SIZE | ENABLE_RESUMED |
                    ENABLE_STARTED | ENABLE_FOCUSED;


    // ******************************************************************** //
    // Private Data
    // ******************************************************************** //

    // The surface manager for the view.
    private SurfaceHolder surfaceHolder = null;

    // The time in ms to sleep each time round the main animation loop.
    // If zero, we will not sleep, but will run continuously.
    private long animationDelay = 0;

    // Option flags for this instance.  A bitwise OR of SURFACE_XXX constants.
    private int surfaceOptions = 0;

    // Enablement flags; see comment above.
    private int enableFlags = 0;

    // Width, height and pixel format of the surface.
    private int canvasWidth = 0;
    private int canvasHeight = 0;
    private Bitmap.Config canvasConfig = null;

    // The ticker thread which runs the animation.  null if not active.
    private Ticker animTicker = null;

}