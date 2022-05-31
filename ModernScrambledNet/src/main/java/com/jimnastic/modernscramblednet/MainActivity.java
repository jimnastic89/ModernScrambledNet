/*
 * NetScramble: unscramble a network and connect all the terminals.
 * The player is given a network diagram with the parts of the network
 * randomly rotated; he/she must rotate them to connect all the terminals
 * to the server.
 *
 * This is an Android implementation of the KDE game "knetwalk" by
 * Andi Peredri, Thomas Nagy, and Reinhold Kainhofer.
 *
 * © 2007-2010 Ian Cameron Smith <johantheghost@yahoo.com>
 *
 * © 2014 Michael Mueller <michael.mueller@silentservices.de>
 *
 *   This program is free software; you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License version 2
 *   as published by the Free Software Foundation (see COPYING).
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 */

package com.jimnastic.modernscramblednet;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.widget.TextView;
import android.widget.ViewAnimator;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.jimnastic.modernscramblednet.BoardView.Skill;

public class MainActivity extends AppCompatActivity
{
    //This is the first thing that happens when app starts
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        Log.i(TAG, "onCreate(): " + (savedInstanceState == null ? "clean start" : "restart"));

        super.onCreate(savedInstanceState);

        appResources = getResources();

        Log.i(TAG, "MainActivity.onCreate() creates new MainActivity.GameTimer()");
        gameTimer = new GameTimer();

        // Create string formatting buffers
        clicksText = new StringBuilder(10);
        timeText = new StringBuilder(10);

        SharedPreferences newPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        // Create the GUI for the game
        Log.i(TAG, "MainActivity.onCreate().setContentView() using R.layout.mainactivity");
        Log.i(TAG, "**************************************************************");
        Log.i(TAG, "We somehow get from MainActivity to BoardView constructor here");
        setContentView(R.layout.mainactivity);
        Log.i(TAG, "**************************************************************");


        Log.i(TAG, "MainActivity.onCreate() runs MainActivity.setupGui()");
        setupGui();

        // Restore our preferences
        SharedPreferences prefs = getPreferences(0);

        /* **** Sound Setup *****/
        this.setVolumeControlStream(AudioManager.STREAM_MUSIC);
        SettingsActivity.SoundString = newPrefs.getString("SoundPreference", "FULL");
        soundMode = SettingsActivity.SoundState();
        Log.i(TAG, "MainActivity.onCreate() runs MainActivity.createSoundPool()");
        soundPool = createSoundPool();
        /* **** End Sound Setup *****/

        /* **** Animation Setup *****/
        SettingsActivity.AnimationState = newPrefs.getBoolean("AnimationPreference",true);
        Log.i(TAG, "MainActivity.onCreate() runs BoardView.setAnimEnable()");
        boardView.setAnimEnable(SettingsActivity.AnimationState);
        /* **** End Animation Setup *****/

        // If we have a previous state to restore, try to do so
        boolean restored = false;
        if (savedInstanceState != null)
            restored = restoreState(savedInstanceState);

        // Get the current game skill level from the preferences, if we didn't get a saved game.
        // Default to NOVICE if it's not there
        if (!restored)
        {
            gameSkill = null;
            String skill = prefs.getString("skillLevel", null);
            if (skill != null)
                gameSkill = Skill.valueOf(skill);
            if (gameSkill == null)
                gameSkill = Skill.NOVICE;
            gameState = GameState.NEW;
        } else
        {
            // Save our restored game state
            restoredGameState = gameState;
            gameState = GameState.RESTORED;
        }

        Log.i(TAG, "End MainActivity.onCreate()");
    }



    // #2 - Called from MainActivity.onCreate()
    // Set up the GUI for the game. Add handlers and animations where needed
    private void setupGui()
    {
        Log.v(MainActivity.TAG, "MainActivity.setupGui.findViewById(" + R.id.view_switcher + ")");
        viewSwitcher = findViewById(R.id.view_switcher);

        Log.v(MainActivity.TAG, "MainActivity.setupGui.findViewById(" + R.id.splash_text + ")");
        splashText = findViewById(R.id.splash_text);

        Log.v(MainActivity.TAG, "MainActivity.setupGui.findViewById(" + R.id.board_view + ")");
        boardView = findViewById(R.id.board_view);

        Log.v(MainActivity.TAG, "MainActivity.setupGui.findViewById(" + R.id.status_clicks + ")");
        statusClicks = findViewById(R.id.status_clicks);

        Log.v(MainActivity.TAG, "MainActivity.setupGui.findViewById(" + R.id.status_mode + ")");
        statusMode = findViewById(R.id.status_mode);

        Log.v(MainActivity.TAG, "MainActivity.setupGui.findViewById(" + R.id.status_time + ")");
        statusTime = findViewById(R.id.status_time);

        // Set up the splash text view to call wakeUp() when the user taps the screen
        splashText.setOnTouchListener((v, event) ->
        {
            if (event.getAction() == MotionEvent.ACTION_DOWN)
            {
                MainActivity.this.wakeUp();
                v.performClick();
            }
            return true;
        });
    }

    // #3 - Called from MainActivity.onCreate()
    //Create a SoundPool containing the app's sound effects
    private SoundPool createSoundPool()
    {
        SoundPool pool = new SoundPool.Builder()
                .setMaxStreams(3)
                .build();
        for (Sound sound : Sound.values())
            sound.soundId = pool.load(this, sound.soundRes, 1);

        return pool;
    }

    // This class implements the game clock. All it does is update the status each tick
    private final class GameTimer extends Timer
    {
        GameTimer()
        {
            // Tick every 0.25 s
            super(250);
        }

        @Override
        protected boolean step(int count, long time)
        {
            updateStatus();

            // Run until explicitly stopped
            return false;
        }

    }

    /**
     * Called when the current activity is being re-displayed to the user (the
     * user has navigated back to it). It will be followed by onStart().
     * <p>
     * For activities that are using raw Cursor objects (instead of creating
     * them through managedQuery(android.net.Uri, String[], String, String[],
     * String), this is usually the place where the cursor should be requeried
     * (because you had deactivated it in onStop().
     * <p>
     * Derived classes must call through to the super class's implementation of
     * this method. If they do not, an exception will be thrown.
     */
    @Override
    protected void onRestart()
    {
        Log.i(TAG, "onRestart()");
        Log.i("AnimationTest","onRestart() called, setting animation to: " + SettingsActivity.AnimationState);
        boardView.setAnimEnable(SettingsActivity.AnimationState);
        soundMode = SettingsActivity.SoundState();
        //setSoundMode(SettingsActivity.SoundState());
        super.onRestart();
    }

    /**
     * Called after onCreate(Bundle) or onStop() when the current activity is
     * now being displayed to the user. It will be followed by onResume() if the
     * activity comes to the foreground, or onStop() if it becomes hidden.
     * <p>
     * Derived classes must call through to the super class's implementation of
     * this method. If they do not, an exception will be thrown.
     */
    @Override
    protected void onStart()
    {
        Log.i(TAG, "onStart()");
        super.onStart();

        boardView.onStart();
    }

    /**
     * This method is called after onStart() when the activity is being
     * re-initialized from a previously saved state, given here in state. Most
     * implementations will simply use onCreate(Bundle) to restore their state,
     * but it is sometimes convenient to do it here after all of the
     * initialization has been done or to allow subclasses to decide whether to
     * use your default implementation. The default implementation of this
     * method performs a restore of any view state that had previously been
     * frozen by onSaveInstanceState(Bundle).
     * <p>
     * This method is called between onStart() and onPostCreate(Bundle).
     *
     * @param inState The data most recently supplied in
     *                onSaveInstanceState(Bundle).
     */
    @Override
    protected void onRestoreInstanceState(Bundle inState)
    {
        Log.i(TAG, "onRestoreInstanceState()");

        super.onRestoreInstanceState(inState);
    }

    /**
     * Called after onRestoreInstanceState(Bundle), onRestart(), or onPause(),
     * for your activity to start interacting with the user. This is a good
     * place to begin animations, open exclusive-access devices (such as the
     * camera), etc.
     * <p>
     * Keep in mind that onResume is not the best indicator that your activity
     * is visible to the user; a system window such as the keyguard may be in
     * front. Use onWindowFocusChanged(boolean) to know for certain that your
     * activity is visible to the user (for example, to resume a game).
     * <p>
     * Derived classes must call through to the super class's implementation of
     * this method. If they do not, an exception will be thrown.
     */
    @Override
    protected void onResume()
    {
        Log.i(TAG, "onResume()");

        super.onResume();

        // Display the skill level
        statusMode.setText(gameSkill.label);
        Log.i("AnimationTest", "onResume() should now set the animation state to " + SettingsActivity.AnimationState);
        boardView.setAnimEnable(SettingsActivity.AnimationState);
        soundMode = SettingsActivity.SoundState();

        // If we restored a state, go to that state. Otherwise start at the welcome screen
        if (gameState == GameState.NEW)
        {
            Log.d(TAG, "onResume() NEW: init");
            setState(GameState.INIT, true);
        } else if (gameState == GameState.RESTORED)
        {
            Log.d(TAG, "onResume() RESTORED: set " + restoredGameState);
            setState(restoredGameState, true);

            // If we restored an aborted state, that means we were starting a game. Kick it off again
            if (restoredGameState == GameState.ABORTED)
            {
                Log.d(TAG, "onResume() RESTORED ABORTED: start");
                startGame(null);
            }
        } else if (gameState == GameState.PAUSED)
        {
            // We just paused without closing down. Resume
            setState(GameState.RUNNING, true);
        } else
        {
            Log.e(TAG, "onResume() !!" + gameState + "!!: init");
        }

        boardView.onResume();
    }

    /**
     * Called to retrieve per-instance state from an activity before being
     * killed so that the state can be restored in onCreate(Bundle) or
     * onRestoreInstanceState(Bundle) (the Bundle populated by this method will
     * be passed to both).
     * <p>
     * This method is called before an activity may be killed so that when it
     * comes back some time in the future it can restore its state.
     * <p>
     * Do not confuse this method with activity lifecycle callbacks such as
     * onPause(), which is always called when an activity is being placed in the
     * background or on its way to destruction, or onStop() which is called
     * before destruction.
     * <p>
     * The default implementation takes care of most of the UI per-instance
     * state for you by calling onSaveInstanceState() on each view in the
     * hierarchy that has an id, and by saving the id of the currently focused
     * view (all of which is restored by the default implementation of
     * onRestoreInstanceState(Bundle)). If you override this method to save
     * additional information not captured by each individual view, you will
     * likely want to call through to the default implementation, otherwise be
     * prepared to save all of the state of each view yourself.
     * <p>
     * If called, this method will occur before onStop(). There are no
     * guarantees about whether it will occur before or after onPause().
     *
     * @param outState A Bundle in which to place any state information you wish to
     *                 save.
     */
    @Override
    public void onSaveInstanceState(Bundle outState)
    {
        Log.i(TAG, "onSaveInstanceState()");

        super.onSaveInstanceState(outState);

        // Save the state
        saveState(outState);
    }

    /**
     * Called as part of the activity lifecycle when an activity is going into
     * the background, but has not (yet) been killed. The counterpart to
     * onResume().
     * <p>
     * When activity B is launched in front of activity A, this callback will be
     * invoked on A. B will not be created until A's onPause() returns, so be
     * sure to not do anything lengthy here.
     * <p>
     * This callback is mostly used for saving any persistent state the activity
     * is editing, to present a "edit in place" model to the user and making
     * sure nothing is lost if there are not enough resources to start the new
     * activity without first killing this one. This is also a good place to do
     * things like stop animations and other things that consume a noticeable
     * mount of CPU in order to make the switch to the next activity as fast as
     * possible, or to close resources that are exclusive access such as the
     * camera.
     * <p>
     * In situations where the system needs more memory it may kill paused
     * processes to reclaim resources. Because of this, you should be sure that
     * all of your state is saved by the time you return from this function. In
     * general onSaveInstanceState(Bundle) is used to save per-instance state in
     * the activity and this method is used to store global persistent data (in
     * content providers, files, etc.).
     * <p>
     * After receiving this call you will usually receive a following call to
     * onStop() (after the next activity has been resumed and displayed),
     * however in some cases there will be a direct call back to onResume()
     * without going through the stopped state.
     * <p>
     * Derived classes must call through to the super class's implementation of
     * this method. If they do not, an exception will be thrown.
     */
    @Override
    protected void onPause()
    {
        Log.i(TAG, "onPause()");
        super.onPause();

        boardView.onPause();

        // Pause the game. Don't show a splash screen because the game is going away
        if (gameState == GameState.RUNNING)
            setState(GameState.PAUSED, false);
    }

    /**
     * Called when you are no longer visible to the user. You will next receive
     * either onStart(), onDestroy(), or nothing, depending on later user
     * activity.
     * <p>
     * Note that this method may never be called, in low memory situations where
     * the system does not have enough memory to keep your activity's process
     * running after its onPause() method is called.
     * <p>
     * Derived classes must call through to the super class's implementation of
     * this method. If they do not, an exception will be thrown.
     */
    @Override
    protected void onStop()
    {
        Log.i(TAG, "onStop()");
        super.onStop();

        boardView.onStop();
    }

    /**
     * Perform any final cleanup before an activity is destroyed. This can
     * happen either because the activity is finishing (someone called finish()
     * on it, or because the system is temporarily destroying this instance of
     * the activity to save space. You can distinguish between these two
     * scenarios with the isFinishing() method.
     * <p>
     * Note: do not count on this method being called as a place for saving
     * data! For example, if an activity is editing data in a content provider,
     * those edits should be committed in either onPause() or
     * onSaveInstanceState(Bundle), not here. This method is usually implemented
     * to free resources like threads that are associated with an activity, so
     * that a destroyed activity does not leave such things around while the
     * rest of its application is still running. There are situations where the
     * system will simply kill the activity's hosting process without calling
     * this method (or any others) in it, so it should not be used to do things
     * that are intended to remain around after the process goes away.
     * <p>
     * Derived classes must call through to the super class's implementation of
     * this method. If they do not, an exception will be thrown.
     */
    @Override
    protected void onDestroy()
    {
        Log.i(TAG, "onDestroy()");

        super.onDestroy();
    }

    // ******************************************************************** //
    // GUI Creation.
    // ******************************************************************** //



    // ******************************************************************** //
    // Menu Management
    // ******************************************************************** //

    /**
     * Initialize the contents of the game's options menu by adding items to the
     * given menu.
     * <p>
     * This is only called once, the first time the options menu is displayed.
     * To update the menu every time it is displayed, see
     * onPrepareOptionsMenu(Menu).
     * <p>
     * When we add items to the menu, we can either supply a Runnable to receive
     * notification of selection, or we can implement the Activity's
     * onOptionsItemSelected(Menu.Item) method to handle them there.
     *
     * @param menu The options menu in which we should place our items. We can
     *             safely hold on this (and any items created from it), making
     *             modifications to it as desired, until the next time
     *             onCreateOptionsMenu() is called.
     * @return true for the menu to be displayed; false to suppress showing it.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        mainMenu = menu;

        // We must call through to the base implementation
        super.onCreateOptionsMenu(menu);

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);

        // GUI is created, state is restored (if any), and re-sync the options menus
        selectCurrentSkill();
        //selectSoundMode();

        return true;
    }

    private void selectCurrentSkill()
    {
        // Set the selected skill menu item to the current skill
        if (mainMenu != null)
        {
            MenuItem skillItem = mainMenu.findItem(gameSkill.id);
            if (skillItem != null)
                skillItem.setChecked(true);
        }
    }

    void selectAutosolveMode(boolean solving)
    {
        // Set the autosolve menu item to the current state
        if (mainMenu != null)
        {
            MenuItem solveItem = mainMenu.findItem(R.id.menu_autosolve);
            if (solveItem != null)
            {
                if (solving)
                {
                    solveItem.setTitle(R.string.menu_stopsolve);
                } else
                {
                    solveItem.setTitle(R.string.menu_autosolve);
                }
            }
        }
    }

    /**
     * This hook is called whenever an item in your options menu is selected. Derived classes should
     * call through to the base class for it to perform the default menu handling. (True?)
     *
     * @param item The menu item that was selected
     * @return false to have the normal processing happen
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int menuID = item.getItemId();

        if (menuID == R.id.menu_new)
            startGame(null);
        else if (menuID == R.id.menu_pause)
            setState(GameState.PAUSED, true);
        else if (menuID == R.id.menu_scores)
        {
            setState(GameState.PAUSED, false);
            Intent sIntent = new Intent();
            sIntent.setClass(this, ScoreList.class);
            startActivity(sIntent);
        }
        else if (menuID == R.id.menu_help)
        {
            setState(GameState.PAUSED, false);
            Intent hIntent = new Intent();
            hIntent.setClass(this, HelpActivity.class);
            startActivity(hIntent);
        }
        else if (menuID == R.id.menu_about)
            showAbout();
        else if (menuID == R.id.skill_novice)
            startGame(Skill.NOVICE);
        else if (menuID == R.id.skill_normal)
            startGame(Skill.NORMAL);
        else if (menuID == R.id.skill_expert)
            startGame(Skill.EXPERT);
        else if (menuID == R.id.skill_master)
            startGame(Skill.MASTER);
        else if (menuID == R.id.skill_insane)
            startGame(Skill.INSANE);
        else if (menuID == R.id.menu_autosolve)
        {
            solverUsed = true;
            boardView.autosolve();
        }
        else if (menuID == R.id.menu_settings)
        {
            Intent settingsIntent = new Intent();
            settingsIntent.setClass(this, SettingsActivity.class);
            startActivity(settingsIntent);
        }
        return true;
    }

    // ******************************************************************** //
    // Game progress
    // ******************************************************************** //

    //This method is called each time the user clicks a cell
    void cellClicked(Cell cell)
    {
        // Count the click, but only if this isn't a repeat click on the same cell
        // This is because the tap interface only rotatesclockwise, and it's not fair to count an
        // anti-clockwise turn as 3 clicks
        if (!isSolved && cell != prevClickedCell)
        {
            ++clickCount;
            updateStatus();
            prevClickedCell = cell;
        }
    }

    // ******************************************************************** //
    // Game Control Functions
    // ******************************************************************** //

    //Wake up: the user has clicked the splash screen, so continue
    private void wakeUp()
    {
        // If we are paused, just go to running
        // Otherwise (in the welcome or game over screen), start a new game
        if (gameState == GameState.PAUSED)
            setState(GameState.RUNNING, true);
        else
            startGame(null);
    }

    // Create a listener for the user starting the game
    private final DialogInterface.OnClickListener startGameListener = (arg0, arg1) -> setState(GameState.RUNNING, true);

    /**
     * Start a game at a given skill level, or the previous skill level. The skill level chosen is
     * saved to the preferences and becomes the default for next time
     *
     * @param sk Skill level to start at; if null, use the previous skill from the preferences
     */
    public void startGame(BoardView.Skill sk)
    {
        // Abort any existing game, so we know we're not just continuing
        setState(GameState.ABORTED, false);

        // Sort out the previous and new skills. Default to previous if no new skill
        BoardView.Skill prevSkill = gameSkill;
        gameSkill = sk != null ? sk : prevSkill;

        // Save the new skill setting in the prefs
        SharedPreferences prefs = getPreferences(0);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("skillLevel", gameSkill.toString());
        editor.apply();

        // Set the selected skill menu item to the current skill
        selectCurrentSkill();
        statusMode.setText(gameSkill.label);

        // OK, now get going!
        Log.i(TAG, "startGame: " + gameSkill + " (was " + prevSkill + ")");

        // If we're going up to master or insane level, set up a dialog to display the new rules
        int msg = 0;
        if (prevSkill != BoardView.Skill.INSANE)
        {
            if (gameSkill == BoardView.Skill.INSANE)
                msg = R.string.help_insane;
            else if (gameSkill == BoardView.Skill.MASTER && prevSkill != BoardView.Skill.MASTER)
                msg = R.string.help_master;
        }

        // If we have a help message to show, show it; the dialog will start the game (and hence the
        // clock) when the user is ready. Otherwise, start the game now
        if (msg != 0)
            new AlertDialog.Builder(this).setMessage(msg)
                    .setPositiveButton(R.string.button_ok, startGameListener)
                    .show();
        else
            setState(GameState.RUNNING, true);
    }

    // ******************************************************************** //
    // Game State
    // ******************************************************************** //

    //Post a state change
    void postState()
    {
        stateHandler.sendEmptyMessage(GameState.SOLVED.ordinal());
    }

    private Handler stateHandler = new Handler(Looper.getMainLooper())
    {
        @Override
        public void handleMessage(Message m)
        {
            setState(GameState.getValue(m.what), true);
        }
    };

    /**
     * Set the game state. Set the screen display and start/stop the clock as appropriate
     *
     * @param state      The state to go into
     * @param showSplash If true, show the "pause" screen if appropriate. Otherwise don't
     */
    private void setState(GameState state, boolean showSplash)
    {
        Log.i(TAG, "setState: " + state + " (was " + gameState + ")");

        // If we're not changing state, don't bother
        if (state == gameState)
            return;

        // Save the previous state, and change
        GameState prev = gameState;
        gameState = state;

        // Handle the state change
        switch (gameState)
        {
            case NEW:
            case RESTORED:
                // Should never get these
                break;
            case INIT:
                Log.i(TAG, "MainActivity.setState() - gameState = INIT");
                gameTimer.stop();
                if (showSplash)
                    showSplashText(R.string.splash_text);
                break;
            case SOLVED:
                // This is a transient state, just used for signalling a win
                gameTimer.stop();
                boardView.setSolved();

                // We allow the user to keep playing after it's over, but don't keep reporting wins
                // Also don't brag or record a score if the user used the solver
                if (!isSolved && !solverUsed)
                    reportWin(boardView.unconnectedCells());
                isSolved = true;

                // Keep running
                gameState = GameState.RUNNING;
                break;
            case ABORTED:
                // Aborted is followed by something else, so don't display anything
                gameTimer.stop();
                break;
            case PAUSED:
                gameTimer.stop();
                if (showSplash)
                    showSplashText(R.string.pause_text);
                break;
            case RUNNING:
                // Set us going, if this is a new game
                if (prev != GameState.RESTORED && prev != GameState.PAUSED)
                {
                    boardView.setupBoard(gameSkill);
                    isSolved = false;
                    clickCount = 0;
                    prevClickedCell = null;
                    solverUsed = false;
                    gameTimer.reset();
                    updateStatus();
                    makeSound(Sound.START.soundId);
                }
                hideSplashText();
                if (!isSolved)
                    gameTimer.start();
                break;
        }
    }

    // Create a listener for the user starting a new game
    private final DialogInterface.OnClickListener newGameListener = (arg0, arg1) -> startGame(null);

    /**
     * Report that the user has won the game. Let the user continue to play with
     * the layout, or start a new game.
     *
     * @param unused The number of unused cells. Normally zero, but it's sometimes
     *               possible to solve the board without using all the cable bits.
     */
    private void reportWin(int unused)
    {

        // Format the win message
        long time = gameTimer.getTime();
        int titleId = R.string.win_title;
        String msg;

        if (unused != 0)
        {
            String fmt = appResources.getString(R.string.win_spares_text);
            msg = String.format(fmt, time / 60000, time / 1000 % 60, clickCount, unused);
        } else
        {
            String fmt = appResources.getString(R.string.win_text);
            msg = String.format(fmt, time / 60000, time / 1000 % 60, clickCount);
        }

        // See if we have a new high score
        int ntiles = boardView.getBoardWidth() * boardView.getBoardHeight();
        String score = registerScore(gameSkill, ntiles, clickCount, (int) (time / 1000));
        if (score != null)
        {
            msg += "\n\n" + score;
            titleId = R.string.win_pbest_title;
        }

        // Display the dialog
        String finish = appResources.getString(R.string.win_finish);
        msg += "\n\n" + finish;
        new AlertDialog.Builder(this).setTitle(titleId).setMessage(msg)
                .setPositiveButton(R.string.win_new, newGameListener)
                .setNegativeButton(R.string.win_continue, null).show();
    }

    // ******************************************************************** //
    // User Input
    // ******************************************************************** //

    /**
     * Called when the activity has detected the user's press of the back key.
     * The default implementation simply finishes the current activity, but you
     * can override this to do whatever you want.
     * <p>
     * Note: this is only called automatically on Android 2.0 on. On earlier
     * versions, we call this ourselves from BoardView.onKeyDown().
     */
    @Override
    public void onBackPressed()
    {
        // Go to the home screen. This causes our state to be saved, whereas the default of finish()
        // discards it
        Intent homeIntent = new Intent();
        homeIntent.setAction(Intent.ACTION_MAIN);
        homeIntent.addCategory(Intent.CATEGORY_HOME);
        this.startActivity(homeIntent);
    }

    // ******************************************************************** //
    // Status Display
    // ******************************************************************** //

    //Update the status line to the current game state
    void updateStatus()
    {
        // Use StringBuilders and a Formatter to avoid allocating new string objects every time
        // This function is called often!

        clicksText.setLength(3);
        clicksText.setCharAt(0, (char) ('0' + clickCount / 100 % 10));
        clicksText.setCharAt(1, (char) ('0' + clickCount / 10 % 10));
        clicksText.setCharAt(2, (char) ('0' + clickCount % 10));
        statusClicks.setText(clicksText);

        timeText.setLength(5);
        int time = (int) (gameTimer.getTime() / 1000);
        int min = time / 60;
        int sec = time % 60;
        timeText.setCharAt(0, (char) ('0' + min / 10));
        timeText.setCharAt(1, (char) ('0' + min % 10));
        timeText.setCharAt(2, ':');
        timeText.setCharAt(3, (char) ('0' + sec / 10));
        timeText.setCharAt(4, (char) ('0' + sec % 10));
        statusTime.setText(timeText);
    }

    /**
     * Set the status text to the given text message. This hides the game board.
     *
     * @param msgId Resource ID of the message to set.
     */
    private void showSplashText(int msgId)
    {
        splashText.setText(msgId);
        if (viewSwitcher.getDisplayedChild() != 1)
        {
            // Stop the game
            boardView.surfaceStop();
            viewSwitcher.setDisplayedChild(1);
        }

        // Any key dismisses it, so we need focus
        splashText.requestFocus();
    }

    //Hide the status text, revealing the board
    void hideSplashText()
    {
        if (viewSwitcher.getDisplayedChild() != 0)
        {
            viewSwitcher.setDisplayedChild(0);

            // Start the game after the animation
            soundHandler.postDelayed(startRunner, 0);
        } else
        {
            // Make sure we're running - we can get here after a restart
            boardView.surfaceStart();
        }
    }

    private Runnable startRunner = new Runnable()
    {
        @Override
        public void run()
        {
            boardView.surfaceStart();
        }
    };

    // ******************************************************************** //
    // High Scores
    // ******************************************************************** //

    /**
     * Check to see if we need to register a new "high score" (personal best).
     *
     * @param skill   The skill level of the completed puzzle.
     * @param NumberOfTiles  The actual number of tiles in the board. This indicates the
     *                actual difficulty level on the specific device.
     * @param clicks  The user's click count.
     * @param seconds The user's time in SECONDS.
     * @return Message to display to the user. Null if nothing to report.
     */
    private String registerScore(BoardView.Skill skill, int NumberOfTiles, int clicks, int seconds)
    {
        // Get the names of the prefs for the counts for this skill level
        String sizeName = "size" + skill.toString();
        String clickName = "clicks" + skill.toString();
        String timeName = "time" + skill.toString();

        // Get the best to date for this skill level
        SharedPreferences scorePrefs = getSharedPreferences("scores", MODE_PRIVATE);
        int bestClicks = scorePrefs.getInt(clickName, -1);
        int bestTime = scorePrefs.getInt(timeName, -1);

        // See if we have a new best click count or time
        long now = System.currentTimeMillis();
        SharedPreferences.Editor editor = scorePrefs.edit();
        String msg = null;
        if (clicks > 0 && (bestClicks < 0 || clicks < bestClicks))
        {
            editor.putInt(sizeName, NumberOfTiles);
            editor.putInt(clickName, clicks);
            editor.putLong(clickName + "Date", now);
            msg = appResources.getString(R.string.best_clicks_text);
        }
        if (seconds > 0 && (bestTime < 0 || seconds < bestTime))
        {
            editor.putInt(sizeName, NumberOfTiles);
            editor.putInt(timeName, seconds);
            editor.putLong(timeName + "Date", now);
            if (msg == null)
                msg = appResources.getString(R.string.best_time_text);
            else
                msg = appResources.getString(R.string.best_both_text);
        }

        if (msg != null)
            editor.apply();

        return msg;
    }

    // ******************************************************************** //
    // Sound
    // ******************************************************************** //



    /**
     * Post a sound to be played on the main app thread.
     *
     * @param which ID of the sound to play.
     */
    void postSound(final Sound which)
    {
        soundHandler.sendEmptyMessage(which.soundId);
    }

    private Handler soundHandler = new Handler(Looper.getMainLooper())
    {
        @Override
        public void handleMessage(Message m)
        {
            makeSound(m.what);
        }
    };

    /**
     * Make a sound.
     *
     * @param soundId ID of the sound to play.
     */
    void makeSound(int soundId)
    {
        if (soundMode == SoundMode.NONE)
            return;

        float vol = 1.0f;
        if (soundMode == SoundMode.QUIET)
            vol = 0.3f;
        soundPool.play(soundId, vol, vol, 1, 0, 1f);
    }

    // ******************************************************************** //
    // State Save/Restore.
    // ******************************************************************** //

    /**
     * Save game state so that the user does not lose anything if the game
     * process is killed while we are in the background.
     *
     * @param outState A Bundle in which to place any state information we wish to
     *                 save.
     */
    private void saveState(Bundle outState)
    {
        // Save the skill level and game state
        outState.putString("gameSkill", gameSkill.toString());
        outState.putString("gameState", gameState.toString());
        outState.putBoolean("isSolved", isSolved);

        // Save the game state of the board
        boardView.saveState(outState);

        // Restore the game timer and click count
        gameTimer.saveState(outState);
        outState.putInt("clickCount", clickCount);
        outState.putBoolean("solverUsed", solverUsed);
    }

    /**
     * Restore our game state from the given Bundle.
     *
     * @param savedInstanceState A Bundle containing the saved state.
     * @return true if the state was restored OK; false if the saved state was
     * incompatible with the current configuration.
     */
    private boolean restoreState(Bundle savedInstanceState)
    {
        // Get the skill level and game state
        gameSkill = Skill.valueOf(savedInstanceState.getString("gameSkill"));
        gameState = GameState.valueOf(savedInstanceState.getString("gameState"));
        isSolved = savedInstanceState.getBoolean("isSolved");

        // Restore the state of the game board
        boolean restored = boardView.restoreState(savedInstanceState, gameSkill);

        // Restore the game timer and click count
        if (restored)
        {
            restored = gameTimer.restoreState(savedInstanceState, false);
            clickCount = savedInstanceState.getInt("clickCount");
            solverUsed = savedInstanceState.getBoolean("solverUsed");
        }

        return restored;
    }

    // ******************************************************************** //
    // Private Types
    // ******************************************************************** //



    //Create and show an about dialog
    public void showAbout()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.about_text)
                .setTitle(R.string.title)
                .setNegativeButton(R.string.button_close, (dialog, id) -> dialog.cancel());
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    // ******************************************************************** //
    // Class Data
    // ******************************************************************** //

    public static final String TAG = "\t\tScrambleLog"; // Debugging tag
    private Resources appResources;                  // The app's resources
    private BoardView.Skill gameSkill;               // The currently selected skill level
    private GameState gameState;                         // The state of the current game
    private SoundPool soundPool;                     // Sound pool used for sound effects
    private GameTimer gameTimer;                     // Timer used to time the game
    private SoundMode soundMode;                     // Current sound mode
    private int clickCount = 0;                      // Number of times the user has clicked
    private boolean solverUsed = false;              // Has the auto-solver been invoked
    public BoardView boardView = null;               // The game board

    // The status bar, consisting of 3 status fields
    private TextView statusClicks;
    private TextView statusMode;
    private TextView statusTime;

    // Text buffers used to format the click count and time. We allocate these here, so we don't
    // allocate new String objects every time we update the status, which is very often
    private StringBuilder clicksText;
    private StringBuilder timeText;

    // The text widget used to display status messages. When visible, it covers the board
    private TextView splashText = null;

    // View switcher used to switch between the splash text and board view
    private ViewAnimator viewSwitcher = null;

    // The menu used to select the skill level. We keep this so we can set the selected item
    private Menu mainMenu;

    // When gameState == State.RESTORED, this is our restored game state
    private GameState restoredGameState;

    // Flag whether the board has been solved. Once solved, the user can keep playing, but we don't
    // count score any more
    private boolean isSolved;

    // The previous cell that was clicked. Used to detect multiple clicks on the same cell
    private Cell prevClickedCell = null;

    // Current state of the game
    enum GameState
    {
        NEW,
        RESTORED,
        INIT,
        PAUSED,
        RUNNING,
        SOLVED,
        ABORTED;

        static GameState getValue(int ordinal)
        {
            return states[ordinal];
        }

        private static GameState[] states = values();
    }

    // The sounds that we make
    enum Sound
    {
        START(R.raw.start),
        CLICK(R.raw.click),
        TURN(R.raw.turn),
        CONNECT(R.raw.connect),
        POP(R.raw.pop),
        WIN(R.raw.win);

        Sound(int res)
        {
            soundRes = res;
        }

        private final int soundRes; // Resource ID for the sound file
        private int soundId = 0; // Sound ID for playing
    }

    // Sound play mode
    enum SoundMode
    {
        NONE(),
        QUIET(),
        FULL();

        SoundMode()
        {
            // ID of the corresponding menu item
        }

    }


}
