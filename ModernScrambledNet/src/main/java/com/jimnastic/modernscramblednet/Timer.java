package com.jimnastic.modernscramblednet;

import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;


/**
 * This class implements a simple periodic timer
 */
public abstract class Timer
        extends Handler
{

    // ******************************************************************** //
    // Constructor.
    // ******************************************************************** //

    /**
     * Construct a periodic timer with a given tick interval.
     *
     * @param    ival            Tick interval in ms.
     */
    public Timer(long ival)
    {
        tickInterval = ival;
        isRunning = false;
        accumTime = 0;
    }


    // ******************************************************************** //
    // Timer Control.
    // ******************************************************************** //

    /**
     * Start the timer.  step() will be called at regular intervals
     * until it returns true; then done() will be called.
     * <p>
     * Subclasses may override this to do their own setup; but they
     * must then call super.start().
     */
    public void start()
    {
        if (isRunning)
            return;

        isRunning = true;

        long now = SystemClock.uptimeMillis();

        // Start accumulating time again.
        lastLogTime = now;

        // Schedule the first event at once.
        nextTime = now;
        postAtTime(runner, nextTime);
    }


    /**
     * Stop the timer.  step() will not be called again until it is
     * restarted.
     * <p>
     * Subclasses may override this to do their own setup; but they
     * must then call super.stop().
     */
    public void stop()
    {
        if (isRunning)
        {
            isRunning = false;
            long now = SystemClock.uptimeMillis();
            accumTime += now - lastLogTime;
            lastLogTime = now;
        }
    }


    /**
     * Stop the timer, and reset the accumulated time and tick count.
     */
    public final void reset()
    {
        stop();
        tickCount = 0;
        accumTime = 0;
    }

    /**
     * Get the accumulated time of this Timer.
     *
     * @return How long this timer has been running, in ms.
     */
    public final long getTime()
    {
        return accumTime;
    }


    // ******************************************************************** //
    // Handlers.
    // ******************************************************************** //

    /**
     * Subclasses override this to handle a timer tick.
     *
     * @param    count        The call count; 0 on the first call.
     * @param    time        The total time for which this timer has been
     * running, in ms.  Reset by reset().
     * @return true if the timer should stop; this will
     * trigger a call to done().  false otherwise;
     * we will continue calling step().
     */
    protected abstract boolean step(int count, long time);


    /**
     * Subclasses may override this to handle completion of a run.
     */
    protected void done()
    {
    }


    // ******************************************************************** //
    // Implementation.
    // ******************************************************************** //

    /**
     * Handle a step of the animation.
     */
    private final Runnable runner = new Runnable()
    {

        public final void run()
        {
            if (isRunning)
            {
                long now = SystemClock.uptimeMillis();

                // Add up the time since the last step.
                accumTime += now - lastLogTime;
                lastLogTime = now;

                if (!step(tickCount++, accumTime))
                {
                    // Schedule the next.  If we've got behind, schedule
                    // it for a tick after now.  (Otherwise we'd end
                    // up with a zillion events queued.)
                    nextTime += tickInterval;
                    if (nextTime <= now)
                        nextTime += tickInterval;
                    postAtTime(runner, nextTime);
                } else
                {
                    isRunning = false;
                    done();
                }
            }
        }

    };


    // ******************************************************************** //
    // State Save/Restore.
    // ******************************************************************** //

    /**
     * Save game state so that the user does not lose anything
     * if the game process is killed while we are in the
     * background.
     *
     * @param    outState        A Bundle in which to place any state
     * information we wish to save.
     */
    void saveState(Bundle outState)
    {
        // Accumulate all time up to now, so we know where we're saving.
        if (isRunning)
        {
            long now = SystemClock.uptimeMillis();
            accumTime += now - lastLogTime;
            lastLogTime = now;
        }

        outState.putLong("tickInterval", tickInterval);
        outState.putBoolean("isRunning", isRunning);
        outState.putInt("tickCount", tickCount);
        outState.putLong("accumTime", accumTime);
    }

    /**
     * Restore our game state from the given Bundle.
     *
     * @param    map            A Bundle containing the saved state.
     * @param    run            If true, restore the saved runnning state;
     * otherwise restore to a stopped state.
     * @return true if the state was restored OK; false
     * if the saved state was incompatible with the
     * current configuration.
     */
    boolean restoreState(Bundle map, boolean run)
    {
        tickInterval = map.getLong("tickInterval");
        isRunning = map.getBoolean("isRunning");
        tickCount = map.getInt("tickCount");
        accumTime = map.getLong("accumTime");
        lastLogTime = SystemClock.uptimeMillis();

        // If we were running, restart if requested, else stop.
        if (isRunning)
        {
            if (run)
                start();
            else
                isRunning = false;
        }

        return true;
    }

    private long tickInterval; // The tick interval in ms
    private boolean isRunning; // True if the timer is running
    private int tickCount;     // Number of times step() has been called
    private long lastLogTime;  // The time at which we last added to accumTime.

    // Time at which to execute the next step.  We schedule each step at this plus x ms; this gives
    // us an even execution rate
    private long nextTime;

    // The accumulated time in ms for which this timer has been running. Increments between start()
    // and stop(); start(true) resets it
    private long accumTime;

    /////////////////// Taken from org.hermit.android.core.SurfaceRunner ///////////////////

    // Base interface for the ticker we use to control the animation
    public interface Ticker
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
}

