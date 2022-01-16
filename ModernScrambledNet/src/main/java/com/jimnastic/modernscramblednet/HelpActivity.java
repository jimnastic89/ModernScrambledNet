package com.jimnastic.modernscramblednet;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class HelpActivity extends AppCompatActivity
{

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.helpactivity);

        Button help_HowToPlay_Button = findViewById(R.id.help_HowToPlay_Button);
        help_HowToPlay_Button.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Log.i("null","Class of clicked button is: " + v.getId());
                TextView help_HowToPlay_TextView = findViewById(R.id.help_HowToPlay_TextView);
                if (help_HowToPlay_TextView.getVisibility() == View.VISIBLE)
                    help_HowToPlay_TextView.setVisibility(View.GONE);
                else
                    help_HowToPlay_TextView.setVisibility(View.VISIBLE);
            }
        });

        Button help_Difficulties_Button = findViewById(R.id.help_Difficulties_Button);
        help_Difficulties_Button.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                TextView help_Difficulties_TextView = findViewById(R.id.help_Difficulties_TextView);
                if (help_Difficulties_TextView.getVisibility() == View.VISIBLE)
                    help_Difficulties_TextView.setVisibility(View.GONE);
                else
                    help_Difficulties_TextView.setVisibility(View.VISIBLE);
            }
        });

        Button help_Menu_Button = findViewById(R.id.help_Menu_Button);
        help_Menu_Button.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                TextView help_Menu_TextView = findViewById(R.id.help_Menu_TextView);
                if (help_Menu_TextView.getVisibility() == View.VISIBLE)
                    help_Menu_TextView.setVisibility(View.GONE);
                else
                    help_Menu_TextView.setVisibility(View.VISIBLE);
            }
        });

        Button help_Hints_Button = findViewById(R.id.help_Hints_Button);
        help_Hints_Button.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                TextView help_Hints_TextView = findViewById(R.id.help_Hints_TextView);
                if (help_Hints_TextView.getVisibility() == View.VISIBLE)
                    help_Hints_TextView.setVisibility(View.GONE);
                else
                    help_Hints_TextView.setVisibility(View.VISIBLE);
            }
        });
    }


}