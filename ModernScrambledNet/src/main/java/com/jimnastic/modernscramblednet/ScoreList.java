package com.jimnastic.modernscramblednet;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

//An activity which displays the "high score list" (personal bests)
public class ScoreList extends AppCompatActivity
{
    /**
     * Called when the activity is starting. This is where most initialization should go: calling
     * setContentView(int) to inflate the activity's UI, etc
     * <p>
     * You can call finish() from within this function, in which case onDestroy() will be
     * immediately called without any of the rest of the activity lifecycle executing.
     * <p>
     * Derived classes must call through to the super class's implementation of this method. If they
     * do not, an exception will be thrown
     *
     * @param savedInstanceState If the activity is being re-initialized after previously being shut
     *                           down then this Bundle contains the data it most recently supplied
     *                           in onSaveInstanceState(Bundle). Note: Otherwise it is null
     */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.score_layout);
        showScores();
    }

    // ******************************************************************** //
    // Menu Management
    // ******************************************************************** //

    /**
     * Initialize the contents of the game's options menu by adding items to the given menu
     * <p>
     * This is only called once, the first time the options menu is displayed. To update the menu
     * every time it is displayed, see onPrepareOptionsMenu(Menu)
     * <p>
     * When we add items to the menu, we can either supply a Runnable to receive notification of
     * selection, or we can implement the Activity's onOptionsItemSelected(Menu.Item) method to
     * handle them there
     *
     * @param menu The options menu in which we should place our items. We can safely hold on this
     *             (and any items created from it), making modifications to it as desired, until the
     *             next time onCreateOptionsMenu() is called
     * @return true for the menu to be displayed; false to suppress showing it
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // We must call through to the base implementation
        super.onCreateOptionsMenu(menu);

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.scores_menu, menu);

        return true;
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
        if (item.getItemId() == R.id.menu_reset)
            resetScores();
        else
            return super.onOptionsItemSelected(item);

        return true;
    }

    // ******************************************************************** //
    // Scores Display
    // ******************************************************************** //

    private void showScores()
    {
        SharedPreferences scorePrefs = getSharedPreferences("scores", MODE_PRIVATE);
        BoardView.Skill[] skillVals = BoardView.Skill.values();

        // Populate the best clicks table
        TableLayout clicksTable = findViewById(R.id.clicksTable);

        // Remove any existing scores rows, as we may be redisplaying
        int cc = clicksTable.getChildCount();
        if (cc > 2)
            for (int i = cc - 1; i >= 2; --i)
                clicksTable.removeViewAt(i);

        for (BoardView.Skill skill : skillVals)
        {
            String sizeName = "size" + skill.toString();
            String clickName = "clicks" + skill.toString();

            // Get the best to date for this skill level
            int size = scorePrefs.getInt(sizeName, -1);
            int clicks = scorePrefs.getInt(clickName, -1);
            long date = scorePrefs.getLong(clickName + "Date", 0);

            // Create a table row for this column
            TableRow row = new TableRow(this);
            clicksTable.addView(row);

            // Add a label field to display the skill level
            TextView skillLab = new TextView(this);
            skillLab.setTextSize(16);
            //skillLab.setTextColor(0xff000000);
            String stext = getString(skill.label);
            if (size > 0)
                stext += " (" + size + ")";
            skillLab.setText(stext);
            row.addView(skillLab);

            // Add a field to display the clicks count
            TextView clickLab = new TextView(this);
            clickLab.setTextSize(16);
            //clickLab.setTextColor(0xff000000);
            clickLab.setText(clicks < 0 ? "--" : "" + clicks);
            row.addView(clickLab);

            // Add a field to display the date/time of this record
            TextView dateLab = new TextView(this);
            dateLab.setTextSize(16);
            //dateLab.setTextColor(0xff000000);
            dateLab.setText(dateString(date));
            row.addView(dateLab);
        }

        // Populate the best times table
        TableLayout timeTable = findViewById(R.id.timeTable);

        // Remove any existing scores rows, as we may be redisplaying
        int tc = timeTable.getChildCount();
        if (tc > 2)
            for (int i = tc - 1; i >= 2; --i)
                timeTable.removeViewAt(i);

        for (BoardView.Skill skill : skillVals)
        {
            String sizeName = "size" + skill.toString();
            String timeName = "time" + skill.toString();

            // Get the best to date for this skill level
            int size = scorePrefs.getInt(sizeName, -1);
            int time = scorePrefs.getInt(timeName, -1);
            long date = scorePrefs.getLong(timeName + "Date", 0);

            // Create a table row for this column
            TableRow row = new TableRow(this);
            timeTable.addView(row);

            // Add a label field to display the skill level
            TextView skillLab = new TextView(this);
            skillLab.setTextSize(16);
            //skillLab.setTextColor(0xff000000);
            String stext = getString(skill.label);
            if (size > 0)
                stext += " (" + size + ")";
            skillLab.setText(stext);
            row.addView(skillLab);

            // Add a field to display the time taken
            TextView timeLab = new TextView(this);
            timeLab.setTextSize(16);
            //timeLab.setTextColor(0xff000000);
            //timeLab.setText(time < 0 ? "--" : String.format("%2d:%02d", time / 60, time % 60));
            timeLab.setText(time < 0 ? "--" : String.format(Locale.UK, "%2d:%02d", time / 60, time % 60));
            row.addView(timeLab);

            // Add a field to display the date/time of this record
            TextView dateLab = new TextView(this);
            dateLab.setTextSize(16);
            //dateLab.setTextColor(0xff000000);
            dateLab.setText(dateString(date));
            row.addView(dateLab);
        }
    }

    private String dateString(long date)
    {
        if (date == 0)
            return "--";
        int flags = DateUtils.isToday(date) ? DateUtils.FORMAT_SHOW_TIME : DateUtils.FORMAT_SHOW_DATE;
        flags |= DateUtils.FORMAT_ABBREV_ALL;
        return DateUtils.formatDateTime(this, date, flags);
    }

    // ******************************************************************** //
    // Scores Management
    // ******************************************************************** //

    private void resetScores()
    {
        SharedPreferences scorePrefs = getSharedPreferences("scores", MODE_PRIVATE);
        SharedPreferences.Editor editor = scorePrefs.edit();
        editor.clear();
        editor.apply();

        showScores();
    }
}